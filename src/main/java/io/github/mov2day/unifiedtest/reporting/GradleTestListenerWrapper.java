package io.github.mov2day.unifiedtest.reporting;

import org.gradle.api.tasks.testing.TestDescriptor;
import org.gradle.api.tasks.testing.TestListener;
import org.gradle.api.tasks.testing.TestResult;
import io.github.mov2day.unifiedtest.collector.UnifiedTestResultCollector;

/**
 * Wrapper class that implements Gradle's TestListener interface and delegates to framework-specific listeners.
 * This allows us to integrate our framework-specific listeners with Gradle's test execution.
 */
public class GradleTestListenerWrapper implements TestListener {
    private final UnifiedTestResultCollector collector;
    private final ConsoleReporter reporter;

    /**
     * Creates a new wrapper with the specified collector and reporter.
     * @param collector the test result collector
     * @param reporter the console reporter
     */
    public GradleTestListenerWrapper(UnifiedTestResultCollector collector, ConsoleReporter reporter) {
        this.collector = collector;
        this.reporter = reporter;
    }

    @Override
    public void beforeSuite(TestDescriptor suite) {
        // No-op - handled by framework-specific listeners
    }

    @Override
    public void afterSuite(TestDescriptor suite, TestResult result) {
        // No-op - handled by framework-specific listeners
    }

    @Override
    public void beforeTest(TestDescriptor testDescriptor) {
        // No-op - handled by framework-specific listeners
    }

    @Override
    public void afterTest(TestDescriptor testDescriptor, TestResult result) {
        // No-op - handled by framework-specific listeners
    }

    /**
     * Gets the test result collector.
     * @return the test result collector
     */
    public UnifiedTestResultCollector getCollector() {
        return collector;
    }

    /**
     * Gets the console reporter.
     * @return the console reporter
     */
    public ConsoleReporter getReporter() {
        return reporter;
    }
} 