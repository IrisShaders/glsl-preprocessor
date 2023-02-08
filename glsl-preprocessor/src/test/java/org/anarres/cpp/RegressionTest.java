/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.anarres.cpp;

import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import org.slf4j.*;

/**
 *
 * @author shevek
 */
public class RegressionTest {

	private static final Logger LOG = LoggerFactory.getLogger(RegressionTest.class);

	public static Stream<Arguments> data() throws Exception {
		File dir = new File("build/resources/test/regression");
		return Arrays.stream(dir.listFiles())
				.filter(inFile -> inFile.getName().endsWith(".in"))
				.map(inFile -> {
					String name = inFile.getName().replace(".in", "");
					File outFile = new File(dir, name + ".out");
					return Arguments.of(name, inFile, outFile);
				});
	}

	@ParameterizedTest
	@MethodSource("data")
	public void testRegression(String name, File inFile, File outFile) throws Exception {
		String inText = Files.readString(inFile.toPath());
		LOG.info("Read " + name + ":\n" + inText);
		Preprocessor pp = new Preprocessor(new StringReader(inText));
		String generatedText = pp.printToString();
		LOG.info("Generated " + name + ":\n" + generatedText);
		if (outFile.exists()) {
			String outText = Files.readString(outFile.toPath());
			LOG.info("Expected " + name + ":\n" + outText);
			assertEquals(outText, inText);
		}

	}
}
