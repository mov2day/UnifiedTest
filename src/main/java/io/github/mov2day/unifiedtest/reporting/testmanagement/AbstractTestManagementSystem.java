package io.github.mov2day.unifiedtest.reporting.testmanagement;

import io.github.mov2day.unifiedtest.reporting.UnifiedTestResult;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;
import java.util.ArrayList;

/**
 * Abstract base implementation of TestManagementSystem.
 * Provides common functionality and helper methods for test management system implementations.
 */
public abstract class AbstractTestManagementSystem implements TestManagementSystem {
    protected Object config;
    protected OperationStatus lastStatus;
    protected boolean configured;
    protected static final int MAX_RETRIES = 3;
    protected static final long RETRY_DELAY_MS = 1000;
    protected final List<UnifiedTestResult> pendingResults;

    protected AbstractTestManagementSystem() {
        this.pendingResults = new ArrayList<>();
    }

    @Override
    public void initialize(Object config) {
        this.config = config;
        this.configured = validateConfig();
        if (!configured) {
            lastStatus = OperationStatus.failure("Invalid configuration");
        }
    }

    @Override
    public boolean isConfigured() {
        return configured;
    }

    @Override
    public OperationStatus getLastStatus() {
        return lastStatus;
    }

    @Override
    public void queueTestResult(UnifiedTestResult result) {
        if (result != null) {
            pendingResults.add(result);
        }
    }

    @Override
    public Map<String, String> pushResults(List<UnifiedTestResult> results) {
        if (!isConfigured()) {
            lastStatus = OperationStatus.failure("System not configured");
            return new HashMap<>();
        }

        // Combine queued results with provided results
        List<UnifiedTestResult> allResults = new ArrayList<>(pendingResults);
        if (results != null) {
            allResults.addAll(results);
        }
        pendingResults.clear();

        if (allResults.isEmpty()) {
            lastStatus = OperationStatus.success("No results to push");
            return new HashMap<>();
        }

        Map<String, String> testIds = new HashMap<>();
        int retryCount = 0;
        boolean success = false;

        while (!success && retryCount < MAX_RETRIES) {
            try {
                testIds = doPushResults(allResults);
                success = true;
                lastStatus = OperationStatus.success("Successfully pushed " + allResults.size() + " test results");
            } catch (Exception e) {
                retryCount++;
                if (retryCount < MAX_RETRIES) {
                    try {
                        TimeUnit.MILLISECONDS.sleep(RETRY_DELAY_MS * retryCount);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                } else {
                    lastStatus = OperationStatus.failure("Failed to push test results after " + MAX_RETRIES + " attempts", e);
                }
            }
        }

        return testIds;
    }

    @Override
    public void flushResults() {
        if (!pendingResults.isEmpty()) {
            pushResults(null);
        }
    }

    /**
     * Validate the configuration for this test management system.
     * @return true if the configuration is valid
     */
    protected abstract boolean validateConfig();

    /**
     * Implementation-specific method to push test results.
     * @param results List of test results to push
     * @return Map of test names to their corresponding IDs in the test management system
     * @throws Exception if the operation fails
     */
    protected abstract Map<String, String> doPushResults(List<UnifiedTestResult> results) throws Exception;

    /**
     * Convert UnifiedTestResult status to test management system specific status.
     * @param status UnifiedTestResult status
     * @return Test management system specific status
     */
    protected abstract String convertStatus(String status);

    /**
     * Convert test management system specific status to UnifiedTestResult status.
     * @param status Test management system specific status
     * @return UnifiedTestResult status
     */
    protected abstract String convertFromStatus(String status);
} 