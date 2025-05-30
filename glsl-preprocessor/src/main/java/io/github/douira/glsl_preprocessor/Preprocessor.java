/*
 * Anarres C Preprocessor
 * Copyright (c) 2007-2015, Shevek
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 *
 * Modified by the contributors of glsl-preprocessor.
 */
package io.github.douira.glsl_preprocessor;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.NonNull;
import io.github.douira.glsl_preprocessor.PreprocessorListener.SourceChangeEvent;
import io.github.douira.glsl_preprocessor.fs.VirtualFile;
import io.github.douira.glsl_preprocessor.fs.VirtualFileSystem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.Reader;
import java.io.StringReader;
import java.util.*;

import static io.github.douira.glsl_preprocessor.PreprocessorCommand.PP_ERROR;
import static io.github.douira.glsl_preprocessor.Token.*;

/**
 * A C Preprocessor.
 * The Preprocessor outputs a token stream which does not need
 * re-lexing for C or C++. Alternatively, the output text may be
 * reconstructed by concatenating the {@link Token#getText() text}
 * values of the returned {@link Token Tokens}.
 */
/*
 * Source file name and line number information is conveyed by lines of the form
 *
 * #line linenum "filename" flags
 *
 * These are called line markers. They are inserted as needed into
 * the output (but never within a string or character constant). They
 * mean that the following line originated in file filename at line
 * linenum. filename will never contain any non-printing characters;
 * they are replaced with octal escape sequences.
 *
 * After the file name comes zero or more flags, which are `1', `2',
 * `3', or `4'. If there are multiple flags, spaces separate them. Here
 * is what the flags mean:
 *
 * `1'
 * This indicates the start of a new file.
 * `2'
 * This indicates returning to a file (after having included another
 * file).
 */
public class Preprocessor implements Closeable {
	public static final int LINE_MARKER_FLAG_NEW_FILE = 1;
	public static final int LINE_MARKER_FLAG_RETURNING = 2;
	
	private static final Logger LOG = LoggerFactory.getLogger(Preprocessor.class);

	private static final Source INTERNAL = new Source() {
		@Override
		public Token token() {
			throw new LexerException("Cannot read from " + getName());
		}

		@Override
		public String getPath() {
			return "<internal-data>";
		}

		@Override
		public String getName() {
			return "internal data";
		}
	};
	private static final Macro __LINE__ = new Macro(INTERNAL, "__LINE__");
	private static final Macro __FILE__ = new Macro(INTERNAL, "__FILE__");
	private static final Macro __COUNTER__ = new Macro(INTERNAL, "__COUNTER__");

	private final List<Source> inputs = new ArrayList<>();

	/* The fundamental engine. */
	private final Map<String, Macro> macros = new HashMap<>();
	private final Stack<State> states = new Stack<>();
	private Source source = null;

	/* Miscellaneous support. */
	private int counter = 0;
	private final Set<String> onceSeenPaths = new HashSet<>();
	private final List<VirtualFile> includes = new ArrayList<>();

	private final Map<String, Integer> sourceNumbers = new HashMap<>();
	private int sourceNumber = 0;

	private final Set<Feature> features = EnumSet.noneOf(Feature.class);
	private final Set<Warning> warnings = EnumSet.noneOf(Warning.class);
	private VirtualFileSystem fileSystem = VirtualFileSystem.EMPTY;
	private PreprocessorListener listener = null;

	{
		macros.put(__LINE__.getName(), __LINE__);
		macros.put(__FILE__.getName(), __FILE__);
		macros.put(__COUNTER__.getName(), __COUNTER__);
		states.push(new State());
	}

	public Preprocessor() {
	}

	public Preprocessor(@NonNull Source initial) {
		addInput(initial);
	}

	public Preprocessor(@NonNull Reader r) {
		this(new LexerSource(r, true));
	}

	public Preprocessor(@NonNull String r) {
		this(new StringReader(r));
	}

	/**
	 * Sets the VirtualFileSystem used by this Preprocessor.
	 */
	public void setFileSystem(@NonNull VirtualFileSystem filesystem) {
		this.fileSystem = filesystem;
	}

	/**
	 * Returns the VirtualFileSystem used by this Preprocessor.
	 */
	@NonNull
	public VirtualFileSystem getFileSystem() {
		return fileSystem;
	}

	/**
	 * Sets the PreprocessorListener which handles events for
	 * this Preprocessor.
	 *
	 * The listener is notified of warnings, errors and source
	 * changes, amongst other things.
	 */
	public void setListener(@NonNull PreprocessorListener listener) {
		this.listener = listener;
		Source s = source;
		while (s != null) {
			// s.setListener(listener);
			s.init(this);
			s = s.getParent();
		}
	}

	/**
	 * Returns the PreprocessorListener which handles events for
	 * this Preprocessor.
	 */
	@NonNull
	public PreprocessorListener getListener() {
		return listener;
	}

	/**
	 * Returns the feature-set for this Preprocessor.
	 *
	 * This set may be freely modified by user code.
	 */
	@NonNull
	public Set<Feature> getFeatures() {
		return features;
	}

	/**
	 * Adds a feature to the feature-set of this Preprocessor.
	 */
	public void addFeature(@NonNull Feature f) {
		features.add(f);
	}

	/**
	 * Adds features to the feature-set of this Preprocessor.
	 */
	public void addFeatures(@NonNull Collection<Feature> f) {
		features.addAll(f);
	}

	/**
	 * Adds features to the feature-set of this Preprocessor.
	 */
	public void addFeatures(Feature... f) {
		addFeatures(Arrays.asList(f));
	}

	/**
	 * Returns true if the given feature is in
	 * the feature-set of this Preprocessor.
	 */
	public boolean getFeature(@NonNull Feature f) {
		return features.contains(f);
	}

	/**
	 * Returns the warning-set for this Preprocessor.
	 *
	 * This set may be freely modified by user code.
	 */
	@NonNull
	public Set<Warning> getWarnings() {
		return warnings;
	}

	/**
	 * Adds a warning to the warning-set of this Preprocessor.
	 */
	public void addWarning(@NonNull Warning w) {
		warnings.add(w);
	}

	/**
	 * Adds warnings to the warning-set of this Preprocessor.
	 */
	public void addWarnings(@NonNull Collection<Warning> w) {
		warnings.addAll(w);
	}

