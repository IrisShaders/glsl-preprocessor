package org.anarres.cpp.fs;
import java.io.IOException;

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
	public Source getSource() throws IOException {
		return new StringLexerSource(content);
	}
}
