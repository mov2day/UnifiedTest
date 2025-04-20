package io.github.mov2day.unifiedtest.framework;

import org.gradle.api.Project;
import org.gradle.api.tasks.testing.Test;
import io.github.mov2day.unifiedtest.collector.UnifiedTestResultCollector;
import io.github.mov2day.unifiedtest.reporting.ConsoleReporter;

public interface TestFrameworkAdapter {
    boolean isApplicable(Project project);
    void registerListeners(Project project, Test testTask, UnifiedTestResultCollector collector, ConsoleReporter reporter);
    String getName();
}
