package io.github.douira.glsl_preprocessor;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

/**
 *
 * @author shevek
 */
public class NumericValueTest {

	private Token testNumericValue(String in) {
		StringLexerSource s = new StringLexerSource(in);
		try {
			Token tok = s.token();
			System.out.println("Token is " + tok);
			assertEquals(Token.NUMBER, tok.getType());

			Token eof = s.token();
			assertEquals(Token.EOF, eof.getType(), "Didn't get EOF, but " + tok);
			return tok;
		} finally {
			s.close();
		}
	}

	private void testNumericValue(String in, double out) {
		System.out.println("Testing '" + in + "' -> " + out);
		Token tok = testNumericValue(in);
		assertEquals(in, tok.getText());
		NumericValue value = (NumericValue) tok.getValue();
		assertEquals(out, value.doubleValue(), 0.01d, "Double mismatch");
		assertEquals((float) out, value.floatValue(), 0.01f, "Float mismatch");
		assertEquals((long) out, value.longValue(), "Long mismatch");
		assertEquals((int) out, value.intValue(), "Integer mismatch");
	}

	@Test
	public void testNumericValue() throws Exception {

		// Zero
		testNumericValue("0", 0);

		// Decimal
		testNumericValue("1", 1);
		testNumericValue("1L", 1);
		testNumericValue("12", 12);
		testNumericValue("12L", 12);

		// Hex
		testNumericValue("0xf", 0xf);
		testNumericValue("0xfL", 0xf);
		testNumericValue("0x12", 0x12);
		testNumericValue("0x12L", 0x12);

		// Negative
		// testNumericValue("-0", 0);
		// testNumericValue("-1", -1);
		// Negative hex
		// testNumericValue("-0x56", -0x56);
		// testNumericValue("-0x102", -0x102);
		// Octal and negative octal
		testNumericValue("0673", Integer.parseInt("673", 8));
		// testNumericValue("-0673", Integer.parseInt("-673", 8));

		// Floating point
		testNumericValue(".0", 0);
		testNumericValue(".00", 0);
		testNumericValue("0.", 0);
		testNumericValue("0.0", 0);
		testNumericValue("00.0", 0);
		testNumericValue("00.", 0);

		// Sign on exponents
		testNumericValue("1e1", 1e1);
		// testNumericValue("-1e1", -1e1);
		testNumericValue("1e-1", 1e-1);
		testNumericValue("1e+1", 1e+1);

		// Hex numbers with decimal exponents
		testNumericValue("0x12e3", 0x12e3);
		testNumericValue("0x12p3", 0x12p3);

		// Octal numbers with decimal exponents
		testNumericValue("012e3", 012e3); // Fails
		testNumericValue("067e4", 067e4); // Fails

		// Issues a warning.
		try {
			testNumericValue("097", 97);
			fail("No warning.");
		} catch (LexerException e) {
		}

	}
}
