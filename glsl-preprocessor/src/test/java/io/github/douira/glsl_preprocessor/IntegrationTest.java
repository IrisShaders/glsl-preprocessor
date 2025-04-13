package io.github.douira.glsl_preprocessor;

import au.com.origin.snapshots.Expect;
import au.com.origin.snapshots.annotations.SnapshotName;
import au.com.origin.snapshots.junit5.SnapshotExtension;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import io.github.douira.glsl_preprocessor.fs.MemoryFileSystem;
import io.github.douira.glsl_preprocessor.fs.StringFile;
import io.github.douira.glsl_preprocessor.fs.VirtualFile;
import io.github.douira.glsl_preprocessor.fs.VirtualFileSystem;
import io.github.douira.glsl_preprocessor.test_util.SnapshotUtil;
import io.github.douira.glsl_preprocessor.test_util.TestCaseProvider.Spacing;
import io.github.douira.glsl_preprocessor.test_util.TestCaseSource;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;

import java.util.function.Consumer;

import static io.github.douira.glsl_preprocessor.test_util.AssertUtil.getBase64Hash;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith({SnapshotExtension.class})
public class IntegrationTest {
	private Expect expect;

	private void preprocessorTestCase(String type, String input, String expectedOutput, @Nullable Consumer<Preprocessor> prepare) {
		Preprocessor pp = new Preprocessor(input);
		var errors = false;
		for (var featureName : type.split(",")) {
			if (featureName.equals("errors")) {
				errors = true;
				continue;
			}
			var feature = Feature.valueOf(featureName);
			pp.addFeature(feature);
		}

		if (prepare != null) {
			prepare.accept(pp);
		}

		var snapshotOutput = new StringBuilder();
		if (errors) {
			var e = assertThrows(Exception.class, pp::printToString);
			pp.close();
			snapshotOutput.append(e.toString());
		} else {
			var output = pp.printToString();
			assertEquals(expectedOutput, output);
			snapshotOutput.append("(no error)");
		}
		
		for (var entry : pp.getSourceNumbers().entrySet()) {
			var source = entry.getKey();
			if (source == null) {
				continue;
			}
			var sourceNumber = entry.getValue();
			snapshotOutput.append("\n");
			snapshotOutput.append(sourceNumber);
			snapshotOutput.append("->");
			snapshotOutput.append(source);
		}

		expect
				.scenario(type + "_" + getBase64Hash(input))
				.toMatchSnapshot(SnapshotUtil.inputOutputSnapshot(
						input, snapshotOutput.toString()));
	}

	@SnapshotName("testPreprocessor")
	@ParameterizedTest
	@TestCaseSource(caseSet = "testPreprocessor", spacing = Spacing.TRIM_SINGLE_BOTH)
	void testPreprocessor(String type, String input, String expectedOutput) {
		preprocessorTestCase(type, input, expectedOutput, null);
	}

	private static class SyntheticLengthContentFileSystem implements VirtualFileSystem {
		@NonNull
		@Override
		public VirtualFile getFile(@NonNull String path) {
			var prefix = "length!";
			if (!path.startsWith(prefix)) {
				throw new IllegalArgumentException("File not found: " + path);
			}
			var command = path.substring(prefix.length()).split(":");
			var lengthTarget = Integer.parseInt(command[0]);
			var builder = new StringBuilder();
			for (int i = 0; i < lengthTarget; i++) {
				builder.append("content ");
				builder.append(i);
				builder.append('\n');
			}
			return new StringFile(path, builder.toString());
		}
	}

	@SnapshotName("testIncludeLineMapping")
	@ParameterizedTest
	@TestCaseSource(caseSet = "testIncludeLineMapping", spacing = Spacing.TRIM_SINGLE_BOTH)
	void testIncludeLineMapping(String type, String input, String expectedOutput) {
		preprocessorTestCase(type, input, expectedOutput, pp -> {
			pp.setFileSystem(new SyntheticLengthContentFileSystem());
		});
	}

	@SnapshotName("testIncludeLineMappingExplicitFiles")
	@ParameterizedTest
	@TestCaseSource(caseSet = "testIncludeLineMappingExplicitFiles", spacing = Spacing.TRIM_SINGLE_BOTH)
	void testIncludeLineMappingExplicitFiles(String type, String input, String expectedOutput) {
		var segments = input.split("@");
		var fs = new MemoryFileSystem();
		String ppInput = null;
		for (var segment : segments) {
			if (segment.isEmpty()) {
				continue;
			}
			var parts = segment.split("\n", 2);
			if (parts.length < 2) {
				throw new IllegalArgumentException("Invalid input: " + segment);
			}
			var path = parts[0].trim();
			var content = parts[1];
			if (path.equals("main")) {
				ppInput = content;
			} else {
				fs.addFile(path, content);
			}
		}
		preprocessorTestCase(type, ppInput, expectedOutput, pp -> {
			pp.setFileSystem(fs);
		});
	}
}