	/**
	 * Returns true if the given warning is in
	 * the warning-set of this Preprocessor.
	 */
	public boolean getWarning(@NonNull Warning w) {
		return warnings.contains(w);
	}

	/**
	 * Adds input for the Preprocessor.
	 *
	 * Inputs are processed in the order in which they are added.
	 */
	public void addInput(@NonNull Source source) {
		source.init(this);
		inputs.add(source);
	}
	
	public void addInput(@NonNull String input) {
		this.addInput(new LexerSource(new StringReader(input), true));
	}

	/**
	 * Handles an error.
	 *
	 * If a PreprocessorListener is installed, it receives the
	 * error. Otherwise, an exception is thrown.
	 */
	protected void error(int line, int column, @NonNull String msg) {
		if (listener != null)
			listener.handleError(source, line, column, msg);
		else
			throw new LexerException("Error at " + line + ":" + column + ": " + msg);
	}

	/**
	 * Handles an error.
	 *
	 * If a PreprocessorListener is installed, it receives the
	 * error. Otherwise, an exception is thrown.
	 *
	 * @see #error(int, int, String)
	 */
	protected void error(@NonNull Token tok, @NonNull String msg) {
		error(tok.getLine(), tok.getColumn(), msg);
	}

	/**
	 * Handles a warning.
	 *
	 * If a PreprocessorListener is installed, it receives the
	 * warning. Otherwise, an exception is thrown.
	 */
	protected void warning(int line, int column, @NonNull String msg) {
		if (warnings.contains(Warning.ERROR))
			error(line, column, msg);
		else if (listener != null)
			listener.handleWarning(source, line, column, msg);
		else
			throw new LexerException("Warning at " + line + ":" + column + ": " + msg);
	}

	/**
	 * Handles a warning.
	 *
	 * If a PreprocessorListener is installed, it receives the
	 * warning. Otherwise, an exception is thrown.
	 *
	 * @see #warning(int, int, String)
	 */
	protected void warning(@NonNull Token tok, @NonNull String msg) {
		warning(tok.getLine(), tok.getColumn(), msg);
	}

	/**
	 * Adds a Macro to this Preprocessor.
	 *
	 * The given {@link Macro} object encapsulates both the name
	 * and the expansion.
	 *
	 * @throws LexerException if the definition fails or is otherwise illegal.
	 */
	public void addMacro(@NonNull Macro m) {
		// System.out.println("Macro " + m);
		String name = m.getName();
		/* Already handled as a source error in macro(). */
		if ("defined".equals(name))
			throw new LexerException("Cannot redefine name 'defined'");
		macros.put(m.getName(), m);
	}

	/**
	 * Defines the given name as a macro.
	 *
	 * The String value is lexed into a token stream, which is
	 * used as the macro expansion.
	 *
	 * @throws LexerException if the definition fails or is otherwise illegal.
	 */
	public void addMacro(@NonNull String name, @NonNull String value) {
		Macro m = new Macro(name);
		try (StringLexerSource s = new StringLexerSource(value)) {
			while (true) {
				Token tok = s.token();
				if (tok.getType() == EOF)
					break;
				m.addToken(tok);
			}
		}
		addMacro(m);
	}

	/**
	 * Defines the given name as a macro, with the value <code>1</code>.
	 *
	 * This is a convenience method, and is equivalent to
	 * <code>addMacro(name, "1")</code>.
	 *
	 * @throws LexerException if the definition fails or is otherwise illegal.
	 */
	public void addMacro(@NonNull String name) {
		addMacro(name, "1");
	}

	/**
	 * Returns the Map of Macros parsed during the run of this
	 * Preprocessor.
	 *
	 * @return The {@link Map} of macros currently defined.
	 */
	@NonNull
	public Map<String, Macro> getMacros() {
		return macros;
	}

	/**
	 * Returns the named macro.
	 *
	 * While you can modify the returned object, unexpected things
	 * might happen if you do.
	 *
	 * @return the Macro object, or null if not found.
	 */
	@CheckForNull
	public Macro getMacro(@NonNull String name) {
		return macros.get(name);
	}

	/**
	 * Returns the list of {@link VirtualFile VirtualFiles} which have been
	 * included by this Preprocessor.
	 *
	 * This does not include any {@link Source} provided to the constructor
	 * or {@link #addInput(Source)}.
	 */
	@NonNull
	public List<? extends VirtualFile> getIncludes() {
		return includes;
	}

	/* States */
	private void push_state() {
		State top = states.peek();
		states.push(new State(top));
	}

	private void pop_state() {
		State s = states.pop();
		if (states.isEmpty()) {
			error(0, 0, "#" + "endif without #" + "if");
			states.push(s);
		}
	}

	private boolean isActive() {
		State state = states.peek();
		return state.isParentActive() && state.isActive();
	}

	/* Sources */
	/**
	 * Returns the top Source on the input stack.
	 *
	 * @see Source
	 * @see #push_source(Source,boolean)
	 * @see #pop_source()
	 *
	 * @return the top Source on the input stack.
	 */
	// @CheckForNull
	protected Source getSource() {
		return source;
	}

	/**
	 * Pushes a Source onto the input stack.
	 *
	 * @param source  the new Source to push onto the top of the input stack.
	 * @param autopop if true, the Source is automatically removed from the input
	 *                stack at EOF.
	 * @see #getSource()
	 * @see #pop_source()
	 */
	protected void push_source(@NonNull Source source, boolean autopop) {
		source.init(this);
		source.setParent(this.source, autopop);
		// source.setListener(listener);
		if (listener != null)
			listener.handleSourceChange(this.source, SourceChangeEvent.SUSPEND);
		this.source = source;
		if (listener != null)
			listener.handleSourceChange(this.source, SourceChangeEvent.PUSH);
	}

