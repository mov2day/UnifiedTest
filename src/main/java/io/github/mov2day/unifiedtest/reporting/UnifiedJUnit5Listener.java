package io.github.mov2day.unifiedtest.reporting;

import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestIdentifier;
import org.junit.platform.launcher.TestPlan;
import org.junit.platform.engine.TestExecutionResult;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import io.github.mov2day.unifiedtest.collector.UnifiedTestResultCollector;
import io.github.mov2day.unifiedtest.collector.UnifiedTestResult;
import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * JUnit 5 test execution listener that integrates with UnifiedTest reporting.
 * Captures test execution events and forwards them to the UnifiedTest collector and reporter.
 */
public class UnifiedJUnit5Listener implements TestExecutionListener {
    private final UnifiedTestResultCollector collector;
    private final ConsoleReporter reporter;
    private final AtomicInteger passed = new AtomicInteger();
    private final AtomicInteger failed = new AtomicInteger();
    private final AtomicInteger skipped = new AtomicInteger();
    private final AtomicInteger total = new AtomicInteger();
    private final Map<TestIdentifier, Long> startTimes = new ConcurrentHashMap<>();

    private static UnifiedTestResultCollector staticCollector;
    private static ConsoleReporter staticReporter;

    /**
     * Sets the test result collector and console reporter for this listener.
     * Must be called before test execution starts.
     * @param collector the test result collector
     * @param reporter the console reporter
     */
    public static void setCollectorAndReporter(UnifiedTestResultCollector collector, ConsoleReporter reporter) {
        staticCollector = collector;
        staticReporter = reporter;
    }

    /**
     * Creates a new listener with default configuration.
     */
    public UnifiedJUnit5Listener() {
        this(staticCollector, staticReporter);
    }

    /**
     * Creates a new listener with the specified collector and reporter.
     * @param collector the test result collector
     * @param reporter the console reporter
     */
    public UnifiedJUnit5Listener(UnifiedTestResultCollector collector, ConsoleReporter reporter) {
        this.collector = collector;
        this.reporter = reporter;
    }

    @Override
    public void executionStarted(TestIdentifier testIdentifier) {
        if (testIdentifier.isTest()) {
            reporter.testRunning(testIdentifier.getDisplayName());
            startTimes.put(testIdentifier, System.currentTimeMillis());
        }
    }

    @Override
    public void executionFinished(TestIdentifier testIdentifier, TestExecutionResult testExecutionResult) {
        if (testIdentifier.isTest()) {
            String status;
            switch (testExecutionResult.getStatus()) {
                case SUCCESSFUL:
                    status = "PASS";
                    passed.incrementAndGet();
                    break;
                case FAILED:
                    status = "FAIL";
                    failed.incrementAndGet();
                    break;
                case ABORTED:
                    status = "SKIP";
                    skipped.incrementAndGet();
                    break;
                default:
                    status = testExecutionResult.getStatus().toString();
            }
            total.incrementAndGet();
            reporter.testResult(testIdentifier.getDisplayName(), status);

            long duration = getDurationAndRemove(testIdentifier);
            String message = null;
            String trace = null;
            if (testExecutionResult.getThrowable().isPresent()) {
                Throwable throwable = testExecutionResult.getThrowable().get();
                message = throwable.getMessage();
                StringWriter sw = new StringWriter();
                throwable.printStackTrace(new PrintWriter(sw));
                trace = sw.toString();
            }

            collector.addResult(new UnifiedTestResult(
                testIdentifier.getSource().isPresent() ? testIdentifier.getSource().get().toString() : "",
                testIdentifier.getDisplayName(),
                status,
                message,
                trace,
                duration
            ));
        }
    }

    @Override
    public void testPlanExecutionFinished(TestPlan testPlan) {
        reporter.summary(total.get(), passed.get(), failed.get(), skipped.get());
    }

    private long getDurationAndRemove(TestIdentifier testIdentifier) {
        Long startTime = startTimes.remove(testIdentifier);
        return startTime != null ? System.currentTimeMillis() - startTime : 0;
    }
}
