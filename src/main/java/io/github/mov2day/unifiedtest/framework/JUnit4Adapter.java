package io.github.mov2day.unifiedtest.framework;

import org.gradle.api.Project;
import org.gradle.api.tasks.testing.Test;
import io.github.mov2day.unifiedtest.collector.UnifiedTestResultCollector;
import io.github.mov2day.unifiedtest.reporting.ConsoleReporter;
import io.github.mov2day.unifiedtest.reporting.UnifiedJUnit4Listener;
import io.github.mov2day.unifiedtest.reporting.PrettyConsoleTestListener;
import io.github.mov2day.unifiedtest.UnifiedTestAgentPlugin;

/**
 * JUnit 4 framework adapter for UnifiedTest.
 * Handles registration of test listeners and configuration for JUnit 4 test execution.
 */
public class JUnit4Adapter implements TestFrameworkAdapter {
    @Override
    public boolean isApplicable(Project project) {
        return FrameworkDetector.detect(project).contains("JUnit4");
    }
    
    @Override
    public void registerListeners(Project project, Test testTask, UnifiedTestResultCollector collector, ConsoleReporter reporter) {
        testTask.useJUnit();
        // Set the collector and reporter for the JUnit 4 listener
        UnifiedJUnit4Listener.setCollectorAndReporter(collector, reporter);
        // Register the JUnit 4 listener using system property
        System.setProperty("junit.listeners", UnifiedJUnit4Listener.class.getName());
        // Add the pretty console listener
        testTask.addTestListener(new PrettyConsoleTestListener(project, getThemeFromConfig(project), collector));
    }
    
    @Override
    public String getName() { return "JUnit4"; }

    private String getThemeFromConfig(Project project) {
        try {
            return project.getExtensions().getByType(UnifiedTestAgentPlugin.UnifiedTestExtensionConfig.class).getTheme().get();
        } catch (Exception e) {
            return "standard"; // Default fallback
        }
    }
}
