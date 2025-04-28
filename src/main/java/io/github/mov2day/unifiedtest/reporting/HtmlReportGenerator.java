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
    public static void generate(Project project, Test testTask, UnifiedTestResultCollector collector) {
        File reportFile = new File(project.getBuildDir(), "unifiedtest/reports/index.html");
        reportFile.getParentFile().mkdirs();
        
        try (FileWriter writer = new FileWriter(reportFile)) {
            writer.write("<!DOCTYPE html>\n<html>\n<head>\n");
            writer.write("<meta charset='UTF-8'>\n");
            writer.write("<meta name='viewport' content='width=device-width, initial-scale=1.0'>\n");
            writer.write("<title>UnifiedTest Report</title>\n");
            writer.write("<style>\n");
            writer.write(":root { --primary: #2563eb; --success: #16a34a; --error: #dc2626; --warning: #ca8a04; }\n");
            writer.write("* { box-sizing: border-box; margin: 0; padding: 0; }\n");
            writer.write("body { font-family: system-ui, -apple-system, sans-serif; line-height: 1.5; color: #1f2937; background: #f9fafb; }\n");
            writer.write(".container { max-width: 1200px; margin: 0 auto; padding: 2rem; }\n");
            writer.write("h1 { font-size: 2.25rem; font-weight: 700; color: var(--primary); margin-bottom: 1rem; }\n");
            writer.write("h2 { font-size: 1.5rem; font-weight: 600; color: #374151; margin: 2rem 0 1rem; }\n");
            writer.write(".card { background: white; border-radius: 0.5rem; box-shadow: 0 1px 3px rgba(0,0,0,0.1); margin-bottom: 1.5rem; padding: 1.5rem; }\n");
            writer.write(".summary { display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap: 1rem; }\n");
            writer.write(".stat { padding: 1.5rem; border-radius: 0.375rem; text-align: center; }\n");
            writer.write(".stat h3 { font-size: 0.875rem; font-weight: 500; text-transform: uppercase; letter-spacing: 0.05em; margin-bottom: 0.5rem; }\n");
            writer.write(".stat p { font-size: 2rem; font-weight: 600; margin: 0.5rem 0; }\n");
            writer.write(".stat small { font-size: 0.875rem; opacity: 0.8; }\n");
            writer.write(".stat.total { background: #e0e7ff; color: var(--primary); }\n");
            writer.write(".stat.passed { background: #dcfce7; color: var(--success); }\n");
            writer.write(".stat.failed { background: #fee2e2; color: var(--error); }\n");
            writer.write(".stat.skipped { background: #fef3c7; color: var(--warning); }\n");
            writer.write("table { width: 100%; border-collapse: collapse; margin: 1rem 0; background: white; }\n");
            writer.write("th { background: #f3f4f6; padding: 0.75rem; text-align: left; font-weight: 600; color: #4b5563; }\n");
            writer.write("td { padding: 0.75rem; border-bottom: 1px solid #e5e7eb; max-width: 320px; word-break: break-all; white-space: pre-line; overflow-wrap: anywhere; text-overflow: ellipsis; overflow: hidden; }\n");
            writer.write("tr:hover { background: #f9fafb; }\n");
            writer.write(".status { font-weight: 500; padding: 0.25rem 0.75rem; border-radius: 9999px; display: inline-block; }\n");
            writer.write(".status.PASS { background: #dcfce7; color: var(--success); }\n");
            writer.write(".status.FAIL { background: #fee2e2; color: var(--error); }\n");
            writer.write(".status.SKIP { background: #fef3c7; color: var(--warning); }\n");
            writer.write(".failure-details { margin: 1rem 0; padding: 1rem; border-radius: 0.375rem; background: #fff1f2; border: 1px solid #fecdd3; }\n");
            writer.write(".stacktrace { background: #f8f8f8; border: 1px solid #eee; padding: 0.5em; margin-top: 0.5em; max-height: 300px; overflow: auto; font-size: 0.95em; display: none; }");
            writer.write(".toggle-stack { margin-left: 1em; font-size: 0.9em; cursor: pointer; background: none; border: none; color: #0074d9; text-decoration: underline; }");
            writer.write(".timestamp { color: #6b7280; font-size: 0.875rem; margin-bottom: 2rem; }\n");
            writer.write(".duration { color: #6b7280; font-size: 0.875rem; margin-left: 1rem; }\n");
            writer.write("</style>\n</head>\n<body>\n");
            writer.write("<div class='container'>\n");
            
            // Header with timestamp
            writer.write("<h1>UnifiedTest Report</h1>\n");
            writer.write(String.format("<div class='timestamp'>Generated on %s</div>\n", 
                java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("MMMM d, yyyy HH:mm:ss"))));

            List<UnifiedTestResult> results = collector.getResults();
            int total = results.size();
            long passed = results.stream().filter(r -> "PASS".equals(r.status)).count();
            long failed = results.stream().filter(r -> "FAIL".equals(r.status)).count();
            long skipped = results.stream().filter(r -> "SKIP".equals(r.status)).count();
            long totalDuration = results.stream().mapToLong(r -> r.duration).sum();

            // Summary statistics
            writer.write("<div class='card'>\n");
            writer.write("<div class='summary'>\n");
            writer.write("<div class='stat total'>\n");
            writer.write("<h3>Total Tests</h3>\n");
            writer.write(String.format("<p>%d</p>\n", total));
            writer.write(String.format("<small>Duration: %s</small>\n", formatDuration(totalDuration)));
            writer.write("</div>\n");
            writer.write("<div class='stat passed'>\n");
            writer.write("<h3>Passed</h3>\n");
            writer.write(String.format("<p>%d</p>\n", passed));
            writer.write(String.format("<small>%.1f%%</small>\n", total > 0 ? (passed * 100.0 / total) : 0));
            writer.write("</div>\n");
            writer.write("<div class='stat failed'>\n");
            writer.write("<h3>Failed</h3>\n");
            writer.write(String.format("<p>%d</p>\n", failed));
            writer.write(String.format("<small>%.1f%%</small>\n", total > 0 ? (failed * 100.0 / total) : 0));
            writer.write("</div>\n");
            writer.write("<div class='stat skipped'>\n");
            writer.write("<h3>Skipped</h3>\n");
            writer.write(String.format("<p>%d</p>\n", skipped));
            writer.write(String.format("<small>%.1f%%</small>\n", total > 0 ? (skipped * 100.0 / total) : 0));
            writer.write("</div>\n");
            writer.write("</div>\n");
            writer.write("</div>\n");

            // Test details table
            writer.write("<div class='card'>\n");
            writer.write("<h2>Test Details</h2>\n");
            writer.write("<table>\n");
            writer.write("<tr><th>Class</th><th>Test</th><th>Status</th><th>Duration</th><th>Details</th></tr>\n");

            for (UnifiedTestResult r : results) {
                writer.write("<tr>\n");
                writer.write(String.format("  <td>%s</td>\n", r.className));
                writer.write(String.format("  <td>%s</td>\n", r.testName));
                writer.write(String.format("  <td><span class='status %s'>%s</span></td>\n", r.status, r.status));
                writer.write(String.format("  <td><span class='duration'>%s</span></td>\n", formatDuration(r.duration)));
                writer.write("  <td>\n");
                
                if ("FAIL".equals(r.status) && (r.failureMessage != null || r.stackTrace != null)) {
                    writer.write("    <div class='failure-details'>\n");
                    if (r.failureMessage != null) {
                        writer.write(String.format("      <strong>Message:</strong> %s\n", r.failureMessage.replace("<", "&lt;").replace(">", "&gt;")));
                    }
                    if (r.stackTrace != null) {
                        writer.write("      <button class='toggle-stack' onclick='toggleStack(this)'>Show Stack Trace</button>\n");
                        writer.write(String.format("      <pre class='stacktrace'>%s</pre>\n", r.stackTrace.replace("<", "&lt;").replace(">", "&gt;")));
                    }
                    writer.write("    </div>\n");
                }
                writer.write("  </td>\n</tr>\n");
            }
            
            writer.write("</table>\n");
            writer.write("</div>\n"); // card end
            writer.write("</div>\n"); // container end
            writer.write("</body>\n");
            writer.write("<script>\n" +
                "function toggleStack(btn) {\n" +
                "  var pre = btn.nextElementSibling;\n" +
                "  if (pre.style.display === 'none' || pre.style.display === '') {\n" +
                "    pre.style.display = 'block';\n" +
                "    btn.textContent = 'Hide Stack Trace';\n" +
                "  } else {\n" +
                "    pre.style.display = 'none';\n" +
                "    btn.textContent = 'Show Stack Trace';\n" +
                "  }\n" +
                "}\n" +
                "</script>\n");
            writer.write("</html>");
        } catch (IOException e) {
            project.getLogger().error("Failed to write UnifiedTest HTML report", e);
        }
    }

    private static String formatDuration(long millis) {
        if (millis < 1000) {
            return millis + "ms";
        }
        double seconds = millis / 1000.0;
        if (seconds < 60) {
            return String.format("%.2fs", seconds);
        }
        long minutes = millis / (60 * 1000);
        seconds = (millis % (60 * 1000)) / 1000.0;
        return String.format("%dm %.2fs", minutes, seconds);
    }
}
