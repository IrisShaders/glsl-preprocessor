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

import static io.github.douira.glsl_preprocessor.Token.*;

import java.io.Closeable;
import java.util.Iterator;

import edu.umd.cs.findbugs.annotations.*;

/**
 * An input to the Preprocessor.
 *
 * Inputs may come from Files, Strings or other sources. The
 * preprocessor maintains a stack of Sources. Operations such as
 * file inclusion or token pasting will push a new source onto
 * the Preprocessor stack. Sources pop from the stack when they
 * are exhausted; this may be transparent or explicit.
 *
 * BUG: Error messages are not handled properly.
 */
public abstract class Source implements Iterable<Token>, Closeable {

	private Source parent;
	private boolean autopop;
	private PreprocessorListener listener;
	private boolean active;
	private boolean werror;

	/* LineNumberReader */

	/*
	 * // We can't do this, since we would lose the LexerException
	 * private class Itr implements Iterator {
	 * private Token next = null;
	 * private void advance() {
	 * try {
	 * if (next != null)
	 * next = token();
	 * }
	 * catch (IOException e) {
	 * throw new UnsupportedOperationException(
	 * "Failed to advance token iterator: " +
	 * e.getMessage()
	 * );
	 * }
	 * }
	 * public boolean hasNext() {
	 * return next.getType() != EOF;
	 * }
	 * public Token next() {
	 * advance();
	 * Token t = next;
	 * next = null;
	 * return t;
	 * }
	 * public void remove() {
	 * throw new UnsupportedOperationException(
	 * "Cannot remove tokens from a Source."
	 * );
	 * }
	 * }
	 */
	public Source() {
		this.parent = null;
		this.autopop = false;
		this.listener = null;
		this.active = true;
		this.werror = false;
	}

	/**
	 * Sets the parent source of this source.
	 *
	 * Sources form a singly linked list.
	 */
	void setParent(Source parent, boolean autopop) {
		this.parent = parent;
		this.autopop = autopop;
	}

	/**
	 * Returns the parent source of this source.
	 *
	 * Sources form a singly linked list.
	 */
	final Source getParent() {
		return parent;
	}

	// @OverrideMustInvoke
	void init(Preprocessor pp) {
		setListener(pp.getListener());
		this.werror = pp.getWarnings().contains(Warning.ERROR);
	}

	/**
	 * Sets the listener for this Source.
	 *
	 * Normally this is set by the Preprocessor when a Source is
	 * used, but if you are using a Source as a standalone object,
	 * you may wish to call this.
	 */
	public void setListener(PreprocessorListener pl) {
		this.listener = pl;
	}

	/**
	 * Returns the File currently being lexed.
	 */
	@CheckForNull
	public String getPath() {
		Source parent = getParent();
		if (parent != null)
			return parent.getPath();
		return null;
	}

	/**
	 * Returns the human-readable name of the current Source.
	 */
	@CheckForNull
	public String getName() {
		Source parent = getParent();
		if (parent != null)
			return parent.getName();
		return null;
	}

	/**
	 * Returns the current line number within this Source.
	 */

	public int getLine() {
		Source parent = getParent();
		if (parent == null)
			return 0;
		return parent.getLine();
	}

	/**
	 * Returns the current column number within this Source.
	 */
	public int getColumn() {
		Source parent = getParent();
		if (parent == null)
			return 0;
		return parent.getColumn();
	}

	/**
	 * Returns true if this Source is expanding the given macro.
	 *
	 * This is used to prevent macro recursion.
	 */
	boolean isExpanding(@NonNull Macro m) {
		Source parent = getParent();
		if (parent != null)
			return parent.isExpanding(m);
		return false;
	}

	/**
	 * Returns true if this Source should be transparently popped
	 * from the input stack.
	 *
	 * Examples of such sources are macro expansions.
	 */
	boolean isAutopop() {
		return autopop;
	}

	/**
	 * Returns true if this source has line numbers.
	 */
	boolean isNumbered() {
		return false;
	}

	/*
	 * This is an incredibly lazy way of disabling warnings when
	 * the source is not active.
	 */
	void setActive(boolean b) {
		this.active = b;
	}

	boolean isActive() {
		return active;
	}

	/**
	 * Returns the next Token parsed from this input stream.
	 *
	 * @see Token
	 */
	@NonNull
	public abstract Token token();

	/**
	 * Returns a token iterator for this Source.
	 */
	@Override
	public Iterator<Token> iterator() {
		return new SourceIterator(this);
	}

	/**
	 * Skips tokens until the end of line.
	 *
	 * @param white true if only whitespace is permitted on the
	 *              remainder of the line.
	 * @return the NL token.
	 */
	@NonNull
	public Token skipline(boolean white) {
		while (true) {
			Token tok = token();
			switch (tok.getType()) {
				case EOF:
					/*
					 * There ought to be a newline before EOF.
					 * At least, in any skipline context.
					 */
					/* XXX Are we sure about this? */
					warning(tok.getLine(), tok.getColumn(),
							"No newline before end of file");
					return new Token(NL,
							tok.getLine(), tok.getColumn(),
							"\n");
				// return tok;
				case NL:
					/* This may contain one or more newlines. */
					return tok;
				case CCOMMENT:
				case CPPCOMMENT:
				case WHITESPACE:
					break;
				default:
					/* XXX Check white, if required. */
					if (white)
						warning(tok.getLine(), tok.getColumn(),
								"Unexpected nonwhite token");
					break;
			}
		}
	}

	protected void error(int line, int column, String msg) {
		if (listener != null)
			listener.handleError(this, line, column, msg);
		else
			throw new LexerException("Error at " + line + ":" + column + ": " + msg);
	}

	protected void warning(int line, int column, String msg) {
		if (werror)
			error(line, column, msg);
		else if (listener != null)
			listener.handleWarning(this, line, column, msg);
		else
			throw new LexerException("Warning at " + line + ":" + column + ": " + msg);
	}

	public void close() {
	}
}
