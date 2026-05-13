package io.github.mov2day.unifiedtest.framework;

import org.gradle.api.Project;
import org.gradle.api.tasks.testing.Test;
import io.github.mov2day.unifiedtest.collector.UnifiedTestResultCollector;
import io.github.mov2day.unifiedtest.reporting.ConsoleReporter;
import io.github.mov2day.unifiedtest.reporting.PrettyConsoleTestListener;
import io.github.mov2day.unifiedtest.reporting.UnifiedJUnit5Listener;

/**
 * Cucumber adapter. The preferred path is cucumber-junit-platform-engine.
 */
public class CucumberAdapter implements TestFrameworkAdapter {
    @Override
    public boolean isApplicable(Project project) {
        return FrameworkDetector.detect(project).contains("Cucumber");
    }

    @Override
    public void registerListeners(Project project, Test testTask, UnifiedTestResultCollector collector, ConsoleReporter reporter, String theme) {
        testTask.useJUnitPlatform();
        UnifiedJUnit5Listener.setCollectorAndReporter(collector, reporter);
        testTask.systemProperty("unifiedtest.environment", "gradle");
        testTask.systemProperty("junit.jupiter.extensions.autodetection.enabled", "true");
        testTask.addTestListener(new PrettyConsoleTestListener(project, theme, collector));
        project.getLogger().lifecycle("UnifiedTest: Gradle adapter for Cucumber registered");
    }

    @Override
    public String getName() {
        return "Cucumber";
    }
}
