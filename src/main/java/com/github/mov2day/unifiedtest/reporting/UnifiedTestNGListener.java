package com.github.mov2day.unifiedtest.reporting;

import org.testng.ITestContext;
import org.testng.ITestListener;
import org.testng.ITestResult;
import java.util.concurrent.atomic.AtomicInteger;
import com.github.mov2day.unifiedtest.collector.UnifiedTestResultCollector;
import com.github.mov2day.unifiedtest.collector.UnifiedTestResult;
import com.github.mov2day.unifiedtest.reporting.ConsoleReporter;

public class UnifiedTestNGListener implements ITestListener {
    private final UnifiedTestResultCollector collector;
    private final AtomicInteger passed = new AtomicInteger();
    private final AtomicInteger failed = new AtomicInteger();
    private final AtomicInteger skipped = new AtomicInteger();
    private final AtomicInteger total = new AtomicInteger();
    private final ConsoleReporter reporter;

    public UnifiedTestNGListener(UnifiedTestResultCollector collector, ConsoleReporter reporter) {
        this.collector = collector;
        this.reporter = reporter;
    }
    @Override
    public void onTestStart(ITestResult result) {
        reporter.testRunning(result.getTestClass().getName() + "." + result.getName());
    }
    @Override
    public void onTestSuccess(ITestResult result) {
        reporter.testResult(result.getTestClass().getName() + "." + result.getName(), "PASS");
        passed.incrementAndGet();
        total.incrementAndGet();
        collector.addResult(new UnifiedTestResult(result.getTestClass().getName(), result.getName(), "PASS"));
    }
    @Override
    public void onTestFailure(ITestResult result) {
        reporter.testResult(result.getTestClass().getName() + "." + result.getName(), "FAIL");
        failed.incrementAndGet();
        total.incrementAndGet();
        collector.addResult(new UnifiedTestResult(result.getTestClass().getName(), result.getName(), "FAIL"));
    }
    @Override
    public void onTestSkipped(ITestResult result) {
        reporter.testResult(result.getTestClass().getName() + "." + result.getName(), "SKIP");
        skipped.incrementAndGet();
        total.incrementAndGet();
        collector.addResult(new UnifiedTestResult(result.getTestClass().getName(), result.getName(), "SKIP"));
    }
    @Override public void onTestFailedButWithinSuccessPercentage(ITestResult result) {}
    @Override public void onStart(ITestContext context) {}
    @Override public void onFinish(ITestContext context) {
        reporter.summary(total.get(), passed.get(), failed.get(), skipped.get());
    }
}
