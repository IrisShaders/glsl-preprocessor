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

/**
 * Features of the Preprocessor, which may be enabled or disabled.
 */
public enum Feature {
	/** Supports ANSI digraphs. */
	DIGRAPHS,

	/** Supports ANSI trigraphs. */
	TRIGRAPHS,

	/** Outputs line marker tokens. */
	LINE_MARKERS,

	/** Outputs line marker tokens by file name. */
	NAMED_LINE_MARKERS,

	/**
	 * Adds flags to the line markers that distinguish what role they have.
	 */
	LINE_MARKER_FLAGS,

	/** Reports tokens of type INVALID as errors. */
	C_SYNTAX,

	/** Preserves comments in the lexed output. Like cpp -C */
	KEEP_COMMENTS,

	/** Preserves comments in the lexed output, even when inactive. */
	KEEP_ALL_COMMENTS,

	/** enable debug messages */
	DEBUG,

	INCLUDE_NEXT,

	/** enables marking the source a #pragma once appears in to only be included once */
	PRAGMA_ONCE,

	/** GLSL support */
	GLSL_PASSTHROUGH,
	GLSL_CUSTOM_PASSTHROUGH,
	/** arbitrary pragmas, which are not passed through though, but won't cause errors if this is enabled */
	ARBITRARY_PRAGMAS,
	/** #custom directive, which is passed through */
	CUSTOM_DIRECTIVE,
}
