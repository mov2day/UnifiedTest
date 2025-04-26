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

    /**
     * Creates a new test result with the specified details.
     * @param className the fully qualified name of the test class
     * @param testName the name of the test method
     * @param status the test execution status
     * @param failureMessage the failure message if test failed
     * @param stackTrace the stack trace if test failed
     */
    public UnifiedTestResult(String className, String testName, String status, String failureMessage, String stackTrace) {
        this.className = className;
        this.testName = testName;
        this.status = status;
        this.failureMessage = failureMessage;
        this.stackTrace = stackTrace;
    }

    /**
     * Creates a new test result without failure details.
     * @param className the fully qualified name of the test class
     * @param testName the name of the test method
     * @param status the test execution status
     */
    public UnifiedTestResult(String className, String testName, String status) {
        this(className, testName, status, null, null);
    }
}
