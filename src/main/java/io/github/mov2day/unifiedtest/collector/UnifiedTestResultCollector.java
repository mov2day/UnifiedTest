package io.github.mov2day.unifiedtest.collector;

import org.gradle.api.tasks.testing.TestDescriptor;
import org.gradle.api.tasks.testing.TestListener;
import org.gradle.api.tasks.testing.TestResult;
import io.github.mov2day.unifiedtest.reporting.UnifiedTestResult;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.function.Consumer;
import java.time.Instant;

/**
 * Collects and stores test results from various test frameworks.
 * Implements TestListener to receive test execution events.
 */
public class UnifiedTestResultCollector implements TestListener, ITestResultCollector {
    private final List<io.github.mov2day.unifiedtest.collector.UnifiedTestResult> results = new ArrayList<>();
    private final Map<String, io.github.mov2day.unifiedtest.collector.UnifiedTestResult> resultMap = new ConcurrentHashMap<>();
    private Consumer<UnifiedTestResult> resultCallback;

    /**
     * Default constructor required for ServiceLoader.
     */
    public UnifiedTestResultCollector() {
        // Initialize with default settings
    }

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

        io.github.mov2day.unifiedtest.collector.UnifiedTestResult testResult = new io.github.mov2day.unifiedtest.collector.UnifiedTestResult(
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

        // Notify callback if set
        if (resultCallback != null) {
            resultCallback.accept(convertToReportingResult(testResult));
        }
    }

    /**
     * Sets a callback to be notified when new test results are collected.
     * @param callback the callback to be notified
     */
    public void setResultCallback(Consumer<UnifiedTestResult> callback) {
        this.resultCallback = callback;
    }

    /**
     * Adds a test result to the collection.
     * @param result the test result to add
     */
    public void addResult(io.github.mov2day.unifiedtest.collector.UnifiedTestResult result) {
        String key = result.className + "." + result.testName;
        if (!resultMap.containsKey(key)) {
            resultMap.put(key, result);
            results.add(result);
            
            // Notify callback if set
            if (resultCallback != null) {
                resultCallback.accept(convertToReportingResult(result));
            }
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
    public List<io.github.mov2day.unifiedtest.collector.UnifiedTestResult> getResults() {
        return new ArrayList<>(results);
    }

    /**
     * Converts a collector test result to a reporting test result.
     * @param result the collector test result
     * @return the reporting test result
     */
    private UnifiedTestResult convertToReportingResult(io.github.mov2day.unifiedtest.collector.UnifiedTestResult result) {
        String name = result.className + "." + result.testName;
        Instant startTime = Instant.ofEpochMilli(System.currentTimeMillis() - result.duration);
        Instant endTime = Instant.ofEpochMilli(System.currentTimeMillis());
        
        if (result.failureMessage != null || result.stackTrace != null) {
            return new UnifiedTestResult(
                name, 
                result.status, 
                startTime, 
                endTime, 
                result.failureMessage, 
                result.stackTrace
            );
        } else {
            return new UnifiedTestResult(
                name, 
                result.status, 
                startTime, 
                endTime
            );
        }
    }
}
