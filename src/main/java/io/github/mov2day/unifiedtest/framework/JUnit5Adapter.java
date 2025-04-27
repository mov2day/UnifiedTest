package io.github.mov2day.unifiedtest.framework;

import org.gradle.api.Project;
import org.gradle.api.tasks.testing.Test;
import io.github.mov2day.unifiedtest.collector.UnifiedTestResultCollector;
import io.github.mov2day.unifiedtest.reporting.ConsoleReporter;
import io.github.mov2day.unifiedtest.reporting.UnifiedJUnit5Listener;
import io.github.mov2day.unifiedtest.reporting.PrettyConsoleTestListener;
import io.github.mov2day.unifiedtest.UnifiedTestAgentPlugin;

/**
 * JUnit 5 (Jupiter) framework adapter for UnifiedTest.
 * Handles registration of test listeners and configuration for JUnit 5 test execution.
 */
public class JUnit5Adapter implements TestFrameworkAdapter {
    @Override
    public boolean isApplicable(Project project) {
        return FrameworkDetector.detect(project).contains("JUnit5");
    }

    @Override
    public void registerListeners(Project project, Test testTask, UnifiedTestResultCollector collector, ConsoleReporter reporter) {
        testTask.useJUnitPlatform();
        // Register the JUnit 5 listener with the collector and reporter
        testTask.addTestListener(new UnifiedJUnit5Listener(collector, reporter));
        // Add the pretty console listener
        testTask.addTestListener(new PrettyConsoleTestListener(project, getThemeFromConfig(project)));
    }

    @Override
    public String getName() { return "JUnit5"; }
    
    private String getThemeFromConfig(Project project) {
        try {
            return project.getExtensions().getByType(UnifiedTestAgentPlugin.UnifiedTestExtensionConfig.class).getTheme().get();
        } catch (Exception e) {
            return "standard"; // Default fallback
        }
    }
}
