package io.github.mov2day.unifiedtest.framework;

import io.github.mov2day.unifiedtest.UnifiedTestAgentPlugin;
import io.github.mov2day.unifiedtest.collector.UnifiedTestResultCollector;
import io.github.mov2day.unifiedtest.reporting.ConsoleReporter;
import io.github.mov2day.unifiedtest.reporting.PrettyConsoleTestListener;
import org.gradle.api.Project;
import org.gradle.api.tasks.testing.Test;

public class SpockAdapter implements TestFrameworkAdapter {
    @Override
    public boolean isApplicable(Project project) {
        return project.getConfigurations().stream().anyMatch(c -> c.getName().toLowerCase().contains("test") &&
                c.getAllDependencies().stream().anyMatch(d -> d.getName().contains("spock-core")));
    }

    @Override
    public void registerListeners(Project project, Test testTask, UnifiedTestResultCollector collector, ConsoleReporter reporter) {
        // Spock uses JUnit platform, so we can reuse some of the JUnit 5 logic
        testTask.useJUnitPlatform();

        // Set the collector for the Spock extension
        io.github.mov2day.unifiedtest.framework.spock.SpockGlobalExtension.setCollector(collector);

        // Add the pretty console listener
        testTask.addTestListener(new PrettyConsoleTestListener(project, getThemeFromConfig(project), collector));

        project.getLogger().lifecycle("UnifiedTest: Adapter for Spock registered");
    }

    @Override
    public String getName() {
        return "Spock";
    }

    private String getThemeFromConfig(Project project) {
        try {
            return project.getExtensions().getByType(UnifiedTestAgentPlugin.UnifiedTestExtensionConfig.class).getTheme().get();
        } catch (Exception e) {
            return "standard"; // Default fallback
        }
    }
}
