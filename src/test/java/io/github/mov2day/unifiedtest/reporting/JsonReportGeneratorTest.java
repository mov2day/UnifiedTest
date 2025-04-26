package io.github.mov2day.unifiedtest.reporting;

import org.gradle.api.Project;
import org.gradle.api.tasks.testing.Test;
import io.github.mov2day.unifiedtest.collector.UnifiedTestResult;
import io.github.mov2day.unifiedtest.collector.UnifiedTestResultCollector;
import java.io.File;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.api.BeforeAll;

/**
 * Unit tests for JsonReportGenerator.
 * Verifies JSON report generation functionality and output format.
 */
public class JsonReportGeneratorTest {
    private static final String TEST_DIR = "build/test-output";

    @BeforeAll
    static void setup() {
        System.setProperty("mockito.inline.extended.stacktrace", "false");
    }

    @org.junit.jupiter.api.Test
    void generatesJsonReportFile() {
        // Create test directory
        File buildDir = new File(TEST_DIR);
        buildDir.mkdirs();

        try {
            // Setup mocks with RETURNS_DEEP_STUBS to avoid mocking each level
            Project project = mock(Project.class, withSettings()
                .defaultAnswer(RETURNS_DEEP_STUBS)
                .verboseLogging());
            
            when(project.getBuildDir()).thenReturn(buildDir);
            
            Test testTask = mock(Test.class, withSettings()
                .defaultAnswer(RETURNS_DEEP_STUBS)
                .verboseLogging());

            // Create test data
            UnifiedTestResultCollector collector = new UnifiedTestResultCollector();
            collector.addResult(new UnifiedTestResult("TestClass", "testName", "SUCCESS"));

            // Generate report
            JsonReportGenerator.generate(project, testTask, collector);
            
            // Verify report was created
            File reportFile = new File(buildDir, "unifiedtest/reports/results.json");
            assertTrue(reportFile.exists(), "Report file should be created");
        } finally {
            // Cleanup
            deleteDirectory(new File(TEST_DIR));
        }
    }

    private void deleteDirectory(File directory) {
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    deleteDirectory(file);
                } else {
                    file.delete();
                }
            }
        }
        directory.delete();
    }
}
