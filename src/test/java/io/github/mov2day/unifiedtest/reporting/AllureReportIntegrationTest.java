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
        File allureResultsDir = new File(project.getLayout().getBuildDirectory().get().getAsFile(), "allure-results");
        allureResultsDir.mkdirs();

        // Create a mock result file
        String mockResult = """
            {
                "uuid": "uuid-1",
                "historyId": "history-1",
                "testCaseId": "case-1",
                "name": "testMethod",
                "fullName": "com.example.CalculatorTest.testMethod",
                "status": "passed",
                "stage": "finished",
                "description": "Validates calculator behavior",
                "labels": [
                    {"name": "owner", "value": "qa"},
                    {"name": "severity", "value": "critical"},
                    {"name": "feature", "value": "Calculator"}
                ],
                "parameters": [
                    {"name": "browser", "value": "chrome"}
                ],
                "links": [
                    {"name": "BUG-1", "url": "https://example.test/BUG-1", "type": "issue"}
                ],
                "statusDetails": {
                    "message": "Recovered by retry",
                    "trace": "stack trace text",
                    "known": true,
                    "flaky": true
                },
                "start": 1234567890,
                "stop": 1234567990,
                "steps": [
                    {
                        "name": "Step 1",
                        "status": "passed",
                        "stage": "finished",
                        "start": 1234567890,
                        "stop": 1234567900,
                        "parameters": [
                            {"name": "coupon", "value": "SUMMER"}
                        ],
                        "attachments": [
                            {
                                "source": "network.json",
                                "name": "Network log",
                                "type": "application/json"
                            }
                        ],
                        "steps": [
                            {
                                "name": "Nested check",
                                "status": "passed",
                                "start": 1234567900,
                                "stop": 1234567910
                            }
                        ]
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
        Files.writeString(allureResultsDir.toPath().resolve("test-result.json"), mockResult);
        Files.writeString(allureResultsDir.toPath().resolve("screenshot.png"), "fake image");
        Files.writeString(allureResultsDir.toPath().resolve("network.json"), "{}");

        assertTrue(allureReader.hasAllureReports());
        Map<String, AllureReportReader.AllureTestResult> results = allureReader.readAllureResults();
        assertFalse(results.isEmpty());
        
        AllureReportReader.AllureTestResult result = results.get("com.example.CalculatorTest.testMethod");
        assertNotNull(result);
        assertEquals("passed", result.getStatus());
        assertEquals(1, result.getSteps().size());
        assertEquals(1, result.getAttachments().size());
        assertEquals("qa", result.getFirstLabel("owner"));
        assertEquals("critical", result.getFirstLabel("severity"));
        assertEquals("Recovered by retry", result.getStatusMessage());
        assertTrue(result.isKnown());
        assertTrue(result.isFlaky());
        assertEquals(2, result.getTotalStepCount());
        assertEquals(2, result.getAllAttachments().size());
        assertEquals(1, result.getImageAttachments().size());
        assertTrue(result.getImageAttachments().get(0).getFileUri().startsWith("file:"));
        assertEquals("browser", result.getParameters().get(0).getName());
        assertEquals("BUG-1", result.getLinks().get(0).getName());
    }

    @Test
    void testAllureReportPath() throws IOException {
        // Create mock Allure report directory
        File allureReportDir = new File(project.getLayout().getBuildDirectory().get().getAsFile(), "allure-report");
        allureReportDir.mkdirs();
        Files.write(allureReportDir.toPath().resolve("index.html"), "<html></html>".getBytes());

        String reportPath = allureReader.getAllureReportPath();
        assertNotNull(reportPath);
        assertTrue(new File(reportPath).exists());
    }
} 
