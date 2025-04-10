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

import java.io.StringReader;

/**
 * A Source for lexing a String.
 *
 * This class is used by token pasting, but can be used by user
 * code.
 */
public class StringLexerSource extends LexerSource {

	private final String name;

	/**
	 * Creates a new Source for lexing the given String.
	 *
	 * @param string  The input string to lex.
	 * @param name  The name of this source.
	 * @param ppvalid true if preprocessor directives are to be
	 *                honoured within the string.
	 */
	public StringLexerSource(String string, String name, boolean ppvalid) {
		super(new StringReader(string), ppvalid);
		this.name = name;
	}

	/**
	 * Creates a new Source for lexing the given String.
	 *
	 * Equivalent to calling <code>new StringLexerSource(string, false)</code>.
	 *
	 * By default, preprocessor directives are not honoured within
	 * the string.
	 *
	 * @param string The input string to lex.
	 */
	public StringLexerSource(String string) {
		this(string, "string literal", false);
	}

	/**
	 * Creates a new Source for lexing the given String.
	 *
	 * Equivalent to calling <code>new StringLexerSource(string, "string literal", ppvalid)</code>.
	 *
	 * @param string The input string to lex.
	 * @param ppvalid true if preprocessor directives are to be
	 *                honoured within the string.
	 */
	public StringLexerSource(String string, boolean ppvalid) {
		this(string, "string literal", ppvalid);
	}

	@Override
	public String toString() {
		return "string literal";
	}

	@Override
	public String getName() {
		return name;
	}
}
