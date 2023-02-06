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
 * A {@link Source} which lexes a file.
 *
 * The input is buffered.
 *
 * @see Source
 */
public class FileLexerSource extends InputLexerSource {

	private final String path;
	private final File file;

	/**
	 * Creates a new Source for lexing the given File.
	 *
	 * Preprocessor directives are honoured within the file.
	 */
	public FileLexerSource(@NonNull File file, @NonNull Charset charset, @NonNull String path)
			throws IOException {
		super(new FileInputStream(file), charset);
		this.file = file;
		this.path = path;
	}

	public FileLexerSource(@NonNull File file, @NonNull Charset charset)
			throws IOException {
		this(file, charset, file.getPath());
	}

	public FileLexerSource(@NonNull String path, @NonNull Charset charset)
			throws IOException {
		this(new File(path), charset, path);
	}

	@NonNull
	public File getFile() {
		return file;
	}

	/**
	 * This is not necessarily the same as getFile().getPath() in case we are in a
	 * chroot.
	 */
	@Override
	public String getPath() {
		return path;
	}

	@Override
	public String getName() {
		return getPath();
	}

	@Override
	public String toString() {
		return "file " + getPath();
	}
}
