package com.github.mov2day.unifiedtest.agent.reporting;

import com.github.mov2day.unifiedtest.agent.collector.UnifiedTestResult;
import org.gradle.api.Project;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

public class HtmlReportGenerator {
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
