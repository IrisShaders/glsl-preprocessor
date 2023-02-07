package org.anarres.cpp;

import static org.junit.jupiter.api.Assertions.*;

import java.io.*;

import org.anarres.cpp.fs.MemoryFileSystem;
import org.anarres.cpp.test_util.CppReader;
import org.junit.jupiter.api.Test;
import org.slf4j.*;

import com.google.common.io.CharStreams;

/**
 *
 * @author shevek
 */
public class IncludeAbsoluteTest {

	private static final Logger LOG = LoggerFactory.getLogger(IncludeAbsoluteTest.class);

	@Test
	public void testAbsoluteInclude() throws Exception {
		File file = new File("build/resources/test/absolute.h");
		String input = "#include <" + file.getAbsolutePath() + ">\n";
		LOG.info("Input: " + input);
		MemoryFileSystem fs = new MemoryFileSystem();
		fs.addFile(file.getAbsolutePath(), "absolute-result");
		Preprocessor pp = new Preprocessor(fs);
		pp.addInput(new StringLexerSource(input, true));
		Reader r = new CppReader(pp);
		String output = CharStreams.toString(r);
		r.close();
		LOG.info("Output: " + output);
		assertTrue(output.contains("absolute-result"));
	}
}
