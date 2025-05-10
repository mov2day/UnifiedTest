package io.github.mov2day.unifiedtest.reporting;

import org.gradle.api.Project;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class AllureReportIntegrationTest {
    private Project project;
    private AllureReportReader allureReader;
    @TempDir
    File tempDir;

    @BeforeEach
    void setUp() {
        project = ProjectBuilder.builder().withProjectDir(tempDir).build();
        allureReader = new AllureReportReader(project);
    }

    @Test
    void testNoAllureReports() {
        assertFalse(allureReader.hasAllureReports());
        assertTrue(allureReader.readAllureResults().isEmpty());
        assertNull(allureReader.getAllureReportPath());
    }

    @Test
    void testAllureReportDetection() throws IOException {
        // Create mock Allure results directory
        File allureResultsDir = new File(project.getBuildDir(), "allure-results");
        allureResultsDir.mkdirs();

        // Create a mock result file
        String mockResult = """
            {
                "name": "testMethod",
                "status": "passed",
                "stage": "finished",
                "start": 1234567890,
                "stop": 1234567891,
                "steps": [
                    {
                        "name": "Step 1",
                        "status": "passed"
                    }
                ],
                "attachments": [
                    {
                        "source": "screenshot.png",
                        "name": "Screenshot",
                        "type": "image/png"
                    }
                ]
            }
            """;
        Files.write(allureResultsDir.toPath().resolve("test-result.json"), mockResult.getBytes());

        assertTrue(allureReader.hasAllureReports());
        Map<String, AllureReportReader.AllureTestResult> results = allureReader.readAllureResults();
        assertFalse(results.isEmpty());
        
        AllureReportReader.AllureTestResult result = results.get("testMethod");
        assertNotNull(result);
        assertEquals("passed", result.getStatus());
        assertEquals(1, result.getSteps().size());
        assertEquals(1, result.getAttachments().size());
    }

    @Test
    void testAllureReportPath() throws IOException {
        // Create mock Allure report directory
        File allureReportDir = new File(project.getBuildDir(), "allure-report");
        allureReportDir.mkdirs();
        Files.write(allureReportDir.toPath().resolve("index.html"), "<html></html>".getBytes());

        String reportPath = allureReader.getAllureReportPath();
        assertNotNull(reportPath);
        assertTrue(new File(reportPath).exists());
    }
} 