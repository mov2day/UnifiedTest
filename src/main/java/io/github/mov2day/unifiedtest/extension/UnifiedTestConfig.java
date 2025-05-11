package io.github.mov2day.unifiedtest.extension;

import org.gradle.api.Project;
import org.gradle.api.tasks.testing.Test;
import org.gradle.api.provider.Property;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.tasks.testing.logging.TestLoggingContainer;
import org.gradle.api.tasks.testing.logging.TestLogEvent;
import javax.inject.Inject;
import java.util.EnumSet;

/**
 * Configuration class for UnifiedTest plugin.
 * Provides settings for test execution and report generation.
 */
public class UnifiedTestConfig {
    private final Property<Boolean> enabled;
    private final Property<String> reportDir;
    private final Property<Boolean> generateHtmlReport;
    private final Property<Boolean> generateAllureReport;

    @Inject
    public UnifiedTestConfig(ObjectFactory objects) {
        this.enabled = objects.property(Boolean.class).convention(true);
        this.reportDir = objects.property(String.class).convention("build/reports/unified-test");
        this.generateHtmlReport = objects.property(Boolean.class).convention(true);
        this.generateAllureReport = objects.property(Boolean.class).convention(true);
    }

    /**
     * Whether the plugin is enabled.
     */
    public Property<Boolean> getEnabled() {
        return enabled;
    }

    /**
     * Directory where test reports will be generated.
     */
    public Property<String> getReportDir() {
        return reportDir;
    }

    /**
     * Whether to generate HTML reports.
     */
    public Property<Boolean> getGenerateHtmlReport() {
        return generateHtmlReport;
    }

    /**
     * Whether to generate Allure reports.
     */
    public Property<Boolean> getGenerateAllureReport() {
        return generateAllureReport;
    }

    /**
     * Configure a test task with UnifiedTest settings.
     * @param testTask The test task to configure
     */
    public void configureTestTask(Test testTask) {
        if (!enabled.get()) {
            return;
        }

        // Configure test task
        TestLoggingContainer testLogging = testTask.getTestLogging();
        testLogging.setEvents(EnumSet.of(
            TestLogEvent.PASSED,
            TestLogEvent.SKIPPED,
            TestLogEvent.FAILED
        ));
        testLogging.setShowExceptions(true);
        testLogging.setShowCauses(true);
        testLogging.setShowStackTraces(true);

        // Set up test listeners and reporters
        testTask.doFirst(task -> {
            // Initialize test execution
        });

        testTask.doLast(task -> {
            // Finalize test execution
        });
    }
} 