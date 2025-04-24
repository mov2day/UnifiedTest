package io.github.mov2day.unifiedtest.framework;

import org.gradle.api.Project;
import org.gradle.api.tasks.testing.Test;
import io.github.mov2day.unifiedtest.collector.UnifiedTestResultCollector;
import io.github.mov2day.unifiedtest.reporting.ConsoleReporter;
import io.github.mov2day.unifiedtest.reporting.UnifiedJUnit5Listener;
import io.github.mov2day.unifiedtest.reporting.PrettyConsoleTestListener;

public class JUnit5Adapter implements TestFrameworkAdapter {
    @Override
    public boolean isApplicable(Project project) {
        return FrameworkDetector.detect(project).contains("JUnit5");
    }

    @Override
    public void registerListeners(Project project, Test testTask, UnifiedTestResultCollector collector, ConsoleReporter reporter) {
        testTask.useJUnitPlatform();
        // Initialize the static collector and reporter in UnifiedJUnit5Listener
        UnifiedJUnit5Listener.setCollectorAndReporter(collector, reporter);
        // Add the test listener using Gradle's test listener API
        testTask.addTestListener(new PrettyConsoleTestListener(project, "standard"));
    }

    @Override
    public String getName() { return "JUnit5"; }
}
