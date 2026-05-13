package io.github.mov2day.unifiedtest.collector;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

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
    /** The framework that produced this result */
    public final String framework;
    private final Map<String, String> metadata = new LinkedHashMap<>();

    /**
     * Creates a new test result with the specified details.
     */
    public UnifiedTestResult(String className, String testName, String status, String failureMessage, String stackTrace, long duration) {
        this(className, testName, status, failureMessage, stackTrace, duration, "unknown");
    }

    /**
     * Creates a new test result with the specified details and framework.
     */
    public UnifiedTestResult(String className, String testName, String status, String failureMessage, String stackTrace, long duration, String framework) {
        this.className = className;
        this.testName = testName;
        this.status = status;
        this.failureMessage = failureMessage;
        this.stackTrace = stackTrace;
        this.duration = duration;
        this.framework = framework == null || framework.isBlank() ? "unknown" : framework;
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

    /**
     * Adds metadata generated after collection, such as trace IDs.
     */
    public void addMetadata(String key, String value) {
        if (key != null && value != null && !key.isBlank() && !value.isBlank()) {
            metadata.put(key, value);
        }
    }

    /**
     * Gets immutable metadata for report rendering.
     */
    public Map<String, String> getMetadata() {
        return Collections.unmodifiableMap(metadata);
    }

    /**
     * Returns this result if it already has a framework, otherwise returns a copy with the supplied framework.
     */
    public UnifiedTestResult withFramework(String frameworkName) {
        if (!"unknown".equals(framework)) {
            return this;
        }
        UnifiedTestResult copy = new UnifiedTestResult(className, testName, status, failureMessage, stackTrace, duration, frameworkName);
        metadata.forEach(copy::addMetadata);
        return copy;
    }
}
