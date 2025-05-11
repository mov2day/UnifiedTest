package io.github.mov2day.unifiedtest.reporting;

import io.github.mov2day.unifiedtest.collector.UnifiedTestResult;
import io.github.mov2day.unifiedtest.collector.UnifiedTestResultCollector;
import org.gradle.api.Project;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ReportGenerationTest {
    private Project project;
    private org.gradle.api.tasks.testing.Test testTask;
    private UnifiedTestResultCollector collector;
    
    @BeforeEach
    void setUp() {
        project = ProjectBuilder.builder().build();
        testTask = project.getTasks().create("test", org.gradle.api.tasks.testing.Test.class);
        collector = new UnifiedTestResultCollector();
        
        // Add sample test results
        collector.addResult(new UnifiedTestResult(
            "com.example.CalculatorTest",
            "testAddition",
            "PASS",
            null,
            null,
            150L
        ));
        
        collector.addResult(new UnifiedTestResult(
            "com.example.CalculatorTest",
            "testDivision",
            "FAIL",
            "Expected 2.5 but got 2.0",
            "java.lang.AssertionError: Expected 2.5 but got 2.0\n    at com.example.CalculatorTest.testDivision(CalculatorTest.java:25)",
            200L
        ));
        
        collector.addResult(new UnifiedTestResult(
            "com.example.CalculatorTest",
            "testSkipped",
            "SKIP",
            "Test ignored",
            null,
            0L
        ));
    }
    
    @Test
    void testHtmlReportGeneration() {
        // Generate the report
        HtmlReportGenerator.generate(project, testTask, collector);
        
        // Verify the report file exists
        File reportFile = new File(project.getBuildDir(), "unifiedtest/reports/index.html");
        assertTrue(reportFile.exists(), "HTML report should be generated");
        
        // Print the report location for manual inspection
        System.out.println("Report generated at: " + reportFile.getAbsolutePath());
    }
} 