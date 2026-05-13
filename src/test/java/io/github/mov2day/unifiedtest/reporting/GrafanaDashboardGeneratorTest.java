package io.github.mov2day.unifiedtest.reporting;

import io.github.mov2day.unifiedtest.collector.UnifiedTestResult;
import io.github.mov2day.unifiedtest.collector.UnifiedTestResultCollector;
import org.gradle.api.Project;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class GrafanaDashboardGeneratorTest {
    @TempDir
    Path tempDir;

    @Test
    void generatesGrafanaDashboardJson() throws Exception {
        Project project = ProjectBuilder.builder().withProjectDir(tempDir.toFile()).build();
        org.gradle.api.tasks.testing.Test testTask = project.getTasks().create("test", org.gradle.api.tasks.testing.Test.class);
        UnifiedTestResultCollector collector = new UnifiedTestResultCollector();
        collector.setFrameworkName("JUnit5");
        collector.addResult(new UnifiedTestResult("example.SampleTest", "passes", "PASS", 12L));

        GrafanaDashboardGenerator.generate(project, testTask, collector, "sample-service", "run-1");

        File dashboard = project.getLayout().getBuildDirectory().file("unifiedtest/dashboard/grafana-dashboard.json").get().getAsFile();
        assertTrue(dashboard.exists(), "Dashboard JSON should exist");
        String content = Files.readString(dashboard.toPath());
        assertTrue(content.contains("\"title\""), "Dashboard should include title");
        assertTrue(content.contains("sample-service"), "Dashboard should include service filter");
        assertTrue(content.contains("Failure Trace Search"), "Dashboard should include failure panel");
    }
}
