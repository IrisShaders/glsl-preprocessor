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

/**
 * A preprocessor exception.
 *
 * Note to users: I don't really like the name of this class. S.
 */
public class LexerException extends RuntimeException {
	public LexerException() {
	}

	public LexerException(String message) {
		super(message);
	}

	public LexerException(Throwable cause) {
		super(cause);
	}

	public LexerException(String message, Throwable cause) {
		super(message, cause);
	}

	public LexerException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}
}