	/**
	 * Pops a Source from the input stack.
	 *
	 * @see #getSource()
	 * @see #push_source(Source,boolean)
	 *
	 * @param createLineMarker whether a line marker token should be created
	 */
	@CheckForNull
	protected Token pop_source(boolean createLineMarker) {
		if (listener != null)
			listener.handleSourceChange(this.source, SourceChangeEvent.POP);
		Source s = this.source;
		this.source = s.getParent();
		/* Always a noop unless called externally. */
		s.close();
		if (listener != null && this.source != null)
			listener.handleSourceChange(this.source, SourceChangeEvent.RESUME);

		Source t = getSource();
		if (createLineMarker && getFeature(Feature.LINE_MARKERS) && s.isNumbered() && t != null) {
			/*
			 * We actually want 'did the nested source
			 * contain a newline token', which isNumbered()
			 * approximates. This is not perfect, but works.
			 */
			return line_token(t.getLine(), t.getName(), LINE_MARKER_FLAG_RETURNING);
		}

		return null;
	}

	protected void pop_source() {
		pop_source(false);
	}

	@NonNull
	private Token next_source() {
		if (inputs.isEmpty())
			return new Token(EOF);
		Source s = inputs.remove(0);
		push_source(s, true);
		return line_token(s.getLine(), s.getName(), LINE_MARKER_FLAG_NEW_FILE);
	}

	/* Source tokens */
	private Token source_token;
	
	/*
	 * XXX Make this include the NL, and make all cpp directives eat
	 * their own NL.
	 */
	@NonNull
	private Token line_token(int line, @CheckForNull String sourceName, int flags) {
		StringBuilder buf = new StringBuilder();
		buf.append("#line ").append(line);

		if (getFeature(Feature.NAMED_LINE_MARKERS)) {
			buf.append(" \"");
			/* XXX This call to escape(sourceName) is correct but ugly. */
			if (sourceName == null) {
				buf.append("<no file>");
			} else {
				MacroTokenSource.escape(buf, sourceName);
			}

			buf.append("\"");
		} else {
			buf.append(" ").append(sourceNumbers.computeIfAbsent(sourceName, n -> sourceNumber++));
		}
		if (getFeature(Feature.LINE_MARKER_FLAGS) && flags != 0) {
			for (int i = 0; i < 4; i++) {
				if ((flags & (1 << i)) != 0) {
					buf.append(" ").append(i + 1);
				}
			}
		}

		buf.append("\n");
		return new Token(P_LINE, line, 0, buf.toString(), null);
	}

	public Map<String, Integer> getSourceNumbers() {
		return sourceNumbers;
	}

	@NonNull
	private Token source_token() {
		if (source_token != null) {
			Token tok = source_token;
			source_token = null;
			if (getFeature(Feature.DEBUG)) {
				LOG.debug("Returning unget token {}", tok);
			}
			return tok;
		}

		while (true) {
			Source s = getSource();
			if (s == null) {
				Token t = next_source();
				if (t.getType() == P_LINE && !getFeature(Feature.LINE_MARKERS)) {
					continue;
				}
				return t;
			}
			Token tok = s.token();
			/* XXX Refactor with skipline() */
			if (tok.getType() == EOF && s.isAutopop()) {
				// System.out.println("Autopop " + s);
				Token mark = pop_source(true);
				if (mark != null) {
					return mark;
				}
				continue;
			}
			if (getFeature(Feature.DEBUG)) {
				LOG.debug("Returning fresh token {}", tok);
			}
			return tok;
		}
	}

	/**
	 * Function that quques a token to be returned by the next call to
	 * {@link #source_token()} which effectively returns it the next time
	 * {@link #token()} is called.
	 * 
	 * @param tok The token to store for returning next
	 */
	private void source_untoken(Token tok) {
		if (this.source_token != null)
			throw new IllegalStateException("Cannot return two tokens");
		this.source_token = tok;
	}

	private boolean isWhite(Token tok) {
		int type = tok.getType();
		return (type == WHITESPACE)
				|| (type == CCOMMENT)
				|| (type == CPPCOMMENT);
	}

	private Token source_token_nonwhite() {
		Token tok;
		do {
			tok = source_token();
		} while (isWhite(tok));
		return tok;
	}

	/**
	 * Returns an NL or an EOF token.
	 *
	 * The metadata on the token will be correct, which is better
	 * than generating a new one.
	 *
	 * This method can, as of recent patches, return a P_LINE token.
	 */
	private Token source_skipline(boolean white) {
		// (new Exception("skipping line")).printStackTrace(System.out);
		Source s = getSource();
		Token tok = s.skipline(white);
		/* XXX Refactor with source_token() */
		if (tok.getType() == EOF && s.isAutopop()) {
			// System.out.println("Autopop " + s);
			Token mark = pop_source(true);
			if (mark != null)
				return mark;
		}
		return tok;
	}

