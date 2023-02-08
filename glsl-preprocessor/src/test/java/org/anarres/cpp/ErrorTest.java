package org.anarres.cpp;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

public class ErrorTest {
	private boolean testError(Preprocessor p) {
		while (true) {
			Token tok = p.token();
			if (tok.getType() == Token.EOF)
				break;
			if (tok.getType() == Token.INVALID)
				return true;
		}
		return false;
	}

	private void testError(String input) throws Exception {
		StringLexerSource sl;
		DefaultPreprocessorListener pl;
		Preprocessor p;

		/* Without a PreprocessorListener, throws an exception. */
		sl = new StringLexerSource(input, true);
		p = new Preprocessor();
		p.addFeature(Feature.CSYNTAX);
		p.addInput(sl);
		try {
			assertTrue(testError(p));
			fail("Lexing unexpectedly succeeded without listener.");
		} catch (LexerException e) {
			/* required */
		}

		/* With a PreprocessorListener, records the error. */
		sl = new StringLexerSource(input, true);
		p = new Preprocessor();
		p.addFeature(Feature.CSYNTAX);
		p.addInput(sl);
		pl = new DefaultPreprocessorListener();
		p.setListener(pl);
		assertNotNull(p.getListener(), "CPP has listener");
		assertTrue(testError(p));
		assertTrue(pl.getErrors() > 0, "Listener has errors");

		/* Without CSYNTAX, works happily. */
		sl = new StringLexerSource(input, true);
		p = new Preprocessor();
		p.addInput(sl);
		assertTrue(testError(p));
	}

	@Test
	public void testErrors() throws Exception {
		testError("\"");
		testError("'");
		// testError("''");
	}
}
