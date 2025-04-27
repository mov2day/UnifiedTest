package io.github.mov2day.unifiedtest.collector;

import org.gradle.api.tasks.testing.TestDescriptor;
import org.gradle.api.tasks.testing.TestListener;
import org.gradle.api.tasks.testing.TestResult;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * Collects and stores test results from various test frameworks.
 * Implements TestListener to receive test execution events.
 */
public class UnifiedTestResultCollector implements TestListener {
    private final List<UnifiedTestResult> results = new ArrayList<>();
    private final Map<String, UnifiedTestResult> resultMap = new ConcurrentHashMap<>();

    @Override public void beforeSuite(TestDescriptor suite) {}
    @Override public void afterSuite(TestDescriptor suite, TestResult result) {}
    @Override public void beforeTest(TestDescriptor testDescriptor) {}
    
    @Override 
    public void afterTest(TestDescriptor testDescriptor, TestResult result) {
        String status;
        switch (result.getResultType()) {
            case SUCCESS:
                status = "PASS";
                break;
            case FAILURE:
                status = "FAIL";
                break;
            case SKIPPED:
                status = "SKIP";
                break;
            default:
                status = result.getResultType().toString();
        }

        long duration = result.getEndTime() - result.getStartTime();
        String message = null;
        String trace = null;

        if (result.getException() != null) {
            message = result.getException().getMessage();
            StringWriter sw = new StringWriter();
            result.getException().printStackTrace(new PrintWriter(sw));
            trace = sw.toString();
        }

        UnifiedTestResult testResult = new UnifiedTestResult(
            testDescriptor.getClassName(),
            testDescriptor.getName(),
            status,
            message,
            trace,
            duration
        );

        String key = testDescriptor.getClassName() + "." + testDescriptor.getName();
        resultMap.put(key, testResult);
        results.add(testResult);
    }

    /**
     * Adds a test result to the collection.
     * @param result the test result to add
     */
    public void addResult(UnifiedTestResult result) {
        String key = result.className + "." + result.testName;
        if (!resultMap.containsKey(key)) {
            resultMap.put(key, result);
            results.add(result);
        }
    }

    /**
     * Checks if a result exists for the specified test.
     * @param className the test class name
     * @param testName the test method name
     * @return true if a result exists for this test
     */
    public boolean hasResult(String className, String testName) {
        return resultMap.containsKey(className + "." + testName);
    }

    /**
     * Gets all collected test results.
     * @return list of all test results
     */
    public List<UnifiedTestResult> getResults() {
        return new ArrayList<>(results);
    }
}
