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
            writer.write("<!DOCTYPE html>\n<html>\n<head>\n<title>UnifiedTest Report</title>\n");
            writer.write("<style>\n");
            writer.write("body { font-family: Arial, sans-serif; margin: 20px; }\n");
            writer.write(".PASS { color: #22863a; background: #f0fff4; }\n");
            writer.write(".FAIL { color: #cb2431; background: #ffeef0; }\n");
            writer.write(".SKIP { color: #b08800; background: #fffbdd; }\n");
            writer.write(".failure-details { margin: 10px 0; padding: 10px; border-left: 4px solid #cb2431; background: #ffeef0; }\n");
            writer.write(".stack-trace { font-family: monospace; white-space: pre-wrap; margin: 10px 0; padding: 10px; background: #f6f8fa; }\n");
            writer.write("</style>\n</head>\n<body>\n");
            writer.write("<h1>UnifiedTest Report</h1>\n");
            writer.write("<table border='1' style='border-collapse: collapse; width: 100%;'>\n");
            writer.write("<tr><th>Class</th><th>Test</th><th>Status</th><th>Details</th></tr>\n");

            List<UnifiedTestResult> results = collector.getResults();
            for (UnifiedTestResult r : results) {
                writer.write(String.format("<tr class='%s'>\n", r.status));
                writer.write(String.format("  <td>%s</td>\n", r.className));
                writer.write(String.format("  <td>%s</td>\n", r.testName));
                writer.write(String.format("  <td>%s</td>\n", r.status));
                writer.write("  <td>\n");
                
                if ("FAIL".equals(r.status) && (r.failureMessage != null || r.stackTrace != null)) {
                    writer.write("    <div class='failure-details'>\n");
                    if (r.failureMessage != null) {
                        writer.write(String.format("      <strong>Message:</strong> %s<br>\n", 
                            r.failureMessage.replace("<", "&lt;").replace(">", "&gt;")));
                    }
                    if (r.stackTrace != null) {
                        writer.write("      <strong>Stack Trace:</strong>\n");
                        writer.write("      <div class='stack-trace'>");
                        writer.write(r.stackTrace.replace("<", "&lt;").replace(">", "&gt;"));
                        writer.write("</div>\n");
                    }
                    writer.write("    </div>\n");
                }
                writer.write("  </td>\n</tr>\n");
            }
            
            writer.write("</table>\n");
            
            // Add summary section
            int total = results.size();
            long passed = results.stream().filter(r -> "PASS".equals(r.status)).count();
            long failed = results.stream().filter(r -> "FAIL".equals(r.status)).count();
            long skipped = results.stream().filter(r -> "SKIP".equals(r.status)).count();
            
            writer.write("<h2>Summary</h2>\n");
            writer.write("<ul>\n");
            writer.write(String.format("<li class='PASS'>Passed: %d (%.1f%%)</li>\n", passed, (passed * 100.0 / total)));
            writer.write(String.format("<li class='FAIL'>Failed: %d (%.1f%%)</li>\n", failed, (failed * 100.0 / total)));
            writer.write(String.format("<li class='SKIP'>Skipped: %d (%.1f%%)</li>\n", skipped, (skipped * 100.0 / total)));
            writer.write(String.format("<li>Total: %d</li>\n", total));
            writer.write("</ul>\n");
            
            writer.write("</body>\n</html>");
        } catch (IOException e) {
            project.getLogger().error("Failed to write UnifiedTest HTML report", e);
        }
    }
}
