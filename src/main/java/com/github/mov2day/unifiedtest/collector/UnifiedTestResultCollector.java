package com.github.mov2day.unifiedtest.collector;

import org.gradle.api.tasks.testing.TestDescriptor;
import org.gradle.api.tasks.testing.TestListener;
import org.gradle.api.tasks.testing.TestResult;
import java.util.List;
import java.util.ArrayList;

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
    public void addResult(UnifiedTestResult result) {
        results.add(result);
    }
    public boolean hasResult(String className, String testName) {
        return results.stream().anyMatch(r -> r.className.equals(className) && r.testName.equals(testName));
    }
    public List<UnifiedTestResult> getResults() { return results; }
}
