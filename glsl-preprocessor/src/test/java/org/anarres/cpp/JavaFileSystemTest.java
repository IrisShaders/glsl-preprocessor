package org.anarres.cpp;

import static org.junit.jupiter.api.Assertions.*;

import java.io.FileNotFoundException;

import org.junit.jupiter.api.Test;

public class JavaFileSystemTest {

	@Test
	public void testJavaFileSystem() throws Exception {
		JavaFileSystem fs = new JavaFileSystem();
		VirtualFile f;

		/* Anyone who has this file on their Unix box is messed up. */
		f = fs.getFile("/foo/bar baz");
		try {
			f.getSource(); /* drop on floor */

			assertTrue(f.isFile(), "Got a source for a non-file");
		} catch (FileNotFoundException e) {
			assertFalse(f.isFile(), "Got no source for a file");
		}

		/* We hope we have this. */
		f = fs.getFile("/usr/include/stdio.h");
		try {
			f.getSource(); /* drop on floor */

			System.out.println("Opened stdio.h");
			assertTrue(f.isFile(), "Got a source for a non-file");
		} catch (FileNotFoundException e) {
			System.out.println("Failed to open stdio.h");
			assertFalse(f.isFile(), "Got no source for a file");
		}

	}
}
