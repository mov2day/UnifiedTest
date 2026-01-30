package io.github.mov2day.unifiedtest.framework;

import org.gradle.api.Project;
import org.gradle.api.tasks.testing.Test;
import io.github.mov2day.unifiedtest.collector.UnifiedTestResultCollector;
import io.github.mov2day.unifiedtest.reporting.ConsoleReporter;
import io.github.mov2day.unifiedtest.reporting.UnifiedTestNGListener;
import io.github.mov2day.unifiedtest.reporting.PrettyConsoleTestListener;
import io.github.mov2day.unifiedtest.UnifiedTestAgentPlugin;

/**
 * TestNG framework adapter for UnifiedTest.
 * Handles registration of test listeners and configuration for TestNG test execution.
 */
public class TestNGAdapter implements TestFrameworkAdapter {
    @Override
    public boolean isApplicable(Project project) {
        return FrameworkDetector.detect(project).contains("TestNG");
    }
    
    @Override
    public void registerListeners(Project project, Test testTask, UnifiedTestResultCollector collector, ConsoleReporter reporter, String theme) {
        testTask.useTestNG();
        // Set the collector and reporter for the TestNG listener
        UnifiedTestNGListener.setCollectorAndReporter(collector, reporter);
        // Add the pretty console listener using the configuration-time theme
        testTask.addTestListener(new PrettyConsoleTestListener(project, theme, collector));
    }
    
    @Override
    public String getName() { return "TestNG"; }

    // theme is now passed in at configuration time; no runtime extension access
}
