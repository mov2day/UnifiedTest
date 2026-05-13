package io.github.mov2day.unifiedtest;

import io.github.mov2day.unifiedtest.reporting.ConsoleReporter;
import io.github.mov2day.unifiedtest.collector.UnifiedTestResultCollector;
import io.github.mov2day.unifiedtest.reporting.JsonReportGenerator;
import io.github.mov2day.unifiedtest.reporting.HtmlReportGenerator;
import io.github.mov2day.unifiedtest.reporting.OpenTelemetryExporter;
import io.github.mov2day.unifiedtest.reporting.GrafanaDashboardGenerator;
import io.github.mov2day.unifiedtest.extension.ExtensionInvoker;
import io.github.mov2day.unifiedtest.extension.TestManagementExtension;
import io.github.mov2day.unifiedtest.reporting.testmanagement.TestManagementSystemFactory;
import io.github.mov2day.unifiedtest.reporting.testmanagement.TestManagementSystem;
import org.gradle.api.Action;
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
import io.github.mov2day.unifiedtest.framework.SpockAdapter;
import io.github.mov2day.unifiedtest.framework.CucumberAdapter;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

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
        private final Property<String> telemetryServiceName;
        private final Property<String> telemetryTraceBaseUrl;
        private final Property<Boolean> dashboardEnabled;
        private final TelemetryConfig telemetry;

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
            this.telemetryServiceName = objects.property(String.class).convention("unified-test");
            this.telemetryTraceBaseUrl = objects.property(String.class).convention("");
            this.dashboardEnabled = objects.property(Boolean.class).convention(true);
            this.telemetry = new TelemetryConfig(objects, telemetryEnabled, telemetryEndpoint, telemetryServiceName, telemetryTraceBaseUrl);
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

        /**
         * Gets the configured service name for telemetry and dashboards.
         * @return the service name property
         */
        public Property<String> getTelemetryServiceName() { return telemetryServiceName; }

        /**
         * Gets the optional trace link base URL used by the HTML report.
         * @return the trace base URL property
         */
        public Property<String> getTelemetryTraceBaseUrl() { return telemetryTraceBaseUrl; }

        /**
         * Gets whether dashboard generation is enabled.
         * @return dashboard enabled property
         */
        public Property<Boolean> getDashboardEnabled() { return dashboardEnabled; }

        /**
         * Nested telemetry DSL: unifiedTest { telemetry { enabled = true } }.
         * @return telemetry config
         */
        public TelemetryConfig getTelemetry() { return telemetry; }

        /**
         * Configures nested telemetry settings.
         * @param action Gradle action
         */
        public void telemetry(Action<? super TelemetryConfig> action) {
            action.execute(telemetry);
        }

        public static class TelemetryConfig {
            private final Property<Boolean> enabled;
            private final Property<String> endpoint;
            private final Property<String> serviceName;
            private final Property<String> traceBaseUrl;

            private TelemetryConfig(ObjectFactory objects, Property<Boolean> enabled, Property<String> endpoint,
                                    Property<String> serviceName, Property<String> traceBaseUrl) {
                this.enabled = enabled;
                this.endpoint = endpoint;
                this.serviceName = serviceName;
                this.traceBaseUrl = traceBaseUrl;
            }

            public Property<Boolean> getEnabled() { return enabled; }
            public Property<String> getEndpoint() { return endpoint; }
            public Property<String> getServiceName() { return serviceName; }
            public Property<String> getTraceBaseUrl() { return traceBaseUrl; }

            public void setEnabled(boolean enabled) { this.enabled.set(enabled); }
            public void setEndpoint(String endpoint) { this.endpoint.set(endpoint); }
            public void setServiceName(String serviceName) { this.serviceName.set(serviceName); }
            public void setTraceBaseUrl(String traceBaseUrl) { this.traceBaseUrl.set(traceBaseUrl); }
        }
    }

    @Override
    public void apply(Project project) {
        UnifiedTestExtensionConfig config = project.getExtensions().create("unifiedTest", UnifiedTestExtensionConfig.class, project.getObjects());
        TestManagementExtension testManagementExtension = project.getExtensions().create("testManagement", TestManagementExtension.class);
        TestManagementSystemFactory testManagementFactory = new TestManagementSystemFactory();
        
        List<TestFrameworkAdapter> adapters = Arrays.asList(
            new SpockAdapter(),
            new CucumberAdapter(),
            new JUnit4Adapter(),
            new JUnit5Adapter(),
            new TestNGAdapter()
        );

        // Report generation: avoid accessing other task's extensions at execution time.
        // Instead generate reports as part of each Test task's doLast (collector is available there).

        // 2. Configure each test task
        project.getTasks().withType(Test.class).configureEach(testTask -> {
            final UnifiedTestResultCollector collector = new UnifiedTestResultCollector();

            // Attach the collector to the test task for later retrieval, only if not already present
            if (testTask.getExtensions().findByName("unifiedTestCollector") == null) {
                testTask.getExtensions().add("unifiedTestCollector", collector);
            }

            // Move framework detection and listener registration to doFirst
            testTask.doFirst(task -> {
                // Initialize test management systems
                testManagementFactory.initialize(testManagementExtension);
                
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
                    // Resolve the theme at execution time so user DSL configuration is visible.
                    String theme = config.getTheme().get();
                    ConsoleReporter reporter = new ConsoleReporter(theme);
                    collector.setFrameworkName(selected.getName());
                    selected.registerListeners(project, testTask, collector, reporter, theme);
                    project.getLogger().lifecycle("UnifiedTest using framework: " + selected.getName());
                } else {
                    collector.setFrameworkName("Gradle");
                    project.getLogger().warn("UnifiedTest: No supported test framework detected or configured. Falling back to default Gradle Test listeners.");
                    testTask.addTestListener(new io.github.mov2day.unifiedtest.reporting.PrettyConsoleTestListener(project, config.getTheme().get(), collector));
                }
            });

            // Add test result callback for test management systems
            collector.setResultCallback(result -> {
                for (TestManagementSystem system : testManagementFactory.getAllSystems()) {
                    if (system.isConfigured()) {
                        system.queueTestResult(result);
                    }
                }
            });

            // Push results to test management systems after test execution and generate reports here
            testTask.doLast(task -> {
                String runId = UUID.randomUUID().toString();
                String serviceName = resolveServiceName(project, config);
                boolean telemetryEnabled = config.getTelemetry().getEnabled().get();
                String telemetryEndpoint = config.getTelemetry().getEndpoint().getOrElse("");
                String traceBaseUrl = config.getTelemetry().getTraceBaseUrl().getOrElse("");

                for (TestManagementSystem system : testManagementFactory.getAllSystems()) {
                    if (system.isConfigured()) {
                        try {
                            system.flushResults();
                            project.getLogger().lifecycle("Successfully pushed test results to {}", 
                                system.getName());
                        } catch (Exception e) {
                            project.getLogger().error("Failed to push results to {}: {}", 
                                system.getName(), e.getMessage());
                        }
                    }
                }

                if (telemetryEnabled) {
                    OpenTelemetryExporter.export(project, testTask, collector, telemetryEndpoint, serviceName, runId, traceBaseUrl);
                }

                // Generate reports directly using the collector available here (execution of this task)
                if (config.getJsonEnabled().get()) {
                    JsonReportGenerator.generate(project, testTask, collector, serviceName, runId);
                }
                if (config.getHtmlEnabled().get()) {
                    HtmlReportGenerator.generate(project, testTask, collector, serviceName, runId, telemetryEnabled, traceBaseUrl);
                }
                if (config.getDashboardEnabled().get()) {
                    GrafanaDashboardGenerator.generate(project, testTask, collector, serviceName, runId);
                }
            });
        });
    }

    private String resolveServiceName(Project project, UnifiedTestExtensionConfig config) {
        String configured = config.getTelemetry().getServiceName().getOrElse("");
        if (configured == null || configured.isBlank() || "unified-test".equals(configured)) {
            return project.getName();
        }
        return configured;
    }
}