	/* processes and expands a macro. */
	private boolean macro(Macro m, Token orig) {
		Token tok;
		List<Argument> args;

		// System.out.println("pp: expanding " + m);
		if (m.isFunctionLike()) {
			OPEN: while (true) {
				tok = source_token();
				// System.out.println("pp: open: token is " + tok);
				switch (tok.getType()) {
					case WHITESPACE: /* XXX Really? */

					case CCOMMENT:
					case CPPCOMMENT:
					case NL:
						break; /* continue */

					case '(':
						break OPEN;
					default:
						source_untoken(tok);
						return false;
				}
			}

			// tok = expanded_token_nonwhite();
			tok = source_token_nonwhite();

			/*
			 * We either have, or we should have args.
			 * This deals elegantly with the case that we have
			 * one empty arg.
			 */
			if (tok.getType() != ')' || m.getArgs() > 0) {
				args = new ArrayList<>();

				Argument arg = new Argument();
				int depth = 0;
				boolean space = false;

				ARGS: while (true) {
					// System.out.println("pp: arg: token is " + tok);
					switch (tok.getType()) {
						case EOF:
							error(tok, "EOF in macro args");
							return false;

						case ',':
							if (depth == 0) {
								if (m.isVariadic()
										&& /* We are building the last arg. */ args.size() == m.getArgs() - 1) {
									/* Just add the comma. */
									arg.addToken(tok);
								} else {
									args.add(arg);
									arg = new Argument();
								}
							} else {
								arg.addToken(tok);
							}
							space = false;
							break;
						case ')':
							if (depth == 0) {
								args.add(arg);
								break ARGS;
							} else {
								depth--;
								arg.addToken(tok);
							}
							space = false;
							break;
						case '(':
							depth++;
							arg.addToken(tok);
							space = false;
							break;

						case WHITESPACE:
						case CCOMMENT:
						case CPPCOMMENT:
						case NL:
							/* Avoid duplicating spaces. */
							space = true;
							break;

						default:
							/*
							 * Do not put space on the beginning of
							 * an argument token.
							 */
							if (space && !arg.isEmpty())
								arg.addToken(Token.space);
							arg.addToken(tok);
							space = false;
							break;

					}
					// tok = expanded_token();
					tok = source_token();
				}
				/*
				 * space may still be true here, thus trailing space
				 * is stripped from arguments.
				 */

				if (args.size() != m.getArgs()) {
					if (m.isVariadic()) {
						if (args.size() == m.getArgs() - 1) {
							args.add(new Argument());
						} else {
							error(tok,
									"variadic macro " + m.getName()
											+ " has at least " + (m.getArgs() - 1) + " parameters "
											+ "but given " + args.size() + " args");
							return false;
						}
					} else {
						error(tok,
								"macro " + m.getName()
										+ " has " + m.getArgs() + " parameters "
										+ "but given " + args.size() + " args");
						/*
						 * We could replay the arg tokens, but I
						 * note that GNU cpp does exactly what we do,
						 * i.e. output the macro name and chew the args.
						 */
						return false;
					}
				}

				for (Argument a : args) {
					a.expand(this);
				}

				// System.out.println("Macro " + m + " args " + args);
			} else {
				/* nargs == 0 and we (correctly) got () */
				args = null;
			}

		} else {
			/* Macro without args. */
			args = null;
		}

		if (m == __LINE__) {
			push_source(new FixedTokenSource(
							new Token(NUMBER,
									orig.getLine(), orig.getColumn(),
									Integer.toString(orig.getLine()),
									new NumericValue(10, Integer.toString(orig.getLine())))),
					true);
		} else if (m == __FILE__) {
			StringBuilder buf = new StringBuilder("\"");
			String name = getSource().getName();
			if (name == null)
				name = "<no file>";
			for (int i = 0; i < name.length(); i++) {
				char c = name.charAt(i);
				switch (c) {
					case '\\':
						buf.append("\\\\");
						break;
					case '"':
						buf.append("\\\"");
						break;
					default:
						buf.append(c);
						break;
				}
			}
			buf.append("\"");
			String text = buf.toString();
			push_source(new FixedTokenSource(
							new Token(STRING,
									orig.getLine(), orig.getColumn(),
									text, text)),
					true);
		} else if (m == __COUNTER__) {
			/*
			 * This could equivalently have been done by adding
			 * a special Macro subclass which overrides getTokens().
			 */
			int value = this.counter++;
			push_source(new FixedTokenSource(
							new Token(NUMBER,
									orig.getLine(), orig.getColumn(),
									Integer.toString(value),
									new NumericValue(10, Integer.toString(value)))),
					true);
		} else {
			push_source(new MacroTokenSource(m, args), true);
		}

		return true;
	}

	/**
	 * Expands an argument.
	 */
	/* I'd rather this was done lazily, but doing so breaks spec. */
	@NonNull
	List<Token> expand(@NonNull List<Token> arg) {
		List<Token> expansion = new ArrayList<>();
		boolean space = false;

		push_source(new FixedTokenSource(arg), false);

		EXPANSION: while (true) {
			Token tok = expanded_token();
			switch (tok.getType()) {
				case EOF:
					break EXPANSION;

				case WHITESPACE:
				case CCOMMENT:
				case CPPCOMMENT:
					space = true;
					break;

				default:
					if (space && !expansion.isEmpty())
						expansion.add(Token.space);
					expansion.add(tok);
					space = false;
					break;
			}
		}

		// Always returns null.
		pop_source(false);

		return expansion;
	}

	/* processes a #define directive */
	private Token define() {
		Token tok = source_token_nonwhite();
		if (tok.getType() != IDENTIFIER) {
			error(tok, "Expected identifier");
			return source_skipline(false);
		}
		/* if predefined */

		String name = tok.getText();
		if ("defined".equals(name)) {
			error(tok, "Cannot redefine name 'defined'");
			return source_skipline(false);
		}

		Macro m = new Macro(getSource(), name);
		List<String> args;

		tok = source_token();
		if (tok.getType() == '(') {
			tok = source_token_nonwhite();
			if (tok.getType() != ')') {
				args = new ArrayList<>();
				ARGS: while (true) {
					switch (tok.getType()) {
						case IDENTIFIER:
							args.add(tok.getText());
							break;
						case ELLIPSIS:
							// Unnamed Variadic macro
							args.add("__VA_ARGS__");
							// We just named the ellipsis, but we unget the token
							// to allow the ELLIPSIS handling below to process it.
							source_untoken(tok);
							break;
						case NL:
						case EOF:
							error(tok,
									"Unterminated macro parameter list");
							return tok;
						default:
							error(tok,
									"error in macro parameters: "
											+ tok.getText());
							return source_skipline(false);
					}
					tok = source_token_nonwhite();
					switch (tok.getType()) {
						case ',':
							break;
						case ELLIPSIS:
							tok = source_token_nonwhite();
							if (tok.getType() != ')')
								error(tok,
										"ellipsis must be on last argument");
							m.setVariadic(true);
							break ARGS;
						case ')':
							break ARGS;

						case NL:
						case EOF:
							/* Do not skip line. */
							error(tok,
									"Unterminated macro parameters");
							return tok;
						default:
							error(tok,
									"Bad token in macro parameters: "
											+ tok.getText());
							return source_skipline(false);
					}
					tok = source_token_nonwhite();
				}
			} else {
				assert tok.getType() == ')' : "Expected ')'";
				args = Collections.emptyList();
			}

			m.setArgs(args);
		} else {
			/* For searching. */
			args = Collections.emptyList();
			source_untoken(tok);
		}

		/* Get an expansion for the macro, using indexOf. */
		boolean space = false;
		boolean paste = false;
		int idx;

		/* Ensure no space at start. */
		tok = source_token_nonwhite();
		EXPANSION: while (true) {
			switch (tok.getType()) {
				case EOF, NL:
					break EXPANSION;

				case CCOMMENT:
				case CPPCOMMENT:
					/* XXX This is where we implement GNU's cpp -CC. */
					// break;
				case WHITESPACE:
					if (!paste)
						space = true;
					break;

				/* Paste. */
				case PASTE:
					space = false;
					paste = true;
					m.addPaste(new Token(M_PASTE,
							tok.getLine(), tok.getColumn(),
							"#" + "#", null));
					break;

				/* Stringify. */
				case '#':
					if (space)
						m.addToken(Token.space);
					space = false;
					Token la = source_token_nonwhite();
					if (la.getType() == IDENTIFIER
							&& ((idx = args.indexOf(la.getText())) != -1)) {
						m.addToken(new Token(M_STRING,
								la.getLine(), la.getColumn(),
								"#" + la.getText(),
								idx));
					} else {
						m.addToken(tok);
						/* Allow for special processing. */
						source_untoken(la);
					}
					break;

				case IDENTIFIER:
					if (space)
						m.addToken(Token.space);
					space = false;
					paste = false;
					idx = args.indexOf(tok.getText());
					if (idx == -1)
						m.addToken(tok);
					else
						m.addToken(new Token(M_ARG,
								tok.getLine(), tok.getColumn(),
								tok.getText(),
								idx));
					break;

				default:
					if (space)
						m.addToken(Token.space);
					space = false;
					paste = false;
					m.addToken(tok);
					break;
			}
			tok = source_token();
		}

		if (getFeature(Feature.DEBUG))
			LOG.debug("Defined macro {}", m);
		addMacro(m);

		return tok; /* NL or EOF. */

	}

