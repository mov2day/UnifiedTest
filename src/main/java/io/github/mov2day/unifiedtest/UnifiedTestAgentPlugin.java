package io.github.mov2day.unifiedtest;

import io.github.mov2day.unifiedtest.reporting.ConsoleReporter;
import io.github.mov2day.unifiedtest.collector.UnifiedTestResultCollector;
import io.github.mov2day.unifiedtest.reporting.JsonReportGenerator;
import io.github.mov2day.unifiedtest.reporting.HtmlReportGenerator;
import io.github.mov2day.unifiedtest.reporting.OpenTelemetryExporter;
import io.github.mov2day.unifiedtest.extension.ExtensionInvoker;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.tasks.testing.Test;
import org.gradle.api.provider.Property;
import org.gradle.api.model.ObjectFactory;
import javax.inject.Inject;
import io.github.mov2day.unifiedtest.framework.TestFrameworkAdapter;
import io.github.mov2day.unifiedtest.framework.JUnit4Adapter;
import io.github.mov2day.unifiedtest.framework.JUnit5Adapter;
import io.github.mov2day.unifiedtest.framework.TestNGAdapter;
import java.util.Arrays;
import java.util.List;
import java.io.File;

/**
 * Main plugin class for UnifiedTest that provides test execution monitoring and reporting.
 * Configures and manages test execution listeners for different test frameworks.
 */
public class UnifiedTestAgentPlugin implements Plugin<Project> {
    /**
     * Configuration class for UnifiedTest plugin extension.
     * Provides configuration options for test framework selection and report generation.
     */
    public static class UnifiedTestExtensionConfig {
        private final Property<String> theme;
        private final Property<String> framework;
        private final Property<Boolean> jsonEnabled;
        private final Property<Boolean> htmlEnabled;
        private final Property<Boolean> telemetryEnabled;
        private final Property<String> telemetryEndpoint;

        /**
         * Creates a new configuration instance.
         * @param objects the object factory for creating properties
         */
        @Inject
        public UnifiedTestExtensionConfig(ObjectFactory objects) {
            this.theme = objects.property(String.class).convention("standard");
            this.framework = objects.property(String.class).convention("");
            this.jsonEnabled = objects.property(Boolean.class).convention(true);
            this.htmlEnabled = objects.property(Boolean.class).convention(true);
            this.telemetryEnabled = objects.property(Boolean.class).convention(false);
            this.telemetryEndpoint = objects.property(String.class).convention("");
        }

        /**
         * Gets the configured test framework.
         * @return the test framework property
         */
        public Property<String> getFramework() { return framework; }

        /**
         * Gets the configured theme for console output.
         * @return the theme property
         */
        public Property<String> getTheme() { return theme; }

        /**
         * Gets whether JSON report generation is enabled.
         * @return the JSON enabled property
         */
        public Property<Boolean> getJsonEnabled() { return jsonEnabled; }

        /**
         * Gets whether HTML report generation is enabled.
         * @return the HTML enabled property
         */
        public Property<Boolean> getHtmlEnabled() { return htmlEnabled; }

        /**
         * Gets whether OpenTelemetry export is enabled.
         * @return the telemetry enabled property
         */
        public Property<Boolean> getTelemetryEnabled() { return telemetryEnabled; }

        /**
         * Gets the configured OpenTelemetry endpoint.
         * @return the telemetry endpoint property
         */
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

        // 1. Register report tasks for all test tasks
        project.getTasks().withType(Test.class).all(testTask -> {
            String reportTaskName = testTask.getName() + "UnifiedTestReport";
            project.getTasks().register(reportTaskName, t -> {
                t.doLast(task -> {
                    // Retrieve the collector from the test task's extensions
                    UnifiedTestResultCollector collector = (UnifiedTestResultCollector) testTask.getExtensions().findByName("unifiedTestCollector");
                    if (config.getJsonEnabled().get()) {
                        File reportsDir = new File(project.getBuildDir(), "unifiedtest/reports");
                        reportsDir.mkdirs();
                        JsonReportGenerator.generate(project, testTask, collector);
                    }
                    if (config.getHtmlEnabled().get()) {
                        File reportsDir = new File(project.getBuildDir(), "unifiedtest/reports");
                        reportsDir.mkdirs();
                        HtmlReportGenerator.generate(project, testTask, collector);
                    }
                });
            });
        });

        // 2. Configure each test task
        project.getTasks().withType(Test.class).configureEach(testTask -> {
            final UnifiedTestResultCollector collector = new UnifiedTestResultCollector();
            final ConsoleReporter reporter = new ConsoleReporter(config.getTheme().get());

            // Attach the collector to the test task for later retrieval, only if not already present
            if (testTask.getExtensions().findByName("unifiedTestCollector") == null) {
                testTask.getExtensions().add("unifiedTestCollector", collector);
            }

            // Move framework detection and listener registration to doFirst
            testTask.doFirst(task -> {
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
                    project.getLogger().warn("UnifiedTest: No supported test framework detected or configured. Falling back to default Gradle Test listeners.");
                    testTask.addTestListener(new io.github.mov2day.unifiedtest.reporting.PrettyConsoleTestListener(project, config.getTheme().get(), collector));
                }
            });

            // Ensure report generation runs even if tests fail
            String reportTaskName = testTask.getName() + "UnifiedTestReport";
            testTask.finalizedBy(reportTaskName);
        });
    }
}