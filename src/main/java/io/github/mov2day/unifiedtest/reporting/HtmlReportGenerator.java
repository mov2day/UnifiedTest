package io.github.mov2day.unifiedtest.reporting;

import io.github.mov2day.unifiedtest.collector.UnifiedTestResult;
import io.github.mov2day.unifiedtest.collector.UnifiedTestResultCollector;
import org.gradle.api.Project;
import org.gradle.api.tasks.testing.Test;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

/**
 * Generates HTML test reports from UnifiedTest results.
 * Creates visually appealing and interactive HTML reports with test execution details.
 */
public class HtmlReportGenerator {
    /**
     * Generates an HTML report from the collected test results.
     * @param project the Gradle project
     * @param testTask the test task
     * @param collector collector containing test results
     */
    public static void generate(Project project, Test testTask, UnifiedTestResultCollector collector) {
        File reportFile = new File(project.getLayout().getBuildDirectory().get().getAsFile(), "unifiedtest/report.html");
        reportFile.getParentFile().mkdirs();
        try (FileWriter writer = new FileWriter(reportFile)) {
            writer.write("<html><head><title>UnifiedTest Report</title></head><body><h1>UnifiedTest Report</h1><table border='1'><tr><th>Class</th><th>Name</th><th>Status</th></tr>");
            List<UnifiedTestResult> results = collector.getResults();
            for (UnifiedTestResult r : results) {
                writer.write(String.format("<tr><td>%s</td><td>%s</td><td>%s</td></tr>", r.className, r.testName, r.status));
            }
            writer.write("</table></body></html>");
        } catch (IOException e) {
            project.getLogger().error("Failed to write UnifiedTest HTML report", e);
        }
    }
}
