package io.github.mov2day.unifiedtest.reporting;

import org.gradle.api.Project;
import org.gradle.api.tasks.testing.Test;

/**
 * Exports test results to OpenTelemetry for observability and monitoring.
 * Enables integration with OpenTelemetry-compatible monitoring systems.
 */
public class OpenTelemetryExporter {
    /**
     * Exports test results to an OpenTelemetry endpoint.
     * @param project the Gradle project
     * @param testTask the test task
     * @param endpoint the OpenTelemetry endpoint URL to export to
     */
    public static void export(Project project, Test testTask, String endpoint) {
        // Placeholder: In a real implementation, export test execution data to OpenTelemetry
        project.getLogger().lifecycle("[UnifiedTest] OpenTelemetry export placeholder: test execution data would be sent here. Endpoint: " + endpoint);
    }
}
