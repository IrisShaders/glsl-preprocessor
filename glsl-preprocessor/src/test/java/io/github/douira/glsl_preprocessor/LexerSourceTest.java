package io.github.douira.glsl_preprocessor;

import org.junit.jupiter.api.Test;

import static io.github.douira.glsl_preprocessor.PreprocessorTest.assertType;
import static io.github.douira.glsl_preprocessor.Token.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class LexerSourceTest {
//	private static final Logger LOG = LoggerFactory.getLogger(LexerSourceTest.class);

	public static void testLexerSource(String in, boolean textmatch, int... out) {
//		LOG.info("Testing '{}' => {}", in, Arrays.toString(out));
		StringLexerSource s = new StringLexerSource(in);

		StringBuilder buf = new StringBuilder();
		for (int j : out) {
			Token tok = s.token();
//			LOG.info("Token is {}", tok);
			assertType(j, tok);
			// assertEquals(col, tok.getColumn());
			buf.append(tok.getText());
		}

		Token tok = s.token();
//		LOG.info("Token is {}", tok);
		assertType(EOF, tok);

		if (textmatch) {
			assertEquals(in, buf.toString());
		}
		s.close();
	}

	@Test
	public void testLexerSource() {

		testLexerSource("int a = 5;", true,
				IDENTIFIER, WHITESPACE, IDENTIFIER, WHITESPACE,
				'=', WHITESPACE, NUMBER, ';');

		// \n is WHITESPACE because ppvalid = false
		testLexerSource("# #   \r\n\n\r \rfoo", true,
				HASH, WHITESPACE, '#', WHITESPACE, IDENTIFIER);

		// No match - trigraphs
		testLexerSource("%:%:", false, PASTE);
		testLexerSource("%:?", false, '#', '?');
		testLexerSource("%:%=", false, '#', MOD_EQ);

		testLexerSource("0x1234ffdUL 0765I", true,
				NUMBER, WHITESPACE, NUMBER);

		testLexerSource("+= -= *= /= %= <= >= >>= <<= &= |= ^= x", true,
				PLUS_EQ, WHITESPACE,
				SUB_EQ, WHITESPACE,
				MULT_EQ, WHITESPACE,
				DIV_EQ, WHITESPACE,
				MOD_EQ, WHITESPACE,
				LE, WHITESPACE,
				GE, WHITESPACE,
				RSH_EQ, WHITESPACE,
				LSH_EQ, WHITESPACE,
				AND_EQ, WHITESPACE,
				OR_EQ, WHITESPACE,
				XOR_EQ, WHITESPACE,
				IDENTIFIER);

		testLexerSource("/**/", true, CCOMMENT);
		testLexerSource("/* /**/ */", true, CCOMMENT, WHITESPACE, '*', '/');
		testLexerSource("/** ** **/", true, CCOMMENT);
		testLexerSource("//* ** **/", true, CPPCOMMENT);
		testLexerSource("'\\r' '\\xf' '\\xff' 'x' 'aa' ''", true,
				CHARACTER, WHITESPACE,
				CHARACTER, WHITESPACE,
				CHARACTER, WHITESPACE,
				CHARACTER, WHITESPACE,
				SQSTRING, WHITESPACE,
				SQSTRING);

		testLexerSource("'' 'x' 'xx'", true,
				SQSTRING, WHITESPACE, CHARACTER, WHITESPACE, SQSTRING);
	}

	@Test
	public void testNumbers() {
		testLexerSource("0", true, NUMBER);
		testLexerSource("045", true, NUMBER);
		testLexerSource("45", true, NUMBER);
		testLexerSource("0.45", true, NUMBER);
		testLexerSource("1.45", true, NUMBER);
		testLexerSource("1e6", true, NUMBER);
		testLexerSource("1.45e6", true, NUMBER);
		testLexerSource(".45e6", true, NUMBER);
		testLexerSource("-6", true, '-', NUMBER);
	}

	@Test
	public void testNumbersSuffix() {
		testLexerSource("6f", true, NUMBER);
		testLexerSource("6d", true, NUMBER);
		testLexerSource("6l", true, NUMBER);
		testLexerSource("6ll", true, NUMBER);
		testLexerSource("6ul", true, NUMBER);
		testLexerSource("6ull", true, NUMBER);
		testLexerSource("6e3f", true, NUMBER);
		testLexerSource("6e3d", true, NUMBER);
		testLexerSource("6e3l", true, NUMBER);
		testLexerSource("6e3ll", true, NUMBER);
		testLexerSource("6e3ul", true, NUMBER);
		testLexerSource("6e3ull", true, NUMBER);
	}

	@Test
	public void testNumbersInvalid() {
		// testLexerSource("0x foo", true, INVALID, WHITESPACE, IDENTIFIER); // FAIL
		testLexerSource("6x foo", true, INVALID, WHITESPACE, IDENTIFIER);
		testLexerSource("6g foo", true, INVALID, WHITESPACE, IDENTIFIER);
		testLexerSource("6xsd foo", true, INVALID, WHITESPACE, IDENTIFIER);
		testLexerSource("6gsd foo", true, INVALID, WHITESPACE, IDENTIFIER);
	}

	@Test
	public void testUnterminatedComment() {
		testLexerSource("5 /*", false, NUMBER, WHITESPACE, INVALID); // Bug #15
		testLexerSource("5 //", false, NUMBER, WHITESPACE, CPPCOMMENT);
	}

	@Test
	public void testUnicode() {
		testLexerSource("foo ‘bar’ baz", true, IDENTIFIER, WHITESPACE, 8216, IDENTIFIER, 8217,
				WHITESPACE,
				IDENTIFIER);
	}
}
