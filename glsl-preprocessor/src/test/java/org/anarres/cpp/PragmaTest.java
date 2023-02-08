/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.anarres.cpp;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.nio.file.Files;

import org.junit.jupiter.api.Test;
import org.slf4j.*;

/**
 *
 * @author shevek
 */
public class PragmaTest {
	private static final Logger LOG = LoggerFactory.getLogger(PragmaTest.class);

	@Test
	public void testPragma() throws Exception {
		File file = new File("build/resources/test/pragma.c");
		assertTrue(file.exists());

		// create a CharSource from a file
		Preprocessor pp = new Preprocessor(Files.newBufferedReader(file.toPath()));
		pp.setListener(new DefaultPreprocessorListener());
		String output = pp.printToString();
		pp.close();
		LOG.info("Output: " + output);
		// assertTrue(output.contains("absolute-result"));
	}
}
