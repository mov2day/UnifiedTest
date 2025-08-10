package io.github.mov2day.unifiedtest.framework;

import io.github.mov2day.unifiedtest.UnifiedTestAgentPlugin;
import io.github.mov2day.unifiedtest.collector.UnifiedTestResultCollector;
import io.github.mov2day.unifiedtest.reporting.ConsoleReporter;
import io.github.mov2day.unifiedtest.reporting.PrettyConsoleTestListener;
import org.gradle.api.Project;
import org.gradle.api.tasks.testing.Test;

public class CucumberAdapter implements TestFrameworkAdapter {
    @Override
    public boolean isApplicable(Project project) {
        return project.getConfigurations().stream().anyMatch(c -> c.getName().toLowerCase().contains("test") &&
                c.getAllDependencies().stream().anyMatch(d -> d.getName().contains("cucumber-java")));
    }

    @Override
    public void registerListeners(Project project, Test testTask, UnifiedTestResultCollector collector, ConsoleReporter reporter) {
        // For Cucumber, we need to configure the test task to use our custom plugin.
        // This is typically done by setting a system property or a command-line option.
        // We will set a system property to specify the plugin.
        testTask.systemProperty("cucumber.plugin", "io.github.mov2day.unifiedtest.framework.cucumber.CucumberReporter");

        // We also need to set the collector for the reporter to use.
        io.github.mov2day.unifiedtest.framework.cucumber.CucumberReporter.setCollector(collector);

        // Add the pretty console listener
        testTask.addTestListener(new PrettyConsoleTestListener(project, getThemeFromConfig(project), collector));

        project.getLogger().lifecycle("UnifiedTest: Adapter for Cucumber registered");
    }

    @Override
    public String getName() {
        return "Cucumber";
    }

    private String getThemeFromConfig(Project project) {
        return FrameworkUtils.getThemeFromConfig(project);
    }
}
