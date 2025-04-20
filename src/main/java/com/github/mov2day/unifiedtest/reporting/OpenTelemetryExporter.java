package com.github.mov2day.unifiedtest.agent.reporting;

import org.gradle.api.Project;
import org.gradle.api.tasks.testing.Test;

public class OpenTelemetryExporter {
    public static void export(Project project, Test testTask, String endpoint) {
        // Placeholder: In a real implementation, export test execution data to OpenTelemetry
        project.getLogger().lifecycle("[UnifiedTest] OpenTelemetry export placeholder: test execution data would be sent here. Endpoint: " + endpoint);
    }
}
