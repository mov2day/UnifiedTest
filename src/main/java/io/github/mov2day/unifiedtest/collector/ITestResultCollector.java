package io.github.mov2day.unifiedtest.collector;

import java.util.List;

/**
 * Core interface for test result collection without Gradle dependencies.
 * This interface allows for different implementations for Gradle and Maven environments.
 */
public interface ITestResultCollector {
    /**
     * Adds a test result to the collection.
     * @param result the test result to add
     */
    void addResult(UnifiedTestResult result);
    
    /**
     * Gets all collected test results.
     * @return list of all test results
     */
    List<UnifiedTestResult> getResults();
    
    /**
     * Checks if a result exists for the specified test.
     * @param className the test class name
     * @param testName the test method name
     * @return true if a result exists for this test
     */
    boolean hasResult(String className, String testName);
} 