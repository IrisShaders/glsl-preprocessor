package io.github.douira.glsl_preprocessor;

import static org.junit.jupiter.api.Assertions.*;

import java.io.StringReader;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

import edu.umd.cs.findbugs.annotations.NonNull;
import io.github.douira.glsl_preprocessor.test_util.ResourceFileSystem;

public class ReaderTest {
	public static String testCppReader(@NonNull String in, Feature... f) {
//		System.out.println("Testing " + in);
		StringReader r = new StringReader(in);
		Preprocessor pp = new Preprocessor(r);
		try (pp) {
			pp.setFileSystem(new ResourceFileSystem());
			pp.addFeatures(f);
			return pp.printToString();
		}
	}

	@Test
	public void testCppReader() {
		testCppReader("#include <test0.h>\n", Feature.LINE_MARKERS);
	}

	@Test
	public void testVarargs() {
		// The newlines are irrelevant, We want exactly one "foo"
		testCppReader("#include <varargs.c>\n");
	}

	@Test
	public void testPragmaOnce() {
		// The newlines are irrelevant, We want exactly one "foo"
		String out = testCppReader("#include <once.c>\n", Feature.PRAGMA_ONCE);
		assertEquals("foo", out.trim());
	}

	@Test
	public void testPragmaOnceWithMarkers() {
		// The newlines are irrelevant, We want exactly one "foo"
		var out = testCppReader("#include <once.c>\n", Feature.PRAGMA_ONCE, Feature.LINE_MARKERS);
		assertEquals(1, Pattern.compile("foo").matcher(out).results().count());
	}
}
