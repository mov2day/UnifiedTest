package io.github.mov2day.unifiedtest.reporting;

import org.junit.runner.Description;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import io.github.mov2day.unifiedtest.collector.UnifiedTestResultCollector;
import io.github.mov2day.unifiedtest.collector.UnifiedTestResult;

/**
 * JUnit 4 test listener that integrates with UnifiedTest reporting.
 * Captures JUnit4 test execution events and forwards them to the UnifiedTest collector and reporter.
 */
public class UnifiedJUnit4Listener extends RunListener {
    private final UnifiedTestResultCollector collector;
    private final AtomicInteger passed = new AtomicInteger();
    private final AtomicInteger failed = new AtomicInteger();
    private final AtomicInteger skipped = new AtomicInteger();
    private final AtomicInteger total = new AtomicInteger();
    private final ConsoleReporter reporter;
    private final Map<Description, Long> startTimes = new ConcurrentHashMap<>();

    /**
     * Creates a new listener with the specified collector and reporter.
     */
    public UnifiedJUnit4Listener(UnifiedTestResultCollector collector, ConsoleReporter reporter) {
        this.collector = collector;
        this.reporter = reporter;
    }

    @Override
    public void testStarted(Description description) {
        String testName = description.getClassName() + "." + description.getMethodName();
        reporter.testRunning(testName);
        startTimes.put(description, System.currentTimeMillis());
    }

    @Override
    public void testFailure(Failure failure) {
        String testClassName = failure.getDescription().getClassName();
        String testMethodName = failure.getDescription().getMethodName();
        String testName = testClassName + "." + testMethodName;
        reporter.testResult(testName, "FAIL");
        failed.incrementAndGet();
        total.incrementAndGet();

        long duration = getDurationAndRemove(failure.getDescription());
        String message = failure.getMessage();
        String trace = null;
        if (failure.getException() != null) {
            StringWriter sw = new StringWriter();
            failure.getException().printStackTrace(new PrintWriter(sw));
            trace = sw.toString();
        }
        collector.addResult(new UnifiedTestResult(
            testClassName,
            testMethodName,
            "FAIL",
            message,
            trace,
            duration
        ));
    }

    @Override
    public void testIgnored(Description description) {
        String testName = description.getClassName() + "." + description.getMethodName();
        reporter.testResult(testName, "SKIP");
        skipped.incrementAndGet();
        total.incrementAndGet();
        collector.addResult(new UnifiedTestResult(
            description.getClassName(),
            description.getMethodName(),
            "SKIP",
            0
        ));
    }

    @Override
    public void testAssumptionFailure(Failure failure) {
        testIgnored(failure.getDescription());
    }

    @Override
    public void testFinished(Description description) {
        // If not already recorded as FAIL or SKIP, mark as PASS
        if (!collector.hasResult(description.getClassName(), description.getMethodName())) {
            String testName = description.getClassName() + "." + description.getMethodName();
            reporter.testResult(testName, "PASS");
            passed.incrementAndGet();
            total.incrementAndGet();
            long duration = getDurationAndRemove(description);
            collector.addResult(new UnifiedTestResult(
                description.getClassName(),
                description.getMethodName(),
                "PASS",
                duration
            ));
        }
    }

    @Override
    public void testRunFinished(Result result) {
        passed.set(result.getRunCount() - failed.get() - skipped.get());
        reporter.summary(result.getRunCount(), passed.get(), failed.get(), skipped.get());
    }

    private long getDurationAndRemove(Description description) {
        Long startTime = startTimes.remove(description);
        return startTime != null ? System.currentTimeMillis() - startTime : 0;
    }
}
