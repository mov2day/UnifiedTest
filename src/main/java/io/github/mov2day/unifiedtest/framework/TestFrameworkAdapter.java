package io.github.mov2day.unifiedtest.framework;

import org.gradle.api.Project;
import org.gradle.api.tasks.testing.Test;
import io.github.mov2day.unifiedtest.collector.UnifiedTestResultCollector;
import io.github.mov2day.unifiedtest.reporting.ConsoleReporter;

/**
 * Interface for adapting different test frameworks to work with UnifiedTest.
 * Implementations should handle framework-specific test listener registration and configuration.
 */
public interface TestFrameworkAdapter {
    /**
     * Checks if this adapter is applicable for the given project.
     * @param project the Gradle project to check
     * @return true if this adapter can be used with the project
     */
    boolean isApplicable(Project project);

    /**
     * Registers test listeners for the framework with the given project and test task.
     * @param project the Gradle project
        * @param testTask the test task to configure
     * @param collector collector for test results
        * @param reporter reporter for console output
        * @param theme the resolved theme (captured at configuration time)
     */
        void registerListeners(Project project, Test testTask, UnifiedTestResultCollector collector, ConsoleReporter reporter, String theme);

    /**
     * Gets the name of the test framework this adapter supports.
     * @return the name of the test framework
     */
    String getName();
}
