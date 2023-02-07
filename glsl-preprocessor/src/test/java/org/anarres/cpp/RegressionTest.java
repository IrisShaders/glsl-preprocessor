/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.anarres.cpp;

import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.util.Arrays;
import java.util.stream.Stream;

import org.anarres.cpp.test_util.CppReader;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import org.slf4j.*;

import com.google.common.base.Charsets;
import com.google.common.io.*;

/**
 *
 * @author shevek
 */
public class RegressionTest {

	private static final Logger LOG = LoggerFactory.getLogger(RegressionTest.class);

	public static Stream<Arguments> data() throws Exception {
		File dir = new File("build/resources/test/regression");
		return Arrays.stream(dir.listFiles(new PatternFilenameFilter(".*\\.in"))).map(inFile -> {
			String name = Files.getNameWithoutExtension(inFile.getName());
			File outFile = new File(dir, name + ".out");
			return Arguments.of(name, inFile, outFile);
		});
	}

	@ParameterizedTest
	@MethodSource("data")
	public void testRegression(String name, File inFile, File outFile) throws Exception {
		String inText = Files.toString(inFile, Charsets.UTF_8);
		LOG.info("Read " + name + ":\n" + inText);
		CppReader cppReader = new CppReader(new StringReader(inText));
		String cppText = CharStreams.toString(cppReader);
		LOG.info("Generated " + name + ":\n" + cppText);
		if (outFile.exists()) {
			String outText = Files.toString(outFile, Charsets.UTF_8);
			LOG.info("Expected " + name + ":\n" + outText);
			assertEquals(outText, inText);
		}

	}
}
