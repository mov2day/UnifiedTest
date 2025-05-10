package io.github.mov2day.unifiedtest.reporting;

import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestIdentifier;
import org.junit.platform.launcher.TestPlan;
import io.github.mov2day.unifiedtest.collector.UnifiedTestResultCollector;
import io.github.mov2day.unifiedtest.collector.UnifiedTestResult;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * JUnit 5 test listener that integrates with UnifiedTest reporting.
 * Captures JUnit5 test execution events and forwards them to the UnifiedTest collector and reporter.
 */
public class UnifiedJUnit5Listener implements TestExecutionListener {
    private static UnifiedTestResultCollector collector;
    private static ConsoleReporter reporter;
    private static final AtomicInteger passed = new AtomicInteger();
    private static final AtomicInteger failed = new AtomicInteger();
    private static final AtomicInteger skipped = new AtomicInteger();
    private static final AtomicInteger total = new AtomicInteger();
    private static final Map<TestIdentifier, Long> startTimes = new ConcurrentHashMap<>();

    /**
     * Default constructor required for ServiceLoader.
     */
    public UnifiedJUnit5Listener() {
        // Initialize default collector and reporter for Maven projects
        if (collector == null) {
            collector = new UnifiedTestResultCollector();
        }
        if (reporter == null) {
            reporter = new ConsoleReporter("standard");
        }
    }

    /**
     * Sets the collector and reporter for the listener.
     * This method should be called before test execution starts.
     * @param collector the test result collector
     * @param reporter the console reporter
     */
    public static void setCollectorAndReporter(UnifiedTestResultCollector collector, ConsoleReporter reporter) {
        UnifiedJUnit5Listener.collector = collector;
        UnifiedJUnit5Listener.reporter = reporter;
    }

    @Override
    public void testPlanExecutionStarted(TestPlan testPlan) {
        // Initialize counters
        passed.set(0);
        failed.set(0);
        skipped.set(0);
        total.set(0);
    }

    @Override
    public void executionStarted(TestIdentifier testIdentifier) {
        if (testIdentifier.isTest()) {
            String testName = getTestName(testIdentifier);
            if (reporter != null) {
                reporter.testRunning(testName);
            }
            startTimes.put(testIdentifier, System.currentTimeMillis());
        }
    }

    @Override
    public void executionSkipped(TestIdentifier testIdentifier, String reason) {
        if (testIdentifier.isTest()) {
            String testName = getTestName(testIdentifier);
            if (reporter != null) {
                reporter.testResult(testName, "SKIP");
            }
            skipped.incrementAndGet();
            total.incrementAndGet();
            if (collector != null) {
                collector.addResult(new UnifiedTestResult(
                    getClassName(testIdentifier),
                    getMethodName(testIdentifier),
                    "SKIP",
                    0
                ));
            }
        }
    }

    @Override
    public void executionFinished(TestIdentifier testIdentifier, TestExecutionResult testExecutionResult) {
        if (testIdentifier.isTest()) {
            String testName = getTestName(testIdentifier);
            String status;
            String message = null;
            String trace = null;
            long duration = getDurationAndRemove(testIdentifier);

            switch (testExecutionResult.getStatus()) {
                case SUCCESSFUL:
                    status = "PASS";
                    passed.incrementAndGet();
                    break;
                case FAILED:
                    status = "FAIL";
                    failed.incrementAndGet();
                    if (testExecutionResult.getThrowable().isPresent()) {
                        Throwable throwable = testExecutionResult.getThrowable().get();
                        message = throwable.getMessage();
                        StringWriter sw = new StringWriter();
                        throwable.printStackTrace(new PrintWriter(sw));
                        trace = sw.toString();
                    }
                    break;
                case ABORTED:
                    status = "SKIP";
                    skipped.incrementAndGet();
                    break;
                default:
                    status = testExecutionResult.getStatus().toString();
            }
            total.incrementAndGet();
            if (reporter != null) {
                reporter.testResult(testName, status);
            }

            if (collector != null) {
                collector.addResult(new UnifiedTestResult(
                    getClassName(testIdentifier),
                    getMethodName(testIdentifier),
                    status,
                    message,
                    trace,
                    duration
                ));
            }
        }
    }

    @Override
    public void testPlanExecutionFinished(TestPlan testPlan) {
        if (reporter != null) {
            reporter.summary(total.get(), passed.get(), failed.get(), skipped.get());
        }
    }

    private String getTestName(TestIdentifier testIdentifier) {
        return getClassName(testIdentifier) + "." + getMethodName(testIdentifier);
    }

    private String getClassName(TestIdentifier testIdentifier) {
        String uniqueId = testIdentifier.getUniqueId();
        // Extract class name from uniqueId which is in format: [engine:junit-jupiter]/[class:ClassName]/[method:methodName]
        int classStart = uniqueId.indexOf("[class:") + 7;
        int classEnd = uniqueId.indexOf("]", classStart);
        return uniqueId.substring(classStart, classEnd);
    }

    private String getMethodName(TestIdentifier testIdentifier) {
        String uniqueId = testIdentifier.getUniqueId();
        // Extract method name from uniqueId which is in format: [engine:junit-jupiter]/[class:ClassName]/[method:methodName]
        int methodStart = uniqueId.indexOf("[method:") + 8;
        int methodEnd = uniqueId.indexOf("]", methodStart);
        return uniqueId.substring(methodStart, methodEnd);
    }

    private long getDurationAndRemove(TestIdentifier testIdentifier) {
        Long startTime = startTimes.remove(testIdentifier);
        return startTime != null ? System.currentTimeMillis() - startTime : 0;
    }
}
