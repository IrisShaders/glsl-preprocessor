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
package io.github.douira.glsl_preprocessor.fs;

import edu.umd.cs.findbugs.annotations.NonNull;

/**
 * An extremely lightweight virtual file system interface.
 */
public interface VirtualFileSystem {
	public static final MemoryFileSystem EMPTY = new MemoryFileSystem();

	@NonNull
	public VirtualFile getFile(@NonNull String path);

	@NonNull
	public default VirtualFile getFile(@NonNull String dir, @NonNull String name) {
		return getFile(dir + '/' + name);
	}
}
