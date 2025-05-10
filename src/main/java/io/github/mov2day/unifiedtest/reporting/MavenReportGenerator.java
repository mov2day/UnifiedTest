package io.github.mov2day.unifiedtest.reporting;

import io.github.mov2day.unifiedtest.collector.ITestResultCollector;
import io.github.mov2day.unifiedtest.collector.UnifiedTestResult;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

/**
 * Generates HTML and JSON reports for Maven projects.
 * This class adapts the report generation functionality to work without Gradle dependencies.
 */
public class MavenReportGenerator {
    
    /**
     * Generates reports after test execution completes in Maven.
     * @param collector the test result collector with test results
     * @param targetDir the target directory for reports (typically target/unifiedtest)
     * @param generateJson whether to generate JSON reports
     * @param generateHtml whether to generate HTML reports
     */
    public static void generateReports(
            ITestResultCollector collector, 
            String targetDir, 
            boolean generateJson, 
            boolean generateHtml) {
        
        File reportsDir = new File(targetDir, "reports");
        reportsDir.mkdirs();
        
        if (generateJson) {
            generateJsonReport(collector, reportsDir);
        }
        
        if (generateHtml) {
            generateHtmlReport(collector, reportsDir);
        }
    }
    
    /**
     * Generates a JSON report with test results.
     */
    private static void generateJsonReport(ITestResultCollector collector, File reportsDir) {
        File reportFile = new File(reportsDir, "results.json");
        
        try (FileWriter writer = new FileWriter(reportFile)) {
            writer.write("[\n");
            List<UnifiedTestResult> results = collector.getResults();
            for (int i = 0; i < results.size(); i++) {
                UnifiedTestResult r = results.get(i);
                writer.write(String.format("  {\"class\":\"%s\", \"name\":\"%s\", \"status\":\"%s\"%s%s}%s\n",
                    r.className,
                    r.testName,
                    r.status,
                    r.failureMessage != null ? String.format(", \"failureMessage\":\"%s\"", 
                        r.failureMessage.replace("\"", "\\\"").replace("\n", "\\n")) : "",
                    r.stackTrace != null ? String.format(", \"stackTrace\":\"%s\"", 
                        r.stackTrace.replace("\"", "\\\"").replace("\n", "\\n")) : "",
                    (i < results.size() - 1 ? "," : "")
                ));
            }
            writer.write("]");
            System.out.println("UnifiedTest JSON report generated at: " + reportFile.getAbsolutePath());
        } catch (IOException e) {
            System.err.println("Failed to write UnifiedTest JSON report: " + e.getMessage());
        }
    }
    
    /**
     * Generates an HTML report with test results.
     */
    private static void generateHtmlReport(ITestResultCollector collector, File reportsDir) {
        File reportFile = new File(reportsDir, "index.html");
        
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
            writer.write(".total::before { background: var(--primary); }\n");
            writer.write(".passed::before { background: var(--success); }\n");
            writer.write(".failed::before { background: var(--error); }\n");
            writer.write(".skipped::before { background: var(--warning); }\n");
            writer.write(".total { color: var(--primary); }\n");
            writer.write(".passed { color: var(--success); }\n");
            writer.write(".failed { color: var(--error); }\n");
            writer.write(".skipped { color: var(--warning); }\n");
            writer.write(".stat h3 { font-size: 1.25rem; font-weight: 500; margin-bottom: 0.5rem; }\n");
            writer.write(".stat p { font-size: 2rem; font-weight: 600; margin-bottom: 0.25rem; }\n");
            writer.write(".stat small { font-size: 0.875rem; opacity: 0.8; }\n");
            writer.write("table { width: 100%; border-collapse: collapse; margin-top: 1rem; font-size: 0.9rem; }\n");
            writer.write("th { text-align: left; padding: 0.75rem; border-bottom: 2px solid #e5e7eb; color: #6b7280; font-weight: 500; }\n");
            writer.write("td { padding: 0.75rem; border-bottom: 1px solid #e5e7eb; vertical-align: top; }\n");
            writer.write("tr:hover { background-color: #f9fafb; }\n");
            writer.write(".status { display: inline-block; padding: 0.25rem 0.5rem; border-radius: 0.25rem; font-weight: 500; }\n");
            writer.write(".PASS { background: rgba(46, 125, 50, 0.1); color: var(--success); }\n");
            writer.write(".FAIL { background: rgba(211, 47, 47, 0.1); color: var(--error); }\n");
            writer.write(".SKIP { background: rgba(237, 108, 2, 0.1); color: var(--warning); }\n");
            writer.write(".duration { color: #6b7280; font-size: 0.875rem; }\n");
            writer.write(".failure-details { margin-top: 0.5rem; }\n");
            writer.write(".toggle-stack { background: #f3f4f6; border: 1px solid #d1d5db; border-radius: 0.25rem; padding: 0.25rem 0.5rem; cursor: pointer; font-size: 0.875rem; margin: 0.5rem 0; }\n");
            writer.write(".toggle-stack:hover { background: #e5e7eb; }\n");
            writer.write(".stacktrace { display: none; margin-top: 0.5rem; padding: 0.75rem; background: #f9fafb; border-radius: 0.25rem; white-space: pre-wrap; font-size: 0.75rem; overflow-x: auto; color: #4b5563; }\n");
            writer.write(".allure-link { display: inline-block; padding: 0.5rem 1rem; background: var(--primary); color: white; text-decoration: none; border-radius: 0.25rem; margin-top: 0.5rem; }\n");
            writer.write(".allure-link:hover { background: #1565c0; }\n");
            writer.write(".filter-bar { margin-bottom: 1rem; display: flex; gap: 0.5rem; }\n");
            writer.write(".filter-btn { padding: 0.5rem 1rem; border: 1px solid #d1d5db; background: white; border-radius: 0.25rem; cursor: pointer; }\n");
            writer.write(".filter-btn.active { background: var(--primary-light); border-color: var(--primary); color: var(--primary); }\n");
            writer.write(".no-results { padding: 2rem; text-align: center; color: #6b7280; }\n");
            writer.write("</style>\n");
            writer.write("</head>\n");
            writer.write("<body>\n");
            writer.write("<div class='container'>\n");
            writer.write("<h1>UnifiedTest Report</h1>\n");
            
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

            // Filter buttons
            writer.write("<div class='filter-bar'>\n");
            writer.write("<button class='filter-btn active' onclick='filterTests(\"all\")'>All Tests</button>\n");
            writer.write("<button class='filter-btn' onclick='filterTests(\"PASS\")'>Passed</button>\n");
            writer.write("<button class='filter-btn' onclick='filterTests(\"FAIL\")'>Failed</button>\n");
            writer.write("<button class='filter-btn' onclick='filterTests(\"SKIP\")'>Skipped</button>\n");
            writer.write("</div>\n");

            // Test details table
            writer.write("<div class='card'>\n");
            writer.write("<h2>Test Details</h2>\n");
            writer.write("<table id='test-table'>\n");
            writer.write("<tr><th>Class</th><th>Test</th><th>Status</th><th>Duration</th><th>Details</th></tr>\n");

            for (UnifiedTestResult r : results) {
                writer.write(String.format("<tr class='test-row %s'>\n", r.status));
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
                }
                
                writer.write("  </td>\n</tr>\n");
            }
            
