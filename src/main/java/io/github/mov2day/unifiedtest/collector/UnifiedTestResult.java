package io.github.mov2day.unifiedtest.collector;

/**
 * Represents the result of a single test execution.
 * Contains the test class name, test method name, and execution status.
 */
public class UnifiedTestResult {
    /** The fully qualified name of the test class */
    public final String className;
    /** The name of the test method */
    public final String testName;
    /** The test execution status (PASS, FAIL, SKIP) */
    public final String status;
    /** The failure message if the test failed, null otherwise */
    public final String failureMessage;
    /** The stack trace if the test failed, null otherwise */
    public final String stackTrace;
    /** The test execution duration in milliseconds */
    public final long duration;

    /**
     * Creates a new test result with the specified details.
     */
    public UnifiedTestResult(String className, String testName, String status, String failureMessage, String stackTrace, long duration) {
        this.className = className;
        this.testName = testName;
        this.status = status;
        this.failureMessage = failureMessage;
        this.stackTrace = stackTrace;
        this.duration = duration;
    }

    /**
     * Creates a new test result with failure details but no duration.
     */
    public UnifiedTestResult(String className, String testName, String status, String failureMessage, String stackTrace) {
        this(className, testName, status, failureMessage, stackTrace, 0);
    }

    /**
     * Creates a new test result without failure details or duration.
     */
    public UnifiedTestResult(String className, String testName, String status) {
        this(className, testName, status, null, null, 0);
    }

    /**
     * Creates a new test result with duration but no failure details.
     */
    public UnifiedTestResult(String className, String testName, String status, long duration) {
        this(className, testName, status, null, null, duration);
    }
}
