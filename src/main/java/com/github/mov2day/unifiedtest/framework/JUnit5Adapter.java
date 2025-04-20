package com.github.mov2day.unifiedtest.agent.framework;

import org.gradle.api.Project;
import org.gradle.api.tasks.testing.Test;
import com.github.mov2day.unifiedtest.agent.collector.UnifiedTestResultCollector;
import com.github.mov2day.unifiedtest.agent.reporting.ConsoleReporter;
import com.github.mov2day.unifiedtest.agent.reporting.UnifiedJUnit5Listener;

public class JUnit5Adapter implements TestFrameworkAdapter {
    @Override
    public boolean isApplicable(Project project) {
        return FrameworkDetector.detect(project).contains("JUnit5");
    }
    @Override
    public void registerListeners(Project project, Test testTask, UnifiedTestResultCollector collector, ConsoleReporter reporter) {
        testTask.useJUnitPlatform();
        // JUnit5 listeners are typically registered via ServiceLoader or system property.
        // For demonstration, you may need to instruct users to register UnifiedJUnit5Listener via ServiceLoader or system property.
    }
    @Override
    public String getName() { return "JUnit5"; }
}
