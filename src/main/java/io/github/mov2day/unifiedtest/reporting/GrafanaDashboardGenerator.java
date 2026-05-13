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

/**
 * Generates an importable Grafana dashboard JSON for traces and test run health.
 */
public class GrafanaDashboardGenerator {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static void generate(Project project, Test testTask, UnifiedTestResultCollector collector, String serviceName, String runId) {
        File dashboardFile = project.getLayout()
            .getBuildDirectory()
            .file("unifiedtest/dashboard/grafana-dashboard.json")
            .get()
            .getAsFile();
        dashboardFile.getParentFile().mkdirs();

        try (FileWriter writer = new FileWriter(dashboardFile)) {
            GSON.toJson(buildDashboard(testTask, collector.getResults(), serviceName, runId), writer);
            project.getLogger().lifecycle("UnifiedTest Grafana dashboard generated at: {}", dashboardFile.getAbsolutePath());
        } catch (IOException e) {
            project.getLogger().warn("Failed to write UnifiedTest Grafana dashboard", e);
        }
    }

    private static JsonObject buildDashboard(Test testTask, List<UnifiedTestResult> results, String serviceName, String runId) {
        JsonObject dashboard = new JsonObject();
        dashboard.addProperty("title", "UnifiedTest - " + serviceName);
        dashboard.addProperty("schemaVersion", 39);
        dashboard.addProperty("version", 1);
        dashboard.addProperty("refresh", "30s");
        dashboard.add("tags", array("unifiedtest", "tests", serviceName));
        dashboard.add("templating", templating(serviceName));
        dashboard.add("annotations", annotations());
        dashboard.add("panels", panels(testTask, results, serviceName, runId));
        dashboard.add("time", timeRange());
        return dashboard;
    }

    private static JsonObject templating(String serviceName) {
        JsonObject templating = new JsonObject();
        JsonArray list = new JsonArray();
        JsonObject service = new JsonObject();
        service.addProperty("name", "serviceName");
        service.addProperty("type", "constant");
        service.addProperty("label", "Service");
        service.addProperty("query", serviceName);
        service.addProperty("current", serviceName);
        list.add(service);
        templating.add("list", list);
        return templating;
    }

    private static JsonObject annotations() {
        JsonObject annotations = new JsonObject();
        annotations.add("list", new JsonArray());
        return annotations;
    }

    private static JsonObject timeRange() {
        JsonObject time = new JsonObject();
        time.addProperty("from", "now-24h");
        time.addProperty("to", "now");
        return time;
    }

    private static JsonArray panels(Test testTask, List<UnifiedTestResult> results, String serviceName, String runId) {
        long total = results.size();
        long passed = results.stream().filter(r -> "PASS".equalsIgnoreCase(r.status)).count();
        long failed = results.stream().filter(r -> "FAIL".equalsIgnoreCase(r.status)).count();
        long skipped = results.stream().filter(r -> "SKIP".equalsIgnoreCase(r.status)).count();

        JsonArray panels = new JsonArray();
        panels.add(statPanel(1, "Latest Run Summary", 0, 0,
            "Service: " + serviceName + "\\nRun: " + runId + "\\nTask: " + testTask.getName() +
                "\\nTotal: " + total + "\\nPassed: " + passed + "\\nFailed: " + failed + "\\nSkipped: " + skipped));
        panels.add(tracePanel(2, "Failure Trace Search", 12, 0,
            "{ service.name = \"$serviceName\" && test.status = \"FAIL\" }"));
        panels.add(tracePanel(3, "Slowest Tests", 0, 8,
            "{ service.name = \"$serviceName\" } | sort(test.duration_ms desc)"));
        panels.add(tracePanel(4, "Failures By Class", 12, 8,
            "{ service.name = \"$serviceName\" && test.status = \"FAIL\" } | by(test.class)"));
        panels.add(tracePanel(5, "Framework Breakdown", 0, 16,
            "{ service.name = \"$serviceName\" } | by(test.framework)"));
        panels.add(tracePanel(6, "Flaky Tests", 12, 16,
            "{ service.name = \"$serviceName\" && test.flaky > 0 }"));
        panels.add(tracePanel(7, "Pass Rate Trend", 0, 24,
            "{ service.name = \"$serviceName\" } | by(unifiedtest.run_id)"));
        return panels;
    }

    private static JsonObject statPanel(int id, String title, int x, int y, String markdown) {
        JsonObject panel = basePanel(id, title, "text", x, y);
        JsonObject options = new JsonObject();
        options.addProperty("mode", "markdown");
        options.addProperty("content", markdown);
        panel.add("options", options);
        return panel;
    }

    private static JsonObject tracePanel(int id, String title, int x, int y, String query) {
        JsonObject panel = basePanel(id, title, "table", x, y);
        JsonArray targets = new JsonArray();
        JsonObject target = new JsonObject();
        target.addProperty("datasource", "Tempo");
        target.addProperty("queryType", "traceql");
        target.addProperty("query", query);
        targets.add(target);
        panel.add("targets", targets);
        return panel;
    }

    private static JsonObject basePanel(int id, String title, String type, int x, int y) {
        JsonObject panel = new JsonObject();
        panel.addProperty("id", id);
        panel.addProperty("title", title);
        panel.addProperty("type", type);
        JsonObject gridPos = new JsonObject();
        gridPos.addProperty("h", 8);
        gridPos.addProperty("w", 12);
        gridPos.addProperty("x", x);
        gridPos.addProperty("y", y);
        panel.add("gridPos", gridPos);
        return panel;
    }

    private static JsonArray array(String... values) {
        JsonArray array = new JsonArray();
        for (String value : values) {
            array.add(value);
        }
        return array;
    }
}
