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

import static org.anarres.cpp.Token.*;

import java.util.*;

import edu.umd.cs.findbugs.annotations.NonNull;

/**
 * An Iterator for {@link Source Sources},
 * returning {@link Token Tokens}.
 */
public class SourceIterator implements Iterator<Token> {

	private final Source source;
	private Token tok;

	public SourceIterator(@NonNull Source s) {
		this.source = s;
		this.tok = null;
	}

	/**
	 * Rethrows IOException inside IllegalStateException.
	 */
	private void advance() {
		try {
			if (tok == null)
				tok = source.token();
		} catch (LexerException e) {
			throw new IllegalStateException(e);
		}
	}

	/**
	 * Returns true if the enclosed Source has more tokens.
	 *
	 * The EOF token is never returned by the iterator.
	 * 
	 * @throws IllegalStateException if the Source
	 *                               throws a LexerException or IOException
	 */
	@Override
	public boolean hasNext() {
		advance();
		return tok.getType() != EOF;
	}

	/**
	 * Returns the next token from the enclosed Source.
	 *
	 * The EOF token is never returned by the iterator.
	 * 
	 * @throws IllegalStateException if the Source
	 *                               throws a LexerException or IOException
	 */
	@Override
	public Token next() {
		if (!hasNext())
			throw new NoSuchElementException();
		Token t = this.tok;
		this.tok = null;
		return t;
	}

	/**
	 * Not supported.
	 *
	 * @throws UnsupportedOperationException unconditionally.
	 */
	@Override
	public void remove() {
		throw new UnsupportedOperationException();
	}
}
