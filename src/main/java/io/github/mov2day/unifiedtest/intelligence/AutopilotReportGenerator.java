package io.github.mov2day.unifiedtest.intelligence;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.github.mov2day.unifiedtest.collector.UnifiedTestResult;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/** Writes a compact, agent-friendly summary for a completed test run. */
public final class AutopilotReportGenerator {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private AutopilotReportGenerator() {}

    public static void generate(Path buildDirectory, String runId, List<UnifiedTestResult> results, AutopilotAnalysis analysis) {
        JsonObject report = new JsonObject();
        report.addProperty("schemaVersion", 1);
        report.addProperty("runId", runId);
        report.addProperty("decision", "REVIEW_CURRENT_RUN");
        report.addProperty("suggestedAction", analysis.getFlakyCount() > 0 ? "INVESTIGATE_FLAKY_TESTS" : "REVIEW_FAILURES");
        JsonObject counts = new JsonObject();
        analysis.getSignalCounts().forEach(counts::addProperty);
        report.add("signalCounts", counts);
        JsonArray tests = new JsonArray();
        for (UnifiedTestResult result : results) {
            JsonObject test = new JsonObject();
            test.addProperty("testId", result.getTestId());
            test.addProperty("testClassId", result.getTestClassId());
            test.addProperty("class", result.className);
            test.addProperty("name", result.testName);
            test.addProperty("status", result.status);
            JsonArray signals = new JsonArray();
            result.getIntelligenceSignals().forEach(signals::add);
            test.add("signals", signals);
            if (result.getFailureFingerprint() != null) test.addProperty("failureFingerprint", result.getFailureFingerprint());
            if (result.getMetadata().containsKey("traceUrl")) test.addProperty("traceUrl", result.getMetadata().get("traceUrl"));
            tests.add(test);
        }
        report.add("tests", tests);
        Path output = buildDirectory.resolve("unifiedtest/intelligence/autopilot.json");
        try {
            Files.createDirectories(output.getParent());
            Files.writeString(output, GSON.toJson(report), StandardCharsets.UTF_8);
        } catch (IOException ignored) {
            // Advisory output must never fail the build.
        }
    }
}
