package com.github.mov2day.unifiedtest.reporting;

import com.github.mov2day.unifiedtest.collector.UnifiedTestResult;
import org.gradle.api.Project;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

public class JsonReportGenerator {
    @SuppressWarnings("deprecation")
    public static void generate(Project project, List<UnifiedTestResult> results) {
        File reportFile = new File(project.getBuildDir(), "unifiedtest/report.json");
        reportFile.getParentFile().mkdirs();
        try (FileWriter writer = new FileWriter(reportFile)) {
            writer.write("[\n");
            for (int i = 0; i < results.size(); i++) {
                UnifiedTestResult r = results.get(i);
                writer.write(String.format("  {\"class\":\"%s\", \"name\":\"%s\", \"status\":\"%s\"}%s\n",
                    r.className, r.testName, r.status, (i < results.size() - 1 ? "," : "")));
            }
            writer.write("]");
        } catch (IOException e) {
            project.getLogger().error("Failed to write UnifiedTest JSON report", e);
        }
    }
}
