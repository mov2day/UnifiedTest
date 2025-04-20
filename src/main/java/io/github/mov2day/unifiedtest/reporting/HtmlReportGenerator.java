package io.github.mov2day.unifiedtest.reporting;

import io.github.mov2day.unifiedtest.collector.UnifiedTestResult;
import org.gradle.api.Project;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

/**
 * Generates an HTML report for UnifiedTest results.
 */
public class HtmlReportGenerator {
    /**
     * Generates an HTML report file from the given test results.
     * @param project the Gradle project
     * @param results the list of test results
     */
    @SuppressWarnings("deprecation")
    public static void generate(Project project, List<UnifiedTestResult> results) {
        File reportFile = new File(project.getBuildDir(), "unifiedtest/report.html");
        reportFile.getParentFile().mkdirs();
        try (FileWriter writer = new FileWriter(reportFile)) {
            writer.write("<html><head><title>UnifiedTest Report</title></head><body><h1>UnifiedTest Report</h1><table border='1'><tr><th>Class</th><th>Name</th><th>Status</th></tr>");
            for (UnifiedTestResult r : results) {
                writer.write(String.format("<tr><td>%s</td><td>%s</td><td>%s</td></tr>", r.className, r.testName, r.status));
            }
            writer.write("</table></body></html>");
        } catch (IOException e) {
            project.getLogger().error("Failed to write UnifiedTest HTML report", e);
        }
    }
}
