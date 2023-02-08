package org.anarres.cpp;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;

import org.junit.jupiter.api.Test;
import org.slf4j.*;

/**
 * https://github.com/shevek/jcpp/issues/25
 *
 * @author shevek
 */
public class TokenPastingWhitespaceTest {

	private static final Logger LOG = LoggerFactory.getLogger(TokenPastingWhitespaceTest.class);

	@Test
	public void testWhitespacePasting() throws IOException {
		Preprocessor pp = new Preprocessor();
		pp.addInput(new StringLexerSource(
				"#define ONE(arg) one_##arg\n"
						+ "#define TWO(arg) ONE(two_##arg)\n"
						+ "\n"
						+ "TWO(good)\n"
						+ "TWO(     /* evil newline */\n"
						+ "    bad)\n"
						+ "\n"
						+ "ONE(good)\n"
						+ "ONE(     /* evil newline */\n"
						+ "    bad)\n",
				true));
		String output;
		try {
			output = pp.printToString().trim();
		} catch (Exception e) {
			throw new RuntimeException(e);
		} finally {
			pp.close();
		}
		LOG.info("Output is:\n" + output);
		assertEquals("one_two_good\n"
				+ "one_two_bad\n"
				+ "\n"
				+ "one_good\n"
				+ "one_bad", output);
	}
}
