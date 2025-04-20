package io.github.mov2day.unifiedtest.collector;

public class UnifiedTestResult {
    public final String className;
    public final String testName;
    public final String status;
    public UnifiedTestResult(String className, String testName, String status) {
        this.className = className;
        this.testName = testName;
        this.status = status;
    }
}