	@NonNull
	private Token undef() {
		Token tok = source_token_nonwhite();
		if (tok.getType() != IDENTIFIER) {
			error(tok,
					"Expected identifier, not " + tok.getText());
			if (tok.getType() == NL || tok.getType() == EOF)
				return tok;
		} else {
			Macro m = getMacro(tok.getText());
			if (m != null) {
				/* XXX error if predefined */
				macros.remove(m.getName());
			}
		}
		return source_skipline(true);
	}

	/**
	 * Attempts to include the given file.
	 *
	 * User code may override this method to implement a virtual
	 * file system.
	 *
	 * @param file The VirtualFile to attempt to include.
	 * @return true if the file was successfully included, false otherwise.
	 */
	protected boolean include(@NonNull VirtualFile file) {
		if (!file.isFile())
			return false;
		if (getFeature(Feature.DEBUG))
			LOG.debug("pp: including {}", file);
		includes.add(file);
		push_source(file.getSource(), true);
		return true;
	}

	/**
	 * Handles an include directive.
	 */
	private void include(@CheckForNull Source parent, int line, @NonNull String name, boolean quoted, boolean next) {
		VirtualFile file = fileSystem.getFile(parent, name, quoted, next);
		if (include(file)) {
			return;
		}
		error(line, 0, "File not found: " + name);
	}

	@NonNull
	private Token include(boolean next) {
		LexerSource lexer = (LexerSource) source;
		try {
			lexer.setInclude(true);
			Token tok = token_nonwhite();

			String name;
			boolean quoted;

			if (tok.getType() == STRING) {
				/*
				 * XXX Use the original text, not the value.
				 * Backslashes must not be treated as escapes here.
				 */
				StringBuilder buf = new StringBuilder((String) tok.getValue());
				HEADER: while (true) {
					tok = token_nonwhite();
					switch (tok.getType()) {
						case STRING:
							buf.append((String) tok.getValue());
							break;
						case NL:
						case EOF:
							break HEADER;
						default:
							warning(tok,
									"Unexpected token on #" + "include line");
							return source_skipline(false);
					}
				}
				name = buf.toString();
				quoted = true;
			} else if (tok.getType() == HEADER) {
				name = (String) tok.getValue();
				quoted = false;
				tok = source_skipline(true);
			} else {
				error(tok,
						"Expected string or header, not " + tok.getText());
				return switch (tok.getType()) {
					case NL, EOF -> tok;
					default ->
						/* Only if not a NL or EOF already. */
							source_skipline(false);
				};
			}

			/* Do the inclusion. */
			include(source, tok.getLine(), name, quoted, next);

			/*
			 * 'tok' is the 'nl' after the include. We use it after the
			 * #line directive.
			 */
			if (getFeature(Feature.LINE_MARKERS)) {
				return line_token(1, source.getName(), LINE_MARKER_FLAG_NEW_FILE);
			}
			return tok;
		} finally {
			lexer.setInclude(false);
		}
	}

	protected void pragma_once(@NonNull Token name) {
		Source s = this.source;
		if (!onceSeenPaths.add(s.getPath())) {
			Token mark = pop_source(true);
			// FixedTokenSource should never generate a line marker on exit.
			if (mark != null) {
				push_source(new FixedTokenSource(List.of(mark)), true);
			}
		}
	}

	protected void pragma(@NonNull Token name, @NonNull List<Token> value) {
		if (getFeature(Feature.PRAGMA_ONCE)) {
			if ("once".equals(name.getText())) {
				pragma_once(name);
				return;
			}
		}
		if (!getFeature(Feature.ARBITRARY_PRAGMAS)) {
			warning(name, "Unknown #" + "pragma: " + name.getText());
		}
	}

	@NonNull
	private Token pragma() {
		Token name;

		NAME: while (true) {
			Token tok = source_token();
			switch (tok.getType()) {
				case EOF:
					/*
					 * There ought to be a newline before EOF.
					 * At least, in any skipline context.
					 */
					/* XXX Are we sure about this? */
					warning(tok,
							"End of file in #" + "pragma");
					return tok;
				case NL:
					/* This may contain one or more newlines. */
					warning(tok,
							"Empty #" + "pragma");
					return tok;
				case CCOMMENT:
				case CPPCOMMENT:
				case WHITESPACE:
					continue;
				case IDENTIFIER:
					name = tok;
					break NAME;
				default:
					warning(tok,
							"Illegal #" + "pragma " + tok.getText());
					return source_skipline(false);
			}
		}

		Token tok;
		List<Token> value = new ArrayList<>();
		VALUE: while (true) {
			tok = source_token();
			switch (tok.getType()) {
				case EOF:
					/*
					 * There ought to be a newline before EOF.
					 * At least, in any skipline context.
					 */
					/* XXX Are we sure about this? */
					warning(tok,
							"End of file in #" + "pragma");
					break VALUE;
				case NL:
					/* This may contain one or more newlines. */
					break VALUE;
				case CCOMMENT:
				case CPPCOMMENT:
					break;
				default: // includes WHITESPACE
					value.add(tok);
					break;
			}
		}

		pragma(name, value);

		return tok; /* The NL. */

	}

