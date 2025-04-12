package io.github.douira.glsl_preprocessor;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.slf4j.*;

/**
 * <a href="https://github.com/shevek/jcpp/issues/25">...</a>
 *
 * @author shevek
 */
public class TokenPastingWhitespaceTest {
	private static final Logger LOG = LoggerFactory.getLogger(TokenPastingWhitespaceTest.class);

	@Test
	public void testWhitespacePasting() {
		Preprocessor pp = new Preprocessor();
		pp.addInput(new StringLexerSource(
				"""
						#define ONE(arg) one_##arg
						#define TWO(arg) ONE(two_##arg)
						
						TWO(good)
						TWO(     /* evil newline */
						    bad)
						
						ONE(good)
						ONE(     /* evil newline */
						    bad)
						""",
				true));
		String output;
		try {
			output = pp.printToString().trim();
		} catch (Exception e) {
			throw new RuntimeException(e);
		} finally {
			pp.close();
		}
		LOG.info("Output is:\n{}", output);
		assertEquals("""
				one_two_good
				one_two_bad
				
				one_good
				one_bad""", output);
	}
}
