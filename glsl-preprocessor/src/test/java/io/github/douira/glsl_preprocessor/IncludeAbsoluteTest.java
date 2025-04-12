package io.github.douira.glsl_preprocessor;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;

import org.junit.jupiter.api.Test;
import org.slf4j.*;

import io.github.douira.glsl_preprocessor.fs.MemoryFileSystem;

/**
 *
 * @author shevek
 */
public class IncludeAbsoluteTest {
	private static final Logger LOG = LoggerFactory.getLogger(IncludeAbsoluteTest.class);

	@Test
	public void testAbsoluteInclude() {
		File file = new File("build/resources/test/absolute.h");
		String input = "#include <" + file.getAbsolutePath() + ">\n";
		LOG.info("Input: {}", input);
		MemoryFileSystem fs = new MemoryFileSystem();
		fs.addFile(file.getAbsolutePath(), "absolute-result");
		Preprocessor pp = new Preprocessor();
		pp.setFileSystem(fs);
		pp.addInput(new StringLexerSource(input, true));
		String output = pp.printToString();
		pp.close();
		LOG.info("Output: {}", output);
		assertTrue(output.contains("absolute-result"));
	}
}
