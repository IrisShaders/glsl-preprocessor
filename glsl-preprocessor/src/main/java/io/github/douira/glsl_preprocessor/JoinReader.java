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

import java.io.*;

class JoinReader /* extends Reader */ implements Closeable {

	private final Reader in;

	// private PreprocessorListener listener;
	private LexerSource source;
	private boolean trigraphs;
	private boolean warnings;

	private int newlines;
	private boolean flushnl;
	private int[] unget;
	private int uptr;

	public JoinReader(Reader in, boolean trigraphs) {
		this.in = in;
		this.trigraphs = trigraphs;
		this.newlines = 0;
		this.flushnl = false;
		this.unget = new int[2];
		this.uptr = 0;
	}

	public JoinReader(Reader in) {
		this(in, false);
	}

	public void setTrigraphs(boolean enable, boolean warnings) {
		this.trigraphs = enable;
		this.warnings = warnings;
	}

	void init(Preprocessor pp, LexerSource s) {
		// this.listener = pp.getListener();
		this.source = s;
		setTrigraphs(pp.getFeature(Feature.TRIGRAPHS),
				pp.getWarning(Warning.TRIGRAPHS));
	}

	private int __read() {
		if (uptr > 0)
			return unget[--uptr];
		try {
			return in.read();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private void _unread(int c) {
		if (c != -1)
			unget[uptr++] = c;
		assert uptr <= unget.length : "JoinReader ungets too many characters";
	}

	protected void warning(String msg) {
		if (source != null)
			source.warning(msg);
		else
			throw new LexerException(msg);
	}

	private char trigraph(char raw, char repl) {
		if (trigraphs) {
			if (warnings)
				warning("trigraph ??" + raw + " converted to " + repl);
			return repl;
		} else {
			if (warnings)
				warning("trigraph ??" + raw + " ignored");
			_unread(raw);
			_unread('?');
			return '?';
		}
	}

	private int _read() {
		int c = __read();
		if (c == '?' && (trigraphs || warnings)) {
			int d = __read();
			if (d == '?') {
				int e = __read();
				switch (e) {
					case '(':
						return trigraph('(', '[');
					case ')':
						return trigraph(')', ']');
					case '<':
						return trigraph('<', '{');
					case '>':
						return trigraph('>', '}');
					case '=':
						return trigraph('=', '#');
					case '/':
						return trigraph('/', '\\');
					case '\'':
						return trigraph('\'', '^');
					case '!':
						return trigraph('!', '|');
					case '-':
						return trigraph('-', '~');
				}
				_unread(e);
			}
			_unread(d);
		}
		return c;
	}

	public int read() {
		if (flushnl) {
			if (newlines > 0) {
				newlines--;
				return '\n';
			}
			flushnl = false;
		}

		while (true) {
			int c = _read();
			switch (c) {
				case '\\':
					int d = _read();
					switch (d) {
						case '\n':
							newlines++;
							continue;
						case '\r':
							newlines++;
							int e = _read();
							if (e != '\n')
								_unread(e);
							continue;
						default:
							_unread(d);
							return c;
					}
				case '\r':
				case '\n':
				case '\u2028':
				case '\u2029':
				case '\u000B':
				case '\u000C':
				case '\u0085':
					flushnl = true;
					return c;
				case -1:
					if (newlines > 0) {
						newlines--;
						return '\n';
					}
				default:
					return c;
			}
		}
	}

	public int read(char cbuf[], int off, int len) {
		for (int i = 0; i < len; i++) {
			int ch = read();
			if (ch == -1)
				return i;
			cbuf[off + i] = (char) ch;
		}
		return len;
	}

	@Override
	public void close() {
		try {
			in.close();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public String toString() {
		return "JoinReader(nl=" + newlines + ")";
	}
}
