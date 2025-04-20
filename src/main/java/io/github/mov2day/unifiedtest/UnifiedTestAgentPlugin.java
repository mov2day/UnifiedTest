package io.github.mov2day.unifiedtest;

import io.github.mov2day.unifiedtest.framework.FrameworkDetector;
import io.github.mov2day.unifiedtest.reporting.ConsoleReporter;
import io.github.mov2day.unifiedtest.collector.UnifiedTestResultCollector;
import io.github.mov2day.unifiedtest.reporting.PrettyConsoleTestListener;
import io.github.mov2day.unifiedtest.reporting.JsonReportGenerator;
import io.github.mov2day.unifiedtest.reporting.HtmlReportGenerator;
import io.github.mov2day.unifiedtest.reporting.OpenTelemetryExporter;
import io.github.mov2day.unifiedtest.extension.ExtensionInvoker;
import io.github.mov2day.unifiedtest.reporting.UnifiedJUnit4Listener;
import io.github.mov2day.unifiedtest.reporting.UnifiedJUnit5Listener;
import io.github.mov2day.unifiedtest.reporting.UnifiedTestNGListener;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.tasks.testing.Test;
import org.gradle.api.provider.Property;
import org.gradle.api.model.ObjectFactory;
import javax.inject.Inject;
import java.util.Set;
import io.github.mov2day.unifiedtest.framework.TestFrameworkAdapter;
import io.github.mov2day.unifiedtest.framework.JUnit4Adapter;
import io.github.mov2day.unifiedtest.framework.JUnit5Adapter;
import io.github.mov2day.unifiedtest.framework.TestNGAdapter;
import java.util.Arrays;
import java.util.List;

/**
 * Entry point for the UnifiedTest Gradle plugin.
 * Provides advanced test automation observability and reporting for JUnit4, JUnit5, and TestNG.
 * Excludes Spock and Cucumber.
 */
public class UnifiedTestAgentPlugin implements Plugin<Project> {
    public static class UnifiedTestExtensionConfig {
        private final Property<String> theme;
        private final Property<String> framework;
        private final Property<Boolean> jsonEnabled;
        private final Property<Boolean> htmlEnabled;
        private final Property<Boolean> telemetryEnabled;
        private final Property<String> telemetryEndpoint;
        @Inject
        public UnifiedTestExtensionConfig(ObjectFactory objects) {
            this.theme = objects.property(String.class).convention("standard");
            this.framework = objects.property(String.class).convention("");
            this.jsonEnabled = objects.property(Boolean.class).convention(true);
            this.htmlEnabled = objects.property(Boolean.class).convention(true);
            this.telemetryEnabled = objects.property(Boolean.class).convention(false);
            this.telemetryEndpoint = objects.property(String.class).convention("");
        }
        public Property<String> getTheme() { return theme; }
        public Property<String> getFramework() { return framework; }
        public Property<Boolean> getJsonEnabled() { return jsonEnabled; }
        public Property<Boolean> getHtmlEnabled() { return htmlEnabled; }
        public Property<Boolean> getTelemetryEnabled() { return telemetryEnabled; }
        public Property<String> getTelemetryEndpoint() { return telemetryEndpoint; }
    }

    @Override
    public void apply(Project project) {
        UnifiedTestExtensionConfig config = project.getExtensions().create("unifiedTest", UnifiedTestExtensionConfig.class, project.getObjects());
        List<TestFrameworkAdapter> adapters = Arrays.asList(
            new JUnit4Adapter(),
            new JUnit5Adapter(),
            new TestNGAdapter()
        );
        project.getTasks().withType(Test.class).configureEach(testTask -> {
            final UnifiedTestResultCollector collector = new UnifiedTestResultCollector();
            final ConsoleReporter reporter = new ConsoleReporter(config.getTheme().get());
            String frameworkConfig = config.getFramework().get();
            TestFrameworkAdapter selected = null;
            if (!frameworkConfig.isEmpty()) {
                for (TestFrameworkAdapter adapter : adapters) {
                    if (adapter.getName().equalsIgnoreCase(frameworkConfig)) {
                        selected = adapter;
                        break;
                    }
                }
            } else {
                for (TestFrameworkAdapter adapter : adapters) {
                    if (adapter.isApplicable(project)) {
                        selected = adapter;
                        break;
                    }
                }
            }
            if (selected != null) {
                selected.registerListeners(project, testTask, collector, reporter);
                project.getLogger().lifecycle("UnifiedTest using framework: " + selected.getName());
            } else {
                project.getLogger().warn("UnifiedTest: No supported test framework detected or configured.");
            }
            if (config.getJsonEnabled().get()) {
                testTask.doLast(task -> JsonReportGenerator.generate(project, collector.getResults()));
            }
            if (config.getHtmlEnabled().get()) {
                testTask.doLast(task -> HtmlReportGenerator.generate(project, collector.getResults()));
            }
            if (config.getTelemetryEnabled().get()) {
                testTask.doLast(task -> OpenTelemetryExporter.export(project, testTask, config.getTelemetryEndpoint().get()));
            }
            testTask.doLast(task -> ExtensionInvoker.invoke(project, testTask));
        });
    }
}