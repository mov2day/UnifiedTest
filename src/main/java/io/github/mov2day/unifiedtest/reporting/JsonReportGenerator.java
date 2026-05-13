package io.github.mov2day.unifiedtest.reporting;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.github.mov2day.unifiedtest.collector.UnifiedTestResult;
import io.github.mov2day.unifiedtest.collector.UnifiedTestResultCollector;
import org.gradle.api.Project;
import org.gradle.api.tasks.testing.Test;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Generates JSON test reports from UnifiedTest results.
 * Creates structured JSON output that can be consumed by other tools or systems.
 */
public class JsonReportGenerator {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static void generate(Project project, Test testTask, UnifiedTestResultCollector collector) {
        generate(project, testTask, collector, project.getName(), "");
    }

    public static void generate(Project project, Test testTask, UnifiedTestResultCollector collector, String serviceName, String runId) {
        File reportFile = project.getLayout()
            .getBuildDirectory()
            .file("unifiedtest/reports/results.json")
            .get()
            .getAsFile();
        reportFile.getParentFile().mkdirs();
        
        try (FileWriter writer = new FileWriter(reportFile)) {
            GSON.toJson(buildReport(testTask, collector.getResults(), serviceName, runId), writer);
        } catch (IOException e) {
            project.getLogger().error("Failed to write UnifiedTest JSON report", e);
        }
    }

    private static JsonObject buildReport(Test testTask, List<UnifiedTestResult> results, String serviceName, String runId) {
        JsonObject report = new JsonObject();
        report.addProperty("serviceName", serviceName);
        report.addProperty("runId", runId);
        report.addProperty("task", testTask.getName());

        JsonObject summary = new JsonObject();
        summary.addProperty("total", results.size());
        summary.addProperty("passed", results.stream().filter(r -> "PASS".equals(r.status)).count());
        summary.addProperty("failed", results.stream().filter(r -> "FAIL".equals(r.status)).count());
        summary.addProperty("skipped", results.stream().filter(r -> "SKIP".equals(r.status)).count());
        summary.addProperty("durationMs", results.stream().mapToLong(r -> r.duration).sum());
        report.add("summary", summary);

        JsonArray testArray = new JsonArray();
        for (UnifiedTestResult result : results) {
            JsonObject test = new JsonObject();
            test.addProperty("class", result.className);
            test.addProperty("name", result.testName);
            test.addProperty("status", result.status);
            test.addProperty("durationMs", result.duration);
            test.addProperty("framework", result.framework);
            if (result.failureMessage != null) {
                test.addProperty("failureMessage", result.failureMessage);
            }
            if (result.stackTrace != null) {
                test.addProperty("stackTrace", result.stackTrace);
            }
            if (!result.getMetadata().isEmpty()) {
                JsonObject metadata = new JsonObject();
                for (Map.Entry<String, String> entry : result.getMetadata().entrySet()) {
                    metadata.addProperty(entry.getKey(), entry.getValue());
                }
                test.add("metadata", metadata);
            }
            testArray.add(test);
        }
        report.add("tests", testArray);
        return report;
    }
}
