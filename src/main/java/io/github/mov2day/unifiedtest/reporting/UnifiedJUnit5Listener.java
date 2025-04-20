package io.github.mov2day.unifiedtest.reporting;

import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestIdentifier;
import org.junit.platform.launcher.TestPlan;
import org.junit.platform.engine.TestExecutionResult;
import java.util.concurrent.atomic.AtomicInteger;
import io.github.mov2day.unifiedtest.collector.UnifiedTestResultCollector;
import io.github.mov2day.unifiedtest.collector.UnifiedTestResult;
import io.github.mov2day.unifiedtest.reporting.ConsoleReporter;

public class UnifiedJUnit5Listener implements TestExecutionListener {
    private final UnifiedTestResultCollector collector;
    private final ConsoleReporter reporter;
    private final AtomicInteger passed = new AtomicInteger();
    private final AtomicInteger failed = new AtomicInteger();
    private final AtomicInteger skipped = new AtomicInteger();
    private final AtomicInteger total = new AtomicInteger();

    private static UnifiedTestResultCollector staticCollector;
    private static ConsoleReporter staticReporter;

    public static void setCollectorAndReporter(UnifiedTestResultCollector collector, ConsoleReporter reporter) {
        staticCollector = collector;
        staticReporter = reporter;
    }

    public UnifiedJUnit5Listener() {
        this(staticCollector, staticReporter);
    }

    public UnifiedJUnit5Listener(UnifiedTestResultCollector collector, ConsoleReporter reporter) {
        this.collector = collector;
        this.reporter = reporter;
    }

    @Override
    public void executionStarted(TestIdentifier testIdentifier) {
        if (testIdentifier.isTest()) {
            reporter.testRunning(testIdentifier.getDisplayName());
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
            collector.addResult(new UnifiedTestResult(testIdentifier.getSource().isPresent() ? testIdentifier.getSource().get().toString() : "", testIdentifier.getDisplayName(), status));
        }
    }

    @Override
    public void testPlanExecutionFinished(TestPlan testPlan) {
        reporter.summary(total.get(), passed.get(), failed.get(), skipped.get());
    }
}
