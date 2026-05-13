package io.github.mov2day.unifiedtest.reporting;

import io.github.mov2day.unifiedtest.collector.UnifiedTestResult;
import io.github.mov2day.unifiedtest.collector.UnifiedTestResultCollector;
import org.gradle.api.Project;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class OpenTelemetryExporterTest {
    @TempDir
    Path tempDir;

    @Test
    void exporterFailsSoftWhenEndpointMissing() {
        Project project = ProjectBuilder.builder().withProjectDir(tempDir.toFile()).build();
        org.gradle.api.tasks.testing.Test testTask = project.getTasks().create("test", org.gradle.api.tasks.testing.Test.class);
        UnifiedTestResultCollector collector = new UnifiedTestResultCollector();
        collector.setFrameworkName("JUnit5");
        collector.addResult(new UnifiedTestResult("example.SampleTest", "passes", "PASS", 12L));

        assertDoesNotThrow(() -> OpenTelemetryExporter.export(project, testTask, collector, "", "sample-service", "run-1", ""));
    }
}
