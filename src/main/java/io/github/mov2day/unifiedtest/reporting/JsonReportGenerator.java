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
 * Generates JSON test reports from UnifiedTest results.
 * Creates structured JSON output that can be consumed by other tools or systems.
 */
public class JsonReportGenerator {
    public static void generate(Project project, Test testTask, UnifiedTestResultCollector collector) {
        File reportFile = new File(project.getBuildDir(), "unifiedtest/reports/results.json");
        reportFile.getParentFile().mkdirs();
        
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
        } catch (IOException e) {
            project.getLogger().error("Failed to write UnifiedTest JSON report", e);
        }
    }
}
