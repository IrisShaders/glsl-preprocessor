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
package org.anarres.cpp;

import java.io.*;
import java.nio.charset.Charset;

import edu.umd.cs.findbugs.annotations.NonNull;

/**
 * A {@link Source} which lexes an {@link InputStream}.
 *
 * The input is buffered.
 *
 * @see Source
 */
public class InputLexerSource extends LexerSource {
	/**
	 * Creates a new Source for lexing the given Reader.
	 *
	 * Preprocessor directives are honoured within the file.
	 */
	public InputLexerSource(@NonNull InputStream input, Charset charset) {
		this(new InputStreamReader(input, charset));
	}

	public InputLexerSource(@NonNull Reader input, boolean ppvalid) {
		super(input, true);
	}

	public InputLexerSource(@NonNull Reader input) {
		this(input, true);
	}

	@Override
	public String getPath() {
		return "<standard-input>";
	}

	@Override
	public String getName() {
		return "standard input";
	}

	@Override
	public String toString() {
		return String.valueOf(getPath());
	}
}
