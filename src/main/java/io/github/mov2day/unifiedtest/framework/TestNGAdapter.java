package io.github.mov2day.unifiedtest.framework;

import org.gradle.api.Project;
import org.gradle.api.tasks.testing.Test;
import io.github.mov2day.unifiedtest.collector.UnifiedTestResultCollector;
import io.github.mov2day.unifiedtest.reporting.ConsoleReporter;
import io.github.mov2day.unifiedtest.reporting.UnifiedTestNGListener;

public class TestNGAdapter implements TestFrameworkAdapter {
    @Override
    public boolean isApplicable(Project project) {
        return FrameworkDetector.detect(project).contains("TestNG");
    }
    @Override
    public void registerListeners(Project project, Test testTask, UnifiedTestResultCollector collector, ConsoleReporter reporter) {
        testTask.useTestNG();
        testTask.options(option -> {
            try {
                option.getClass().getMethod("listener", Object.class)
                    .invoke(option, new UnifiedTestNGListener(collector, reporter));
            } catch (Exception e) {
                project.getLogger().warn("Could not register UnifiedTestNGListener: " + e.getMessage());
            }
        });
    }
    @Override
    public String getName() { return "TestNG"; }
}
