package io.github.mov2day.unifiedtest.reporting;

import io.github.mov2day.unifiedtest.collector.UnifiedTestResult;
import io.github.mov2day.unifiedtest.collector.UnifiedTestResultCollector;
import org.gradle.api.Project;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class HtmlReportGeneratorTest {
    private Project project;
    private org.gradle.api.tasks.testing.Test testTask;

    @TempDir
    Path tempDir;

    private UnifiedTestResultCollector collector;

    @BeforeEach
    void setUp() {
        project = ProjectBuilder.builder().withProjectDir(tempDir.toFile()).build();
        testTask = project.getTasks().create("test", org.gradle.api.tasks.testing.Test.class);
        collector = new UnifiedTestResultCollector();
    }

    @org.junit.jupiter.api.Test
    void shouldGenerateHtmlReportWithAllTestStatuses() throws Exception {
        // Given
        collector.addResult(new UnifiedTestResult(
            "com.example.CalculatorTest",
            "testAddition",
            "PASS",
            100L
        ));

        collector.addResult(new UnifiedTestResult(
            "com.example.CalculatorTest",
            "testDivision",
            "FAIL",
            "Division by zero",
            "java.lang.ArithmeticException: Division by zero\n    at Calculator.divide(Calculator.java:15)",
            150L
        ));

        collector.addResult(new UnifiedTestResult(
            "com.example.CalculatorTest",
            "testMultiplication",
            "SKIP",
            50L
        ));

        // When
        HtmlReportGenerator.generate(project, testTask, collector);

        // Then
        File reportFile = project.getLayout().getBuildDirectory().file("unifiedtest/reports/index.html").get().getAsFile();
        assertTrue(reportFile.exists(), "Report file should be generated");

        String content = Files.readString(reportFile.toPath());
        
        // Verify basic structure
        assertTrue(content.contains("<!DOCTYPE html>"), "Should have DOCTYPE");
        assertTrue(content.contains("<title>UnifiedTest Report</title>"), "Should have title");
        
        // Verify test results
        assertTrue(content.contains("testAddition"), "Should contain passed test");
        assertTrue(content.contains("testDivision"), "Should contain failed test");
        assertTrue(content.contains("testMultiplication"), "Should contain skipped test");
        
        // Verify status badges
        assertTrue(content.contains("badge pass"), "Should have PASS status");
        assertTrue(content.contains("badge fail"), "Should have FAIL status");
        assertTrue(content.contains("badge skip"), "Should have SKIP status");
        
        // Verify rich report sections
        assertTrue(content.contains("Failure spotlight"), "Should include failure insight section");
        assertTrue(content.contains("Slowest tests"), "Should include duration insight section");
        assertTrue(content.contains("Suites"), "Should include suite breakdown");
        assertTrue(content.contains("Frameworks"), "Should include framework breakdown");
        assertTrue(content.contains("Evidence"), "Should include evidence summary");
        assertTrue(content.contains("run-meta"), "Should include service/run metadata");
        assertTrue(content.contains("duration-bar"), "Should include duration bars");
        
        // Verify stack trace is in the detail drawer
        assertTrue(content.contains("<details class=\"detail\">"), "Should have collapsible details");
        assertTrue(content.contains("Details and evidence"), "Should label the detail drawer");
        assertTrue(content.contains("UnifiedTest metadata"), "Should render native result metadata");
        assertTrue(content.contains("Division by zero"), "Should contain error message");
        assertTrue(content.contains("java.lang.ArithmeticException"), "Should contain stack trace");
        
        // Verify summary statistics
        assertTrue(content.contains("<strong>3</strong>"), "Should show total of 3 tests");
        assertTrue(content.contains("<strong>1</strong>"), "Should show 1 passed test");
        assertTrue(content.contains("33.3%"), "Should show correct percentage");
        assertTrue(content.contains("statusFilter"), "Should contain status filter");
        assertTrue(content.contains("frameworkFilter"), "Should contain framework filter");
    }

    @org.junit.jupiter.api.Test
    void shouldHandleEmptyResults() throws Exception {
        // When
        HtmlReportGenerator.generate(project, testTask, collector);

        // Then
        File reportFile = project.getLayout().getBuildDirectory().file("unifiedtest/reports/index.html").get().getAsFile();
        assertTrue(reportFile.exists(), "Report file should be generated even with no results");

        String content = Files.readString(reportFile.toPath());
        assertTrue(content.contains("<strong>0</strong>"), "Should show zero tests");
        assertTrue(content.contains("0.0%"), "Should show 0% for all categories");
    }

    @org.junit.jupiter.api.Test
    void shouldHandleLongStackTraces() throws Exception {
        // Given
        StringBuilder longStackTrace = new StringBuilder();
        for (int i = 0; i < 50; i++) {
            longStackTrace.append("    at Class").append(i).append(".method(Class").append(i).append(".java:").append(i).append(")\n");
        }

        collector.addResult(new UnifiedTestResult(
            "com.example.LongTest",
            "testWithLongStackTrace",
            "FAIL",
            "Test failed",
            longStackTrace.toString(),
            200L
        ));

        // When
        HtmlReportGenerator.generate(project, testTask, collector);

        // Then
        File reportFile = project.getLayout().getBuildDirectory().file("unifiedtest/reports/index.html").get().getAsFile();
        String content = Files.readString(reportFile.toPath());
        
        assertTrue(content.contains("max-height:300px"), "Should have scrollable stack trace");
        assertTrue(content.contains("overflow:auto"), "Should have overflow handling");
        assertTrue(content.contains("Class49"), "Should contain complete stack trace");
    }

    @org.junit.jupiter.api.Test
    void shouldRenderAllureLikeEvidenceAndScreenshots() throws Exception {
        // Given
        collector.addResult(new UnifiedTestResult(
            "com.example.CalculatorTest",
            "testDivision",
            "FAIL",
            "Division by zero",
            "java.lang.ArithmeticException: Division by zero",
            275L,
            "JUnit5"
        ));

        File allureResultsDir = project.getLayout()
            .getBuildDirectory()
            .dir("allure-results")
            .get()
            .getAsFile();
        assertTrue(allureResultsDir.mkdirs());
        Files.writeString(allureResultsDir.toPath().resolve("checkout.svg"), """
            <svg xmlns="http://www.w3.org/2000/svg" width="320" height="180">
              <rect width="320" height="180" fill="#111827"/>
              <text x="24" y="96" fill="#fff">Failure screenshot</text>
            </svg>
            """);
        Files.writeString(allureResultsDir.toPath().resolve("test-result.json"), """
            {
              "uuid": "uuid-division",
              "historyId": "history-division",
              "testCaseId": "case-division",
              "name": "testDivision",
              "fullName": "com.example.CalculatorTest.testDivision",
              "status": "failed",
              "stage": "finished",
              "description": "Division validation",
              "statusDetails": {
                "message": "Division by zero",
                "trace": "stack trace from allure",
                "flaky": true
              },
              "labels": [
                {"name": "owner", "value": "qa"},
                {"name": "severity", "value": "critical"},
                {"name": "feature", "value": "Calculator"},
                {"name": "story", "value": "Division"}
              ],
              "parameters": [
                {"name": "divisor", "value": "0"}
              ],
              "links": [
                {"name": "BUG-42", "url": "https://example.test/BUG-42", "type": "issue"}
              ],
              "steps": [
                {
                  "name": "Open calculator",
                  "status": "passed",
                  "start": 1000,
                  "stop": 1020
                }
              ],
              "attachments": [
                {
                  "source": "checkout.svg",
                  "name": "Failure screenshot",
                  "type": "image/svg+xml"
                }
              ],
              "start": 1000,
              "stop": 1275
            }
            """);

        // When
        HtmlReportGenerator.generate(project, testTask, collector);

        // Then
        File reportFile = project.getLayout().getBuildDirectory().file("unifiedtest/reports/index.html").get().getAsFile();
        String content = Files.readString(reportFile.toPath());

        assertTrue(content.contains("Allure 3 metadata"), "Should render Allure-style metadata");
        assertTrue(content.contains("Screenshot evidence"), "Should render screenshot section");
        assertTrue(content.contains("evidence-gallery"), "Should use a screenshot gallery");
        assertTrue(content.contains("img loading=\"lazy\""), "Should render screenshot previews");
        assertTrue(content.contains("checkout.svg"), "Should reference screenshot attachment");
        assertTrue(content.contains("Owner"), "Should render labels");
        assertTrue(content.contains("qa"), "Should render label values");
        assertTrue(content.contains("Parameters"), "Should render parameters");
        assertTrue(content.contains("Links"), "Should render links");
        assertTrue(content.contains("Open calculator"), "Should render steps");
        assertTrue(content.contains("flaky"), "Should render Allure status flags");
    }
} 
