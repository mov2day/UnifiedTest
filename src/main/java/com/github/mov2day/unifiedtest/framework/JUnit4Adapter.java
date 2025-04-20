package com.github.mov2day.unifiedtest.agent.framework;

import org.gradle.api.Project;
import org.gradle.api.tasks.testing.Test;
import com.github.mov2day.unifiedtest.agent.collector.UnifiedTestResultCollector;
import com.github.mov2day.unifiedtest.agent.reporting.ConsoleReporter;
import com.github.mov2day.unifiedtest.agent.reporting.UnifiedJUnit4Listener;

public class JUnit4Adapter implements TestFrameworkAdapter {
    @Override
    public boolean isApplicable(Project project) {
        return FrameworkDetector.detect(project).contains("JUnit4");
    }
    @Override
    public void registerListeners(Project project, Test testTask, UnifiedTestResultCollector collector, ConsoleReporter reporter) {
        testTask.useJUnit();
        testTask.options(option -> {
            try {
                option.getClass().getMethod("listener", Object.class)
                    .invoke(option, new UnifiedJUnit4Listener(collector, reporter));
            } catch (Exception e) {
                project.getLogger().warn("Could not register UnifiedJUnit4Listener: " + e.getMessage());
            }
        });
    }
    @Override
    public String getName() { return "JUnit4"; }
}
