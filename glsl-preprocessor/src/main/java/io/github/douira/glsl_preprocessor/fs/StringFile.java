package io.github.douira.glsl_preprocessor.fs;

import io.github.douira.glsl_preprocessor.*;

class StringFile implements VirtualFile {
	private final String name;
	private final String content;

	public StringFile(String name, String content) {
		this.name = name;
		this.content = content;
	}

	@Override
	public boolean isFile() {
		return true;
	}

	@Override
	public Source getSource() {
		return new StringLexerSource(content, name, true);
	}

	@Override
	public String toString() {
		return name;
	}
}
