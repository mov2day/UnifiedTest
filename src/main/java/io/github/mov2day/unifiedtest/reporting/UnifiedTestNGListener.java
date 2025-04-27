package io.github.mov2day.unifiedtest.reporting;

import org.testng.ITestContext;
import org.testng.ITestListener;
import org.testng.ITestResult;
import java.util.concurrent.atomic.AtomicInteger;
import io.github.mov2day.unifiedtest.collector.UnifiedTestResultCollector;
import io.github.mov2day.unifiedtest.collector.UnifiedTestResult;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * TestNG test listener that integrates with UnifiedTest reporting.
 * Captures TestNG test execution events and forwards them to the UnifiedTest collector and reporter.
 */
public class UnifiedTestNGListener implements ITestListener {
    private final UnifiedTestResultCollector collector;
    private final AtomicInteger passed = new AtomicInteger();
    private final AtomicInteger failed = new AtomicInteger();
    private final AtomicInteger skipped = new AtomicInteger();
    private final AtomicInteger total = new AtomicInteger();
    private final ConsoleReporter reporter;
    private final Map<ITestResult, Long> startTimes = new ConcurrentHashMap<>();

    /**
     * Creates a new listener with the specified collector and reporter.
     * @param collector the test result collector
     * @param reporter the console reporter
     */
    public UnifiedTestNGListener(UnifiedTestResultCollector collector, ConsoleReporter reporter) {
        this.collector = collector;
        this.reporter = reporter;
    }

    @Override
    public void onTestStart(ITestResult result) {
        String testName = result.getTestClass().getName() + "." + result.getMethod().getMethodName();
        reporter.testRunning(testName);
        startTimes.put(result, System.currentTimeMillis());
    }

    @Override
    public void onTestSuccess(ITestResult result) {
        String testName = result.getTestClass().getName() + "." + result.getMethod().getMethodName();
        reporter.testResult(testName, "PASS");
        passed.incrementAndGet();
        total.incrementAndGet();
        long duration = getDurationAndRemove(result);
        collector.addResult(new UnifiedTestResult(
            result.getTestClass().getName(),
            result.getMethod().getMethodName(),
            "PASS",
            duration
        ));
    }

    @Override
    public void onTestFailure(ITestResult result) {
        String testName = result.getTestClass().getName() + "." + result.getMethod().getMethodName();
        reporter.testResult(testName, "FAIL");
        failed.incrementAndGet();
        total.incrementAndGet();

        long duration = getDurationAndRemove(result);
        String message = null;
        String trace = null;
        if (result.getThrowable() != null) {
            message = result.getThrowable().getMessage();
            StringWriter sw = new StringWriter();
            result.getThrowable().printStackTrace(new PrintWriter(sw));
            trace = sw.toString();
        }

        collector.addResult(new UnifiedTestResult(
            result.getTestClass().getName(),
            result.getMethod().getMethodName(),
            "FAIL",
            message,
            trace,
            duration
        ));
    }

    @Override
    public void onTestSkipped(ITestResult result) {
        String testName = result.getTestClass().getName() + "." + result.getMethod().getMethodName();
        reporter.testResult(testName, "SKIP");
        skipped.incrementAndGet();
        total.incrementAndGet();
        long duration = getDurationAndRemove(result);
        collector.addResult(new UnifiedTestResult(
            result.getTestClass().getName(),
            result.getMethod().getMethodName(),
            "SKIP",
            duration
        ));
    }

    @Override public void onTestFailedButWithinSuccessPercentage(ITestResult result) {
        onTestFailure(result);
    }
    @Override public void onStart(ITestContext context) {}
    @Override public void onFinish(ITestContext context) {
        reporter.summary(total.get(), passed.get(), failed.get(), skipped.get());
    }

    private long getDurationAndRemove(ITestResult result) {
        Long startTime = startTimes.remove(result);
        return startTime != null ? System.currentTimeMillis() - startTime : 0;
    }
}