	/* For #error and #warning. */
	private void error(@NonNull Token pptok, boolean is_error) {
		StringBuilder buf = new StringBuilder();
		buf.append('#').append(pptok.getText()).append(' ');
		/* Peculiar construction to ditch first whitespace. */
		Token tok = source_token_nonwhite();
		ERROR: while (true) {
			switch (tok.getType()) {
				case NL:
				case EOF:
					break ERROR;
				default:
					buf.append(tok.getText());
					break;
			}
			tok = source_token();
		}
		if (is_error)
			error(pptok, buf.toString());
		else
			warning(pptok, buf.toString());
	}

	/*
	 * This bypasses token() for #elif expressions.
	 * If we don't do this, then isActive() == false
	 * causes token() to simply chew the entire input line.
	 */
	@NonNull
	private Token expanded_token() {
		while (true) {
			Token tok = source_token();
			// System.out.println("Source token is " + tok);
			if (tok.getType() == IDENTIFIER) {
				Macro m = getMacro(tok.getText());
				if (m == null)
					return tok;
				if (source.isExpanding(m))
					return tok;
				if (macro(m, tok))
					continue;
			}
			return tok;
		}
	}

	@NonNull
	private Token expanded_token_nonwhite() {
		Token tok;
		do {
			tok = expanded_token();
			// System.out.println("expanded token is " + tok);
		} while (isWhite(tok));
		return tok;
	}

	@CheckForNull
	private Token expr_token = null;

	@NonNull
	private Token expr_token() {
		Token tok = expr_token;

		if (tok != null) {
			// System.out.println("ungetting");
			expr_token = null;
		} else {
			tok = expanded_token_nonwhite();
			// System.out.println("expt is " + tok);

			if (tok.getType() == IDENTIFIER
					&& tok.getText().equals("defined")) {
				Token la = source_token_nonwhite();
				boolean paren = false;
				if (la.getType() == '(') {
					paren = true;
					la = source_token_nonwhite();
				}

				// System.out.println("Core token is " + la);
				if (la.getType() != IDENTIFIER) {
					error(la,
							"defined() needs identifier, not "
									+ la.getText());
					tok = new Token(NUMBER,
							la.getLine(), la.getColumn(),
							"0", new NumericValue(10, "0"));
				} else if (macros.containsKey(la.getText())) {
					// System.out.println("Found macro");
					tok = new Token(NUMBER,
							la.getLine(), la.getColumn(),
							"1", new NumericValue(10, "1"));
				} else {
					// System.out.println("Not found macro");
					tok = new Token(NUMBER,
							la.getLine(), la.getColumn(),
							"0", new NumericValue(10, "0"));
				}

				if (paren) {
					la = source_token_nonwhite();
					if (la.getType() != ')') {
						expr_untoken(la);
						error(la, "Missing ) in defined(). Got " + la.getText());
					}
				}
			}
		}

		// System.out.println("expr_token returns " + tok);
		return tok;
	}

	private void expr_untoken(@NonNull Token tok) {
		if (expr_token != null)
			throw new InternalException(
					"Cannot unget two expression tokens.");
		expr_token = tok;
	}

	private int expr_priority(@NonNull Token op) {
		return switch (op.getType()) {
			case '/' -> 11;
			case '%' -> 11;
			case '*' -> 11;
			case '+' -> 10;
			case '-' -> 10;
			case LSH -> 9;
			case RSH -> 9;
			case '<' -> 8;
			case '>' -> 8;
			case LE -> 8;
			case GE -> 8;
			case EQ -> 7;
			case NE -> 7;
			case '&' -> 6;
			case '^' -> 5;
			case '|' -> 4;
			case LAND -> 3;
			case LOR -> 2;
			case '?' -> 1;
			default ->
				// System.out.println("Unrecognised operator " + op);
					0;
		};
	}

	private int expr_char(Token token) {
		Object value = token.getValue();
		if (value instanceof Character)
			return (Character) value;
		String text = String.valueOf(value);
		if (text.isEmpty())
			return 0;
		return text.charAt(0);
	}

	private long expr(int priority) {
		/*
		 * (new Exception("expr(" + priority + ") called")).printStackTrace();
		 */

		Token tok = expr_token();
		long lhs, rhs;

		// System.out.println("Expr lhs token is " + tok);
		switch (tok.getType()) {
			case '(':
				lhs = expr(0);
				tok = expr_token();
				if (tok.getType() != ')') {
					expr_untoken(tok);
					error(tok, "Missing ) in expression. Got " + tok.getText());
					return 0;
				}
				break;

			case '~':
				lhs = ~expr(11);
				break;
			case '!':
				lhs = expr(11) == 0 ? 1 : 0;
				break;
			case '-':
				lhs = -expr(11);
				break;
			case NUMBER:
				NumericValue value = (NumericValue) tok.getValue();
				lhs = value.longValue();
				break;
			case CHARACTER:
				lhs = expr_char(tok);
				break;
			case IDENTIFIER:
				if (warnings.contains(Warning.UNDEF))
					warning(tok, "Undefined token '" + tok.getText()
							+ "' encountered in conditional.");
				lhs = 0;
				break;

			default:
				expr_untoken(tok);
				error(tok,
						"Bad token in expression: " + tok.getText());
				return 0;
		}

		while (true) {
			// System.out.println("expr: lhs is " + lhs + ", pri = " + priority);
			Token op = expr_token();
			int pri = expr_priority(op); /* 0 if not a binop. */

			if (pri == 0 || priority >= pri) {
				expr_untoken(op);
				break;
			}
			rhs = expr(pri);
			// System.out.println("rhs token is " + rhs);
			switch (op.getType()) {
				case '/':
					if (rhs == 0) {
						error(op, "Division by zero");
						lhs = 0;
					} else {
						lhs = lhs / rhs;
					}
					break;
				case '%':
					if (rhs == 0) {
						error(op, "Modulus by zero");
						lhs = 0;
					} else {
						lhs = lhs % rhs;
					}
					break;
				case '*':
					lhs = lhs * rhs;
					break;
				case '+':
					lhs = lhs + rhs;
					break;
				case '-':
					lhs = lhs - rhs;
					break;
				case '<':
					lhs = lhs < rhs ? 1 : 0;
					break;
				case '>':
					lhs = lhs > rhs ? 1 : 0;
					break;
				case '&':
					lhs = lhs & rhs;
					break;
				case '^':
					lhs = lhs ^ rhs;
					break;
				case '|':
					lhs = lhs | rhs;
					break;

				case LSH:
					lhs = lhs << rhs;
					break;
				case RSH:
					lhs = lhs >> rhs;
					break;
				case LE:
					lhs = lhs <= rhs ? 1 : 0;
					break;
				case GE:
					lhs = lhs >= rhs ? 1 : 0;
					break;
				case EQ:
					lhs = lhs == rhs ? 1 : 0;
					break;
				case NE:
					lhs = lhs != rhs ? 1 : 0;
					break;
				case LAND:
					lhs = (lhs != 0) && (rhs != 0) ? 1 : 0;
					break;
				case LOR:
					lhs = (lhs != 0) || (rhs != 0) ? 1 : 0;
					break;

				case '?': {
					tok = expr_token();
					if (tok.getType() != ':') {
						expr_untoken(tok);
						error(tok, "Missing : in conditional expression. Got " + tok.getText());
						return 0;
					}
					long falseResult = expr(0);
					lhs = (lhs != 0) ? rhs : falseResult;
				}
				break;

				default:
					error(op,
							"Unexpected operator " + op.getText());
					return 0;

			}
		}

		/*
		 * (new Exception("expr returning " + lhs)).printStackTrace();
		 */
		// System.out.println("expr returning " + lhs);
		return lhs;
	}

