package io.github.mov2day.unifiedtest.collector;

import org.gradle.api.tasks.testing.TestDescriptor;
import org.gradle.api.tasks.testing.TestListener;
import org.gradle.api.tasks.testing.TestResult;
import java.util.List;
import java.util.ArrayList;

/**
 * Collects and stores test results from various test frameworks.
 * Implements TestListener to receive test execution events.
 */
public class UnifiedTestResultCollector implements TestListener {
    private final List<UnifiedTestResult> results = new ArrayList<>();
    @Override public void beforeSuite(TestDescriptor suite) {}
    @Override public void afterSuite(TestDescriptor suite, TestResult result) {}
    @Override public void beforeTest(TestDescriptor testDescriptor) {}
    @Override public void afterTest(TestDescriptor testDescriptor, TestResult result) {
        String status = result.getResultType().toString();
        results.add(new UnifiedTestResult(
            testDescriptor.getClassName(),
            testDescriptor.getName(),
            status
        ));
    }

    /**
     * Adds a test result to the collection.
     * @param result the test result to add
     */
    public void addResult(UnifiedTestResult result) {
        results.add(result);
    }

    /**
     * Checks if a result exists for the specified test.
     * @param className the test class name
     * @param testName the test method name
     * @return true if a result exists for this test
     */
    public boolean hasResult(String className, String testName) {
        return results.stream().anyMatch(r -> r.className.equals(className) && r.testName.equals(testName));
    }

    /**
     * Gets all collected test results.
     * @return list of all test results
     */
    public List<UnifiedTestResult> getResults() { return results; }
}
