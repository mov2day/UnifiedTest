package io.github.mov2day.unifiedtest.reporting;

import org.junit.runner.Description;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;

import java.util.concurrent.atomic.AtomicInteger;

import io.github.mov2day.unifiedtest.collector.UnifiedTestResultCollector;
import io.github.mov2day.unifiedtest.collector.UnifiedTestResult;
import io.github.mov2day.unifiedtest.reporting.ConsoleReporter;

public class UnifiedJUnit4Listener extends RunListener {
    private final UnifiedTestResultCollector collector;
    private final AtomicInteger passed = new AtomicInteger();
    private final AtomicInteger failed = new AtomicInteger();
    private final AtomicInteger skipped = new AtomicInteger();
    private final AtomicInteger total = new AtomicInteger();
    private final ConsoleReporter reporter;

    public UnifiedJUnit4Listener(UnifiedTestResultCollector collector, ConsoleReporter reporter) {
        this.collector = collector;
        this.reporter = reporter;
    }

    @Override
    public void testStarted(Description description) {
        reporter.testRunning(description.getClassName() + "." + description.getMethodName());
    }

    @Override
    public void testFailure(Failure failure) {
        reporter.testResult(failure.getDescription().getClassName() + "." + failure.getDescription().getMethodName(), "FAIL");
        failed.incrementAndGet();
        total.incrementAndGet();
        collector.addResult(new UnifiedTestResult(failure.getDescription().getClassName(), failure.getDescription().getMethodName(), "FAIL"));
    }

    @Override
    public void testIgnored(Description description) {
        reporter.testResult(description.getClassName() + "." + description.getMethodName(), "SKIP");
        skipped.incrementAndGet();
        total.incrementAndGet();
        collector.addResult(new UnifiedTestResult(description.getClassName(), description.getMethodName(), "SKIP"));
    }

    @Override
    public void testAssumptionFailure(Failure failure) {
        testIgnored(failure.getDescription());
    }

    @Override
    public void testFinished(Description description) {
        // If not already recorded as FAIL or SKIP, mark as PASS
        if (!collector.hasResult(description.getClassName(), description.getMethodName())) {
            passed.incrementAndGet();
            total.incrementAndGet();
            collector.addResult(new UnifiedTestResult(description.getClassName(), description.getMethodName(), "PASS"));
        }
    }

    @Override
    public void testRunFinished(Result result) {
        passed.set(result.getRunCount() - failed.get() - skipped.get());
        reporter.summary(result.getRunCount(), passed.get(), failed.get(), skipped.get());
    }
}