	@NonNull
	private Token toWhitespace(@NonNull Token tok) {
		String text = tok.getText();
		int len = text.length();
		boolean cr = false;
		int nls = 0;

		for (int i = 0; i < len; i++) {
			char c = text.charAt(i);

			switch (c) {
				case '\r':
					cr = true;
					nls++;
					break;
				case '\n':
					if (cr) {
						cr = false;
						break;
					}
					/* fallthrough */
				case '\u2028':
				case '\u2029':
				case '\u000B':
				case '\u000C':
				case '\u0085':
					cr = false;
					nls++;
					break;
			}
		}

		char[] cbuf = new char[nls];
		Arrays.fill(cbuf, '\n');
		return new Token(WHITESPACE,
				tok.getLine(), tok.getColumn(),
				new String(cbuf));
	}

	@NonNull
	private Token _token() {
		while (true) {
			Token tok;
			if (!isActive()) {
				Source s = getSource();
				if (s == null) {
					Token t = next_source();
					if (t.getType() == P_LINE && !getFeature(Feature.LINE_MARKERS))
						continue;
					return t;
				}

				try {
					/* XXX Tell lexer to ignore warnings. */
					s.setActive(false);
					tok = source_token();
				} finally {
					/* XXX Tell lexer to stop ignoring warnings. */
					s.setActive(true);
				}
				switch (tok.getType()) {
					case HASH:
					case NL:
					case EOF:
						/* The preprocessor has to take action here. */
						break;
					case WHITESPACE:
						return tok;
					case CCOMMENT:
					case CPPCOMMENT:
						// Patch up to preserve whitespace.
						if (getFeature(Feature.KEEP_ALL_COMMENTS))
							return tok;
						if (!isActive())
							return toWhitespace(tok);
						if (getFeature(Feature.KEEP_COMMENTS))
							return tok;
						return toWhitespace(tok);
					default:
						// Return NL to preserve whitespace.
						/* XXX This might lose a comment. */
						return source_skipline(false);
				}
			} else {
				tok = source_token();
			}

			LEX: switch (tok.getType()) {
				case EOF:
					/* Pop the stacks. */
					return tok;

				case WHITESPACE:
				case NL:
					return tok;

				case CCOMMENT:
				case CPPCOMMENT:
					return tok;

				case '!':
				case '%':
				case '&':
				case '(':
				case ')':
				case '*':
				case '+':
				case ',':
				case '-':
				case '/':
				case ':':
				case ';':
				case '<':
				case '=':
				case '>':
				case '?':
				case '[':
				case ']':
				case '^':
				case '{':
				case '|':
				case '}':
				case '~':
				case '.':

					/* From Olivier Chafik for Objective C? */
				case '@':
					/* The one remaining ASCII, might as well. */
				case '`':

					// case '#':
				case AND_EQ:
				case ARROW:
				case CHARACTER:
				case DEC:
				case DIV_EQ:
				case ELLIPSIS:
				case EQ:
				case GE:
				case HEADER: /* Should only arise from include() */

				case INC:
				case LAND:
				case LE:
				case LOR:
				case LSH:
				case LSH_EQ:
				case SUB_EQ:
				case MOD_EQ:
				case MULT_EQ:
				case NE:
				case OR_EQ:
				case PLUS_EQ:
				case RANGE:
				case RSH:
				case RSH_EQ:
				case STRING:
				case SQSTRING:
				case XOR_EQ:
					return tok;

				case NUMBER:
					return tok;

				case IDENTIFIER:
					Macro m = getMacro(tok.getText());
					if (m == null)
						return tok;
					if (source.isExpanding(m))
						return tok;
					if (macro(m, tok))
						break;
					return tok;

				case P_LINE:
					if (getFeature(Feature.LINE_MARKERS))
						return tok;
					break;

				case INVALID:
					if (getFeature(Feature.C_SYNTAX))
						error(tok, String.valueOf(tok.getValue()));
					return tok;

				default:
					throw new InternalException("Bad token " + tok);
				// break;

				case HASH:
					Token hashToken = tok;
					tok = source_token_nonwhite();
					// (new Exception("here")).printStackTrace();
					switch (tok.getType()) {
						case NL:
							break LEX; /* Some code has #\n */

						case IDENTIFIER:
							break;
						default:
							error(tok,
									"Preprocessor directive not a word "
											+ tok.getText());
							return source_skipline(false);
					}
					PreprocessorCommand ppcmd = PreprocessorCommand.forText(tok.getText());
					if (ppcmd == null) {
						error(tok,
								"Unknown preprocessor directive "
										+ tok.getText());
						return source_skipline(false);
					}

					switch (ppcmd) {
						case PP_DEFINE:
							if (!isActive()) {
								return source_skipline(false);
							} else {
								return define();
							}

						case PP_UNDEF:
							if (!isActive()) {
								return source_skipline(false);
							} else {
								return undef();
							}

						case PP_INCLUDE:
							if (!isActive()) {
								return source_skipline(false);
							} else {
								return include(false);
							}

						case PP_INCLUDE_NEXT:
							if (!isActive()) {
								return source_skipline(false);
							}
							if (!getFeature(Feature.INCLUDE_NEXT)) {
								error(tok,
										"Directive include_next not enabled");
								return source_skipline(false);
							}
							return include(true);

						case PP_WARNING:
						case PP_ERROR:
							if (!isActive()) {
								return source_skipline(false);
							} else {
								error(tok, ppcmd == PP_ERROR);
							}
							break;

						case PP_IF:
							push_state();
							if (!isActive()) {
								return source_skipline(false);
							}
							expr_token = null;
							states.peek().setActive(expr(0) != 0);
							tok = expr_token(); /* unget */

							if (tok.getType() == NL)
								return tok;
							return source_skipline(true);

						case PP_ELIF:
							State state = states.peek();
							if (state.sawElse()) {
								error(tok,
										"#elif after #" + "else");
								return source_skipline(false);
							} else if (!state.isParentActive()) {
								/* Nested in skipped 'if' */
								return source_skipline(false);
							} else if (state.isActive()) {
								/* The 'if' part got executed. */
								state.setParentActive(false);
								/*
								 * This is like # else # if but with
								 * only one # end.
								 */
								state.setActive(false);
								return source_skipline(false);
							} else {
								expr_token = null;
								state.setActive(expr(0) != 0);
								tok = expr_token(); /* unget */

								if (tok.getType() == NL)
									return tok;
								return source_skipline(true);
							}

						case PP_ELSE:
							state = states.peek();
							if (state.sawElse()) {
								error(tok,
										"#" + "else after #" + "else");
								return source_skipline(false);
							} else {
								state.setSawElse();
								state.setActive(!state.isActive());
								return source_skipline(warnings.contains(Warning.ENDIF_LABELS));
							}

						case PP_IFDEF:
							push_state();
							if (!isActive()) {
								return source_skipline(false);
							} else {
								tok = source_token_nonwhite();
								// System.out.println("ifdef " + tok);
								if (tok.getType() != IDENTIFIER) {
									error(tok,
											"Expected identifier, not "
													+ tok.getText());
									return source_skipline(false);
								} else {
									String text = tok.getText();
									boolean exists = macros.containsKey(text);
									states.peek().setActive(exists);
									return source_skipline(true);
								}
							}

						case PP_IFNDEF:
							push_state();
							if (!isActive()) {
								return source_skipline(false);
							} else {
								tok = source_token_nonwhite();
								if (tok.getType() != IDENTIFIER) {
									error(tok,
											"Expected identifier, not "
													+ tok.getText());
									return source_skipline(false);
								} else {
									String text = tok.getText();
									boolean exists = macros.containsKey(text);
									states.peek().setActive(!exists);
									return source_skipline(true);
								}
							}

						case PP_ENDIF:
							pop_state();
							return source_skipline(warnings.contains(Warning.ENDIF_LABELS));

						case PP_LINE:
							return source_skipline(false);

						case PP_PRAGMA:
							if (!isActive()) {
								return source_skipline(false);
							}
							return pragma();

						// return the hash token instead and un-token the current token so that it is
						// handled next time when this method is called
						case PP_EXTENSION:
						case PP_VERSION:
							if (getFeature(Feature.GLSL_PASSTHROUGH)) {
								source_untoken(tok);
								return hashToken;
							} else {
								throw new LexerException("GLSL passthrough not enabled");
							}

						case PP_CUSTOM:
							if (getFeature(Feature.GLSL_CUSTOM_PASSTHROUGH)) {
								source_untoken(tok);
								return hashToken;
							} else {
								throw new LexerException("GLSL custom passthrough not enabled");
							}

						default:
							/*
							 * Actual unknown directives are
							 * processed above. If we get here,
							 * we succeeded the map lookup but
							 * failed to handle it. Therefore,
							 * this is (unconditionally?) fatal.
							 */
							// if (isActive()) /* XXX Could be warning. */
							throw new InternalException(
									"Internal error: Unknown directive "
											+ tok);
						// return source_skipline(false);
					}

			}
		}
	}

