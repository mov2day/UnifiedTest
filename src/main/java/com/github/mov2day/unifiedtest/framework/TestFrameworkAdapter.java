package com.github.mov2day.unifiedtest.agent.framework;

import org.gradle.api.Project;
import org.gradle.api.tasks.testing.Test;
import com.github.mov2day.unifiedtest.agent.collector.UnifiedTestResultCollector;
import com.github.mov2day.unifiedtest.agent.reporting.ConsoleReporter;

public interface TestFrameworkAdapter {
    boolean isApplicable(Project project);
    void registerListeners(Project project, Test testTask, UnifiedTestResultCollector collector, ConsoleReporter reporter);
    String getName();
}
