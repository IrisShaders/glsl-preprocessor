/**
 * Missing license header, assuming there was supposed to be one.
 * 
 * Modified by the contributors of glsl-preprocessor.
 */
package io.github.douira.glsl_preprocessor.test_util;

import java.io.InputStream;
import java.nio.charset.Charset;

import edu.umd.cs.findbugs.annotations.NonNull;
import io.github.douira.glsl_preprocessor.*;
import io.github.douira.glsl_preprocessor.fs.*;

/**
 *
 * @author shevek
 */
public class ResourceFileSystem implements VirtualFileSystem {
	private final ClassLoader loader;
	private final Charset charset;

	public ResourceFileSystem(@NonNull ClassLoader loader, @NonNull Charset charset) {
		this.loader = loader;
		this.charset = charset;
	}

	public ResourceFileSystem(@NonNull ClassLoader loader) {
		this(loader, Charset.defaultCharset());
	}

	public ResourceFileSystem() {
		this(ClassLoader.getSystemClassLoader());
	}

	@NonNull
	@Override
	public VirtualFile getFile(@NonNull String path) {
		return new ResourceFile(loader, path);
	}
	
	private class ResourceFile implements VirtualFile {

		private final ClassLoader loader;
		private final String path;

		public ResourceFile(ClassLoader loader, String path) {
			this.loader = loader;
			this.path = path;
		}

		@Override
		public boolean isFile() {
			return loader.getResource(path) != null;
		}

		@NonNull
		@Override
		public Source getSource() {
			InputStream stream = loader.getResourceAsStream(path);
			return new InputLexerSource(stream, charset);
		}
	}
}
