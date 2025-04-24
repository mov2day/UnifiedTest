package io.github.mov2day.unifiedtest.reporting;

import org.gradle.api.Project;
import org.gradle.api.tasks.testing.Test;

import io.github.mov2day.unifiedtest.collector.UnifiedTestResult;
import io.github.mov2day.unifiedtest.collector.UnifiedTestResultCollector;

import java.io.File;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for JsonReportGenerator.
 * Verifies JSON report generation functionality and output format.
 */
public class JsonReportGeneratorTest {
    @org.junit.jupiter.api.Test
    void generatesJsonReportFile() {
        Project project = mock(Project.class);
        Test testTask = mock(Test.class);
        File buildDir = new File("build/test-output");
        when(project.getLayout().getBuildDirectory().get().getAsFile()).thenReturn(buildDir);
        
        UnifiedTestResultCollector collector = new UnifiedTestResultCollector();
        collector.addResult(new UnifiedTestResult("TestClass", "testName", "SUCCESS"));

        JsonReportGenerator.generate(project, testTask, collector);
        
        File reportFile = new File(buildDir, "unifiedtest/report.json");
        assertTrue(reportFile.exists());
        reportFile.delete();
    }
}
