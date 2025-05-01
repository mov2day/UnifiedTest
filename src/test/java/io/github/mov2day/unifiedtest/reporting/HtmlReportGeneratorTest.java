package io.github.mov2day.unifiedtest.reporting;

import io.github.mov2day.unifiedtest.collector.UnifiedTestResult;
import io.github.mov2day.unifiedtest.collector.UnifiedTestResultCollector;
import org.gradle.api.Project;
import org.gradle.api.tasks.testing.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class HtmlReportGeneratorTest {
    @Mock
    private Project project;

    @Mock
    private org.gradle.api.tasks.testing.Test testTask;

    @TempDir
    Path tempDir;

    private UnifiedTestResultCollector collector;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        collector = new UnifiedTestResultCollector();
        
        // Mock build directory
        when(project.getBuildDir()).thenReturn(tempDir.toFile());
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
        File reportFile = new File(tempDir.toFile(), "unifiedtest/reports/index.html");
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
        assertTrue(content.contains("class='status PASS'"), "Should have PASS status");
        assertTrue(content.contains("class='status FAIL'"), "Should have FAIL status");
        assertTrue(content.contains("class='status SKIP'"), "Should have SKIP status");
        
        // Verify stack trace is collapsible
        assertTrue(content.contains("toggle-stack"), "Should have toggle button for stack trace");
        assertTrue(content.contains("Division by zero"), "Should contain error message");
        assertTrue(content.contains("java.lang.ArithmeticException"), "Should contain stack trace");
        
        // Verify summary statistics
        assertTrue(content.contains("<p>3</p>"), "Should show total of 3 tests");
        assertTrue(content.contains("<p>1</p>"), "Should show 1 passed test");
        assertTrue(content.contains("33.3%"), "Should show correct percentage");
    }

    @org.junit.jupiter.api.Test
    void shouldHandleEmptyResults() throws Exception {
        // When
        HtmlReportGenerator.generate(project, testTask, collector);

        // Then
        File reportFile = new File(tempDir.toFile(), "unifiedtest/reports/index.html");
        assertTrue(reportFile.exists(), "Report file should be generated even with no results");

        String content = Files.readString(reportFile.toPath());
        assertTrue(content.contains("<p>0</p>"), "Should show zero tests");
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
        File reportFile = new File(tempDir.toFile(), "unifiedtest/reports/index.html");
        String content = Files.readString(reportFile.toPath());
        
        assertTrue(content.contains("max-height: 300px"), "Should have scrollable stack trace");
        assertTrue(content.contains("overflow: auto"), "Should have overflow handling");
        assertTrue(content.contains("Class49"), "Should contain complete stack trace");
    }
} 