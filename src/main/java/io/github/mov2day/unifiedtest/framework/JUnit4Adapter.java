package io.github.mov2day.unifiedtest.framework;

import org.gradle.api.Project;
import org.gradle.api.tasks.testing.Test;
import io.github.mov2day.unifiedtest.collector.UnifiedTestResultCollector;
import io.github.mov2day.unifiedtest.reporting.ConsoleReporter;
import io.github.mov2day.unifiedtest.reporting.UnifiedJUnit4Listener;
import io.github.mov2day.unifiedtest.reporting.PrettyConsoleTestListener;
import io.github.mov2day.unifiedtest.UnifiedTestAgentPlugin;

public class JUnit4Adapter implements TestFrameworkAdapter {
    @Override
    public boolean isApplicable(Project project) {
        return FrameworkDetector.detect(project).contains("JUnit4");
    }
    
    @Override
    public void registerListeners(Project project, Test testTask, UnifiedTestResultCollector collector, ConsoleReporter reporter) {
        testTask.useJUnit();
        // Add framework-specific listener
        testTask.options(option -> {
            try {
                option.getClass().getMethod("listener", Object.class)
                    .invoke(option, new UnifiedJUnit4Listener(collector, reporter));
            } catch (Exception e) {
                project.getLogger().warn("Could not register UnifiedJUnit4Listener: " + e.getMessage());
            }
        });
        // Add pretty console output listener with configured theme
        testTask.addTestListener(new PrettyConsoleTestListener(project, getThemeFromConfig(project)));
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
