package io.github.douira.glsl_preprocessor.fs;

import java.io.IOException;
import java.util.*;

public class MemoryFileSystem implements VirtualFileSystem {
	private final Map<String, VirtualFile> files = new HashMap<String, VirtualFile>();

	public MemoryFileSystem() {
	}

	public MemoryFileSystem(Map<String, VirtualFile> files) {
		this.files.putAll(files);
	}

	public void addFile(String path, VirtualFile file) {
		files.put(path, file);
	}

	public void addFile(String path, String content) {
		addFile(path, new StringFile(path, content));
	}

	@Override
	public VirtualFile getFile(String path) {
		if (!files.containsKey(path)) {
			throw new RuntimeException(new IOException("File not found: " + path));
		}
		return files.get(path);
	}
}
