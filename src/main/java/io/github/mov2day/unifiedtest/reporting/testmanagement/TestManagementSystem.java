package io.github.mov2day.unifiedtest.reporting.testmanagement;

import io.github.mov2day.unifiedtest.reporting.UnifiedTestResult;
import java.util.List;
import java.util.Map;

/**
 * Interface for test management system integrations.
 * Implementations should handle the specific requirements of each test management system.
 */
public interface TestManagementSystem {
    /**
     * Initialize the test management system with configuration.
     * @param config Configuration object specific to the test management system
     */
    void initialize(Object config);

    /**
     * Queue a test result for later batch processing.
     * @param result Test result to queue
     */
    void queueTestResult(UnifiedTestResult result);

    /**
     * Push test results to the test management system.
     * If results parameter is null, only queued results will be pushed.
     * @param results List of test results to push (can be null)
     * @return Map of test names to their corresponding IDs in the test management system
     */
    Map<String, String> pushResults(List<UnifiedTestResult> results);

    /**
     * Flush any queued results to the test management system.
     */
    void flushResults();

    /**
     * Get the name of the test management system.
     * @return System name (e.g., "zephyr", "testrail")
     */
    String getName();

    /**
     * Check if the test management system is properly configured.
     * @return true if the system is configured and ready to use
     */
    boolean isConfigured();

    /**
     * Get the status of the last operation.
     * @return Status object containing success/failure information and any error messages
     */
    OperationStatus getLastStatus();
} 