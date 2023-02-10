package io.github.douira.glsl_preprocessor;

import static io.github.douira.glsl_preprocessor.test_util.AssertUtil.*;
import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;

import au.com.origin.snapshots.Expect;
import au.com.origin.snapshots.annotations.SnapshotName;
import au.com.origin.snapshots.junit5.SnapshotExtension;
import io.github.douira.glsl_preprocessor.test_util.*;
import io.github.douira.glsl_preprocessor.test_util.TestCaseProvider.Spacing;

@ExtendWith({ SnapshotExtension.class })
public class IntegrationTest {
	private Expect expect;

	@ParameterizedTest
	@TestCaseSource(caseSet = "testPreprocessor", spacing = Spacing.TRIM_SINGLE_BOTH)
	@SnapshotName("testPreprocessor")
	void testPreprocessor(String type, String input) {
		var pp = new Preprocessor(input);
		var errors = false;
		for (var featureName : type.split(",")) {
			if (featureName.equals("errors")) {
				errors = true;
				continue;
			}
			var feature = Feature.valueOf(featureName);
			pp.addFeature(feature);
		}

		String output;
		try {
			output = pp.printToString();
			assertFalse(errors, "It should throw errors when specified.");
		} catch (Exception e) {
			if (!errors) {
				throw e;
			}
			output = e.toString();
		} finally {
			pp.close();
		}
		expect
				.scenario(type + "_" + getBase64Hash(input))
				.toMatchSnapshot(SnapshotUtil.inputOutputSnapshot(
						input, output));
	}
}