	@NonNull
	private Token token_nonwhite() {
		Token tok;
		do {
			tok = _token();
		} while (isWhite(tok));
		return tok;
	}

	/**
	 * Returns the next preprocessor token.
	 *
	 * @see Token
	 * @return The next fully preprocessed token.
	 */
	@NonNull
	public Token token() {
		Token tok = _token();
		if (getFeature(Feature.DEBUG))
			LOG.debug("pp: Returning {}", tok);
		return tok;
	}

	public void printTo(StringBuilder builder) {
		while (true) {
			Token token = token();
			if (token == null) {
				return;
			}
			switch (token.getType()) {
				case EOF:
					return;
				case CCOMMENT:
				case CPPCOMMENT:
					if (!getFeature(Feature.KEEP_COMMENTS)) {
						builder.append(' ');
						break;
					}
				default:
					builder.append(token.getText());
					break;
			}
		}
	}

	public StringBuilder print() {
		StringBuilder sb = new StringBuilder();
		printTo(sb);
		return sb;
	}

	public String printToString() {
		return print().toString();
	}

	@Override
	public String toString() {
		StringBuilder buf = new StringBuilder();

		Source s = getSource();
		while (s != null) {
			buf.append(" -> ").append(s).append("\n");
			s = s.getParent();
		}

		Map<String, Macro> macros = new TreeMap<>(getMacros());
		for (Macro macro : macros.values()) {
			buf.append("#").append("macro ").append(macro).append("\n");
		}

		return buf.toString();
	}

	@Override
	public void close() {
		{
			Source s = source;
			while (s != null) {
				s.close();
				s = s.getParent();
			}
		}
		for (Source s : inputs) {
			s.close();
		}
	}
}
