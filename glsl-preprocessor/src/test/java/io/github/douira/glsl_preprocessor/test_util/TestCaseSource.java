package io.github.douira.glsl_preprocessor.test_util;

import java.lang.annotation.*;

import org.junit.jupiter.params.provider.ArgumentsSource;

import io.github.douira.glsl_preprocessor.test_util.TestCaseProvider.Spacing;

/**
 * Provides parameterized test arguments from a .cases test case file. The test
 * cases with the given name are read. If no name is provided, the name of the
 * snapshot is used instead. If that is also no set, then the name of the method
 * is used.
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@ArgumentsSource(TestCaseProvider.class)
public @interface TestCaseSource {
	/**
	 * The name of the test case set to use.
	 * 
	 * @return The name of the test case set
	 */
	String caseSet() default "";

	/**
	 * The spacing type to use when parsing test cases from the file.
	 * 
	 * @return The spacing type
	 */
	Spacing spacing() default Spacing.NONE;
}
