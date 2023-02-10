package io.github.douira.glsl_preprocessor.fs;

import io.github.douira.glsl_preprocessor.*;

class StringFile implements VirtualFile {
	private final String content;

	public StringFile(String content) {
		this.content = content;
	}

	@Override
	public boolean isFile() {
		return true;
	}

	@Override
	public Source getSource() {
		return new StringLexerSource(content);
	}
}
