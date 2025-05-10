package io.github.mov2day.unifiedtest.reporting;

import io.github.mov2day.unifiedtest.collector.UnifiedTestResult;
import io.github.mov2day.unifiedtest.collector.UnifiedTestResultCollector;
import org.gradle.api.Project;
import org.gradle.api.tasks.testing.Test;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

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
            writer.write(":root { --primary: #1976d2; --primary-light: #e3f2fd; --success: #2e7d32; --error: #d32f2f; --warning: #ed6c02; }\n");
            writer.write("* { box-sizing: border-box; margin: 0; padding: 0; }\n");
            writer.write("body { font-family: 'Roboto', system-ui, -apple-system, sans-serif; line-height: 1.5; color: #1f2937; background: #f5f5f5; }\n");
            writer.write(".container { max-width: 1200px; margin: 0 auto; padding: 2rem; }\n");
            writer.write("h1 { font-size: 2.25rem; font-weight: 400; color: var(--primary); margin-bottom: 1rem; letter-spacing: -0.5px; }\n");
            writer.write("h2 { font-size: 1.5rem; font-weight: 400; color: #374151; margin: 2rem 0 1rem; letter-spacing: -0.25px; }\n");
            writer.write(".card { background: white; border-radius: 0.75rem; box-shadow: 0 2px 4px rgba(0,0,0,0.05), 0 1px 2px rgba(0,0,0,0.1); margin-bottom: 1.5rem; padding: 1.5rem; transition: box-shadow 0.3s ease; }\n");
            writer.write(".card:hover { box-shadow: 0 4px 6px rgba(0,0,0,0.05), 0 2px 4px rgba(0,0,0,0.1); }\n");
            writer.write(".summary { display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap: 1rem; }\n");
            writer.write(".stat { padding: 1.5rem; border-radius: 0.75rem; text-align: center; cursor: pointer; transition: all 0.3s ease; position: relative; overflow: hidden; }\n");
            writer.write(".stat::before { content: ''; position: absolute; top: 0; left: 0; right: 0; bottom: 0; opacity: 0.1; transition: opacity 0.3s ease; }\n");
            writer.write(".stat:hover::before { opacity: 0.15; }\n");
            writer.write(".stat h3 { font-size: 0.875rem; font-weight: 500; text-transform: uppercase; letter-spacing: 0.05em; margin-bottom: 0.5rem; position: relative; }\n");
            writer.write(".stat p { font-size: 2.5rem; font-weight: 300; margin: 0.5rem 0; position: relative; }\n");
            writer.write(".stat small { font-size: 0.875rem; opacity: 0.8; position: relative; }\n");
            writer.write(".stat.total { background: var(--primary-light); color: var(--primary); }\n");
            writer.write(".stat.total::before { background: var(--primary); }\n");
            writer.write(".stat.passed { background: #f0fdf4; color: var(--success); }\n");
            writer.write(".stat.passed::before { background: var(--success); }\n");
            writer.write(".stat.failed { background: #fef2f2; color: var(--error); }\n");
            writer.write(".stat.failed::before { background: var(--error); }\n");
            writer.write(".stat.skipped { background: #fff7ed; color: var(--warning); }\n");
            writer.write(".stat.skipped::before { background: var(--warning); }\n");
            writer.write("table { width: 100%; border-collapse: separate; border-spacing: 0; margin: 1rem 0; background: white; border-radius: 0.75rem; overflow: hidden; box-shadow: 0 2px 4px rgba(0,0,0,0.05); }\n");
            writer.write("th { background: #f8fafc; padding: 1rem; text-align: left; font-weight: 500; color: #4b5563; font-size: 0.875rem; letter-spacing: 0.025em; border-bottom: 1px solid #e5e7eb; }\n");
            writer.write("td { padding: 1rem; border-bottom: 1px solid #e5e7eb; font-size: 0.875rem; max-width: 320px; word-break: break-all; white-space: pre-line; overflow-wrap: anywhere; text-overflow: ellipsis; overflow: hidden; }\n");
            writer.write("tr:last-child td { border-bottom: none; }\n");
            writer.write("tr:hover td { background: #f8fafc; }\n");
            writer.write(".status { font-weight: 500; padding: 0.375rem 1rem; border-radius: 9999px; display: inline-flex; align-items: center; gap: 0.375rem; font-size: 0.75rem; letter-spacing: 0.025em; }\n");
            writer.write(".status::before { content: ''; display: inline-block; width: 0.5rem; height: 0.5rem; border-radius: 50%; margin-right: 0.25rem; }\n");
            writer.write(".status.PASS { background: #f0fdf4; color: var(--success); }\n");
            writer.write(".status.PASS::before { background: var(--success); }\n");
            writer.write(".status.FAIL { background: #fef2f2; color: var(--error); }\n");
            writer.write(".status.FAIL::before { background: var(--error); }\n");
            writer.write(".status.SKIP { background: #fff7ed; color: var(--warning); }\n");
            writer.write(".status.SKIP::before { background: var(--warning); }\n");
            writer.write(".failure-details { margin: 1rem 0; padding: 1.25rem; border-radius: 0.75rem; background: #fef2f2; border: 1px solid rgba(220,38,38,0.1); }\n");
            writer.write(".stacktrace { background: #f8fafc; border: 1px solid #e5e7eb; padding: 1rem; margin-top: 0.75rem; max-height: 300px; overflow: auto; font-size: 0.875rem; font-family: ui-monospace, monospace; border-radius: 0.5rem; display: none; }\n");
            writer.write(".toggle-stack { margin: 0.75rem 0 0; padding: 0.5rem 1rem; font-size: 0.875rem; cursor: pointer; background: white; border: 1px solid #e5e7eb; color: var(--primary); border-radius: 0.5rem; transition: all 0.2s ease; }\n");
            writer.write(".toggle-stack:hover { background: #f8fafc; border-color: var(--primary); }\n");
            writer.write(".timestamp { color: #6b7280; font-size: 0.875rem; margin-bottom: 2rem; display: flex; align-items: center; gap: 0.5rem; }\n");
            writer.write(".timestamp::before { content: '🕒'; }\n");
            writer.write(".duration { color: #6b7280; font-size: 0.875rem; display: inline-flex; align-items: center; gap: 0.5rem; }\n");
            writer.write(".duration::before { content: '⏱'; }\n");
            writer.write(".allure-details { margin-top: 1rem; padding: 1.25rem; border-radius: 0.75rem; background: white; border: 1px solid #e5e7eb; box-shadow: 0 1px 3px rgba(0,0,0,0.05); }\n");
            writer.write(".allure-details h4 { font-size: 1rem; font-weight: 500; margin: 1rem 0 0.75rem; color: #374151; letter-spacing: -0.025em; }\n");
            writer.write(".allure-status { display: inline-flex; align-items: center; padding: 0.375rem 1rem; border-radius: 9999px; font-weight: 500; margin-right: 1rem; font-size: 0.875rem; gap: 0.375rem; }\n");
            writer.write(".allure-status.passed { background: #f0fdf4; color: var(--success); }\n");
            writer.write(".allure-status.passed::before { content: ''; display: inline-block; width: 0.5rem; height: 0.5rem; border-radius: 50%; background: var(--success); }\n");
            writer.write(".allure-status.failed { background: #fef2f2; color: var(--error); }\n");
            writer.write(".allure-status.failed::before { content: ''; display: inline-block; width: 0.5rem; height: 0.5rem; border-radius: 50%; background: var(--error); }\n");
            writer.write(".allure-duration { display: inline-flex; align-items: center; color: #6b7280; font-size: 0.875rem; gap: 0.375rem; }\n");
            writer.write(".allure-duration::before { content: '⏱'; }\n");
            writer.write(".allure-steps { margin-top: 0.75rem; display: flex; flex-direction: column; gap: 0.5rem; }\n");
            writer.write(".allure-step { padding: 1rem; border-radius: 0.5rem; background: #f8fafc; border: 1px solid #e5e7eb; display: flex; align-items: center; transition: all 0.2s ease; }\n");
            writer.write(".allure-step:hover { transform: translateY(-1px); box-shadow: 0 2px 4px rgba(0,0,0,0.05); }\n");
            writer.write(".allure-step .step-status { display: inline-flex; align-items: center; padding: 0.25rem 0.75rem; border-radius: 9999px; font-size: 0.75rem; font-weight: 500; margin-right: 1rem; gap: 0.25rem; }\n");
            writer.write(".allure-step .step-name { flex: 1; font-size: 0.875rem; }\n");
            writer.write(".allure-step.passed { border-left: 4px solid var(--success); }\n");
            writer.write(".allure-step.passed .step-status { background: #f0fdf4; color: var(--success); }\n");
            writer.write(".allure-step.passed .step-status::before { content: ''; display: inline-block; width: 0.375rem; height: 0.375rem; border-radius: 50%; background: var(--success); }\n");
            writer.write(".allure-step.failed { border-left: 4px solid var(--error); }\n");
            writer.write(".allure-step.failed .step-status { background: #fef2f2; color: var(--error); }\n");
            writer.write(".allure-step.failed .step-status::before { content: ''; display: inline-block; width: 0.375rem; height: 0.375rem; border-radius: 50%; background: var(--error); }\n");
            writer.write(".allure-attachments { display: grid; grid-template-columns: repeat(auto-fill, minmax(250px, 1fr)); gap: 1rem; margin-top: 0.75rem; }\n");
            writer.write(".allure-attachment { background: white; border: 1px solid #e5e7eb; border-radius: 0.75rem; overflow: hidden; transition: all 0.2s ease; }\n");
            writer.write(".allure-attachment:hover { transform: translateY(-2px); box-shadow: 0 4px 6px rgba(0,0,0,0.05); }\n");
            writer.write(".allure-attachment .attachment-name { padding: 0.75rem; font-size: 0.875rem; font-weight: 500; border-bottom: 1px solid #e5e7eb; background: #f8fafc; }\n");
            writer.write(".allure-attachment img { width: 100%; height: auto; max-height: 200px; object-fit: contain; padding: 1rem; }\n");
            writer.write(".allure-attachment a { display: flex; align-items: center; justify-content: center; padding: 1rem; text-align: center; color: var(--primary); text-decoration: none; gap: 0.5rem; font-size: 0.875rem; }\n");
            writer.write(".allure-attachment a::before { content: '📎'; }\n");
            writer.write(".allure-attachment a:hover { background: #f8fafc; }\n");
            writer.write(".allure-link { display: inline-flex; align-items: center; margin-top: 1rem; padding: 0.75rem 1.25rem; background: var(--primary); color: white; text-decoration: none; border-radius: 0.5rem; font-size: 0.875rem; font-weight: 500; gap: 0.5rem; transition: all 0.2s ease; }\n");
            writer.write(".allure-link::before { content: '📊'; }\n");
            writer.write(".allure-link:hover { background: #1565c0; transform: translateY(-1px); }\n");
            writer.write(".status-summary { display: flex; gap: 1rem; color: #6b7280; font-size: 0.875rem; }\n");
            writer.write(".status-summary .step-count::before { content: '📋'; margin-right: 0.25rem; }\n");
            writer.write(".status-summary .attachment-count::before { content: '📎'; margin-right: 0.25rem; }\n");
            writer.write("</style>\n");
            writer.write("<link href='https://fonts.googleapis.com/css2?family=Roboto:wght@300;400;500&display=swap' rel='stylesheet'>\n");
            writer.write("</head>\n<body>\n");
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
            writer.write("<div class='stat total' onclick='filterTests(\"all\")'>\n");
            writer.write("<h3>Total Tests</h3>\n");
            writer.write(String.format("<p>%d</p>\n", total));
            writer.write(String.format("<small>Duration: %s</small>\n", formatDuration(totalDuration)));
            writer.write("</div>\n");
            writer.write("<div class='stat passed' onclick='filterTests(\"PASS\")'>\n");
            writer.write("<h3>Passed</h3>\n");
            writer.write(String.format("<p>%d</p>\n", passed));
            writer.write(String.format("<small>%.1f%%</small>\n", total > 0 ? (passed * 100.0 / total) : 0));
            writer.write("</div>\n");
            writer.write("<div class='stat failed' onclick='filterTests(\"FAIL\")'>\n");
            writer.write("<h3>Failed</h3>\n");
            writer.write(String.format("<p>%d</p>\n", failed));
            writer.write(String.format("<small>%.1f%%</small>\n", total > 0 ? (failed * 100.0 / total) : 0));
            writer.write("</div>\n");
            writer.write("<div class='stat skipped' onclick='filterTests(\"SKIP\")'>\n");
            writer.write("<h3>Skipped</h3>\n");
            writer.write(String.format("<p>%d</p>\n", skipped));
            writer.write(String.format("<small>%.1f%%</small>\n", total > 0 ? (skipped * 100.0 / total) : 0));
            writer.write("</div>\n");
            writer.write("</div>\n");
            writer.write("</div>\n");

            // Check for Allure reports
            AllureReportReader allureReader = new AllureReportReader(project);
            boolean hasAllureReports = allureReader.hasAllureReports();
            Map<String, AllureReportReader.AllureTestResult> allureResults = hasAllureReports ? 
                allureReader.readAllureResults() : Collections.emptyMap();

            // Add Allure report link if available
            if (hasAllureReports) {
                String allureReportPath = allureReader.getAllureReportPath();
                if (allureReportPath != null) {
                    writer.write("<div class='card'>\n");
                    writer.write("<h2>Allure Report</h2>\n");
                    writer.write("<p>Detailed test reports with screenshots, steps, and environment information are available in the Allure report.</p>\n");
                    writer.write(String.format("<a href='file://%s/index.html' class='allure-link' target='_blank'>View Full Allure Report</a>\n", allureReportPath));
                    writer.write("</div>\n");
                }
            }

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
                
                // For failed tests, show detailed information
                if ("FAIL".equals(r.status)) {
                    writer.write("    <div class='failure-details'>\n");
                    if (r.failureMessage != null) {
                        writer.write(String.format("      <strong>Message:</strong> %s\n", r.failureMessage.replace("<", "&lt;").replace(">", "&gt;")));
                    }
                    if (r.stackTrace != null) {
                        writer.write("      <button class='toggle-stack' onclick='toggleStack(this)'>Show Stack Trace</button>\n");
                        writer.write(String.format("      <pre class='stacktrace'>%s</pre>\n", r.stackTrace.replace("<", "&lt;").replace(">", "&gt;")));
                    }
                    writer.write("    </div>\n");

                    // Add Allure details if available
                    String testKey = r.className + "." + r.testName;
                    String simpleTestKey = r.testName;
                    AllureReportReader.AllureTestResult allureResult = allureResults.get(testKey);
                    if (allureResult == null) {
                        allureResult = allureResults.get(simpleTestKey);
                    }
                    
                    if (allureResult != null) {
                        writer.write("    <div class='allure-details'>\n");
                        writer.write("      <h4>Test Execution Details</h4>\n");
                        
                        // Add test status and duration
                        writer.write(String.format("      <div class='allure-status %s'>Status: %s</div>\n",
                            allureResult.getStatus().toLowerCase(), allureResult.getStatus()));
                        writer.write(String.format("      <div class='allure-duration'>Duration: %s</div>\n",
                            formatDuration(allureResult.getDuration())));
                        
                        // Add steps
                        if (!allureResult.getSteps().isEmpty()) {
                            writer.write("      <h4>Test Steps:</h4>\n");
                            writer.write("      <div class='allure-steps'>\n");
                            for (AllureReportReader.AllureTestResult.Step step : allureResult.getSteps()) {
                                writer.write(String.format("        <div class='allure-step %s'>\n", 
                                    step.getStatus().toLowerCase()));
                                writer.write(String.format("          <span class='step-status'>%s</span>\n",
                                    step.getStatus()));
                                writer.write(String.format("          <span class='step-name'>%s</span>\n",
                                    step.getName()));
                                writer.write("        </div>\n");
                            }
                            writer.write("      </div>\n");
                        }
                        
                        // Add attachments
                        if (!allureResult.getAttachments().isEmpty()) {
                            writer.write("      <h4>Evidence:</h4>\n");
                            writer.write("      <div class='allure-attachments'>\n");
                            for (AllureReportReader.AllureTestResult.Attachment attachment : allureResult.getAttachments()) {
                                if (attachment.getType().startsWith("image/")) {
                                    writer.write(String.format("        <div class='allure-attachment'>\n"));
                                    writer.write(String.format("          <div class='attachment-name'>%s</div>\n",
                                        attachment.getName()));
                                    writer.write(String.format("          <img src='file://%s' alt='%s'>\n", 
                                        attachment.getSource(), attachment.getName()));
                                    writer.write("        </div>\n");
                                } else {
                                    writer.write(String.format("        <div class='allure-attachment'>\n"));
                                    writer.write(String.format("          <div class='attachment-name'>%s</div>\n",
                                        attachment.getName()));
                                    writer.write(String.format("          <a href='file://%s' target='_blank'>View Attachment</a>\n", 
                                        attachment.getSource()));
                                    writer.write("        </div>\n");
                                }
                            }
                            writer.write("      </div>\n");
                        }
                        
                        writer.write("    </div>\n");
                    }
                } else {
                    // For passed and skipped tests, show minimal information
                    String testKey = r.className + "." + r.testName;
                    String simpleTestKey = r.testName;
                    AllureReportReader.AllureTestResult allureResult = allureResults.get(testKey);
                    if (allureResult == null) {
                        allureResult = allureResults.get(simpleTestKey);
                    }
                    
                    if (allureResult != null) {
                        writer.write(String.format("    <div class='status-summary %s'>\n", r.status.toLowerCase()));
                        writer.write(String.format("      <span class='step-count'>%d steps</span>\n", 
                            allureResult.getSteps().size()));
                        if (!allureResult.getAttachments().isEmpty()) {
                            writer.write(String.format("      <span class='attachment-count'>%d attachments</span>\n",
                                allureResult.getAttachments().size()));
                        }
                        writer.write("    </div>\n");
                    }
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
                "}\n\n" +
                "function filterTests(status) {\n" +
                "  const rows = document.querySelectorAll('table tr:not(:first-child)');\n" +
                "  rows.forEach(row => {\n" +
                "    const statusCell = row.querySelector('.status');\n" +
                "    if (status === 'all' || (statusCell && statusCell.textContent === status)) {\n" +
                "      row.style.display = '';\n" +
                "    } else {\n" +
                "      row.style.display = 'none';\n" +
                "    }\n" +
                "  });\n" +
                "  // Update active state on summary stats\n" +
                "  document.querySelectorAll('.stat').forEach(stat => {\n" +
                "    if ((status === 'all' && stat.classList.contains('total')) ||\n" +
                "        (stat.classList.contains(status.toLowerCase()))) {\n" +
                "      stat.style.opacity = '1';\n" +
                "    } else {\n" +
                "      stat.style.opacity = '0.7';\n" +
                "    }\n" +
                "  });\n" +
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
