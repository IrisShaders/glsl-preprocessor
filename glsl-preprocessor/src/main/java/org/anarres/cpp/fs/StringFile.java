package org.anarres.cpp.fs;

import org.anarres.cpp.*;

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