            writer.write("</table>\n");
            writer.write("<div id='no-results' class='no-results' style='display:none;'>No matching tests found.</div>\n");
            writer.write("</div>\n"); // card end
            
            // JavaScript for interactivity
            writer.write("<script>\n");
            writer.write("function toggleStack(btn) {\n");
            writer.write("  const stack = btn.nextElementSibling;\n");
            writer.write("  if (stack.style.display === 'block') {\n");
            writer.write("    stack.style.display = 'none';\n");
            writer.write("    btn.textContent = 'Show Stack Trace';\n");
            writer.write("  } else {\n");
            writer.write("    stack.style.display = 'block';\n");
            writer.write("    btn.textContent = 'Hide Stack Trace';\n");
            writer.write("  }\n");
            writer.write("}\n");
            
            writer.write("function filterTests(status) {\n");
            writer.write("  const rows = document.querySelectorAll('.test-row');\n");
            writer.write("  const filterBtns = document.querySelectorAll('.filter-btn');\n");
            writer.write("  let visibleCount = 0;\n");
            
            writer.write("  filterBtns.forEach(btn => {\n");
            writer.write("    btn.classList.remove('active');\n");
            writer.write("    if (btn.textContent.toUpperCase().includes(status) || \n");
            writer.write("        (status === 'all' && btn.textContent === 'All Tests')) {\n");
            writer.write("      btn.classList.add('active');\n");
            writer.write("    }\n");
            writer.write("  });\n");
            
            writer.write("  rows.forEach(row => {\n");
            writer.write("    if (status === 'all' || row.classList.contains(status)) {\n");
            writer.write("      row.style.display = '';\n");
            writer.write("      visibleCount++;\n");
            writer.write("    } else {\n");
            writer.write("      row.style.display = 'none';\n");
            writer.write("    }\n");
            writer.write("  });\n");
            
            writer.write("  document.getElementById('no-results').style.display = \n");
            writer.write("    visibleCount === 0 ? 'block' : 'none';\n");
            writer.write("}\n");
            writer.write("</script>\n");
            
            writer.write("</div>\n"); // container end
            writer.write("</body>\n");
            writer.write("</html>");
            
            System.out.println("UnifiedTest HTML report generated at: " + reportFile.getAbsolutePath());
        } catch (IOException e) {
            System.err.println("Failed to write UnifiedTest HTML report: " + e.getMessage());
        }
    }
    
    /**
     * Format a duration in milliseconds as a human-readable string.
     */
    private static String formatDuration(long millis) {
        if (millis < 1000) {
            return millis + "ms";
        } else if (millis < 60000) {
            return String.format("%.1fs", millis / 1000.0);
        } else {
            long minutes = millis / 60000;
            long seconds = (millis % 60000) / 1000;
            return String.format("%dm %ds", minutes, seconds);
        }
    }
} 