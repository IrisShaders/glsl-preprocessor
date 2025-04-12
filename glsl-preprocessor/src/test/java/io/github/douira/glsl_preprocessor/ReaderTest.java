package io.github.douira.glsl_preprocessor;

import static org.junit.jupiter.api.Assertions.*;

import java.io.StringReader;

import org.junit.jupiter.api.Test;

import edu.umd.cs.findbugs.annotations.NonNull;
import io.github.douira.glsl_preprocessor.test_util.ResourceFileSystem;

public class ReaderTest {
	public static String testCppReader(@NonNull String in, Feature... f) {
		System.out.println("Testing " + in);
		StringReader r = new StringReader(in);
		Preprocessor pp = new Preprocessor(r);
		try (pp) {
			pp.setFileSystem(new ResourceFileSystem());
			pp.addFeatures(f);
			return pp.printToString();
		}
	}

	@Test
	public void testCppReader()
			throws Exception {
		testCppReader("#include <test0.h>\n", Feature.LINEMARKERS);
	}

	@Test
	public void testVarargs()
			throws Exception {
		// The newlines are irrelevant, We want exactly one "foo"
		testCppReader("#include <varargs.c>\n");
	}

	@Test
	public void testPragmaOnce()
			throws Exception {
		// The newlines are irrelevant, We want exactly one "foo"
		String out = testCppReader("#include <once.c>\n", Feature.PRAGMA_ONCE);
		assertEquals("foo", out.trim());
	}

	@Test
	public void testPragmaOnceWithMarkers()
			throws Exception {
		// The newlines are irrelevant, We want exactly one "foo"
		testCppReader("#include <once.c>\n", Feature.PRAGMA_ONCE, Feature.LINEMARKERS);
	}
}
