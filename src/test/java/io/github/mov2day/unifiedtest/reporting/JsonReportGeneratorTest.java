package io.github.mov2day.unifiedtest.reporting;

import org.gradle.api.Project;
import org.gradle.api.tasks.testing.Test;
import io.github.mov2day.unifiedtest.collector.UnifiedTestResult;
import io.github.mov2day.unifiedtest.collector.UnifiedTestResultCollector;
import java.io.File;
import java.nio.file.Files;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.io.TempDir;

/**
 * Unit tests for JsonReportGenerator.
 * Verifies JSON report generation functionality and output format.
 */
@ExtendWith(MockitoExtension.class)
public class JsonReportGeneratorTest {
    @Mock
    private Project project;
    @Mock
    private Test testTask;
    
    @TempDir
    File tempDir;
    
    @BeforeEach
    void setup() {
        when(project.getBuildDir()).thenReturn(tempDir);
        new File(tempDir, "unifiedtest/reports").mkdirs();
    }
    
    @org.junit.jupiter.api.Test
    void generatesJsonReportFile() throws Exception {
        // Create test data
        UnifiedTestResultCollector collector = new UnifiedTestResultCollector();
        collector.addResult(new UnifiedTestResult("TestClass", "testName", "SUCCESS"));

        // Generate report
        JsonReportGenerator.generate(project, testTask, collector);
        
        // Verify report file exists and contains content
        File reportFile = new File(tempDir, "unifiedtest/reports/results.json");
        assertTrue(reportFile.exists(), "Report file should be generated");
        String content = Files.readString(reportFile.toPath());
        assertTrue(content.contains("TestClass"), "Report should contain test class name");
        assertTrue(content.contains("testName"), "Report should contain test name");
        assertTrue(content.contains("SUCCESS"), "Report should contain test result");
    }
}
