package io.github.mov2day.unifiedtest.intelligence;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.github.mov2day.unifiedtest.collector.UnifiedTestResult;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/** Stores bounded local run observations and derives deterministic failure signals. */
public final class RunHistoryStore {
    private static final Gson GSON = new Gson();
    private static final int FLAKE_WINDOW = 10;
    private final Path historyFile;

    public RunHistoryStore(Path stateDir) {
        this.historyFile = stateDir.resolve("runs.jsonl");
    }

    public AutopilotAnalysis analyzeAndAppend(String runId, List<UnifiedTestResult> results, int historyLimit) {
        return analyzeAndAppend(runId, "", results, historyLimit);
    }

    public AutopilotAnalysis analyzeAndAppend(String runId, String taskName, List<UnifiedTestResult> results, int historyLimit) {
        List<JsonObject> history = readHistory();
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (UnifiedTestResult result : results) {
            List<JsonObject> observations = observationsFor(history, result.getTestId(), taskName);
            List<String> signals = signalsFor(result, observations);
            result.setIntelligenceSignals(signals);
            if (result.failureMessage != null || result.stackTrace != null) {
                result.setFailureFingerprint(fingerprint(result.failureMessage, result.stackTrace));
            }
            for (String signal : signals) counts.merge(signal, 1, Integer::sum);
        }
        history.add(toRunRecord(runId, taskName, results));
        int limit = Math.max(1, historyLimit);
        if (history.size() > limit) history = history.subList(history.size() - limit, history.size());
        writeHistory(history);
        return new AutopilotAnalysis(true, counts);
    }

    /** Returns a privacy-safe, backward-compatible snapshot for local dashboard rendering. */
    public HistorySnapshot readSnapshot() {
        List<HistoryRun> runs = new ArrayList<>();
        for (JsonObject run : readHistory()) {
            List<HistoryTest> tests = new ArrayList<>();
            JsonArray values = run.getAsJsonArray("tests");
            if (values != null) {
                for (JsonElement value : values) {
                    if (!value.isJsonObject()) continue;
                    JsonObject test = value.getAsJsonObject();
                    tests.add(new HistoryTest(
                        string(test, "testId"),
                        string(test, "testClassId"),
                        string(test, "className"),
                        string(test, "testName"),
                        string(test, "status"),
                        string(test, "fingerprint")));
                }
            }
            runs.add(new HistoryRun(string(run, "runId"), string(run, "timestamp"), string(run, "taskName"), tests));
        }
        return new HistorySnapshot(runs);
    }

    private List<String> signalsFor(UnifiedTestResult result, List<JsonObject> observations) {
        List<String> signals = new ArrayList<>();
        if (observations.isEmpty()) signals.add("NEW_TEST");
        if (isFailure(result.status) && !observations.isEmpty() && isPassing(status(observations.get(observations.size() - 1)))) {
            signals.add("REGRESSION");
        }
        List<String> statuses = observations.stream().map(RunHistoryStore::status).collect(Collectors.toCollection(ArrayList::new));
        statuses.add(result.status);
        if (statuses.size() > FLAKE_WINDOW) statuses = statuses.subList(statuses.size() - FLAKE_WINDOW, statuses.size());
        long passes = statuses.stream().filter(RunHistoryStore::isPassing).count();
        long failures = statuses.stream().filter(RunHistoryStore::isFailure).count();
        if (passes >= 2 && failures >= 2) signals.add("FLAKY");
        if (isFailure(result.status)) {
            String current = fingerprint(result.failureMessage, result.stackTrace);
            long previousMatches = observations.stream()
                .filter(observation -> current.equals(string(observation, "fingerprint")))
                .count();
            if (previousMatches >= 2) signals.add("KNOWN_FAILURE");
        }
        return signals;
    }

    private JsonObject toRunRecord(String runId, String taskName, List<UnifiedTestResult> results) {
        JsonObject run = new JsonObject();
        run.addProperty("runId", runId);
        run.addProperty("timestamp", Instant.now().toString());
        if (taskName != null && !taskName.isBlank()) run.addProperty("taskName", taskName);
        JsonArray tests = new JsonArray();
        for (UnifiedTestResult result : results) {
            JsonObject test = new JsonObject();
            test.addProperty("testId", result.getTestId());
            test.addProperty("testClassId", result.getTestClassId());
            test.addProperty("className", result.className);
            test.addProperty("testName", result.testName);
            test.addProperty("status", result.status);
            if (result.getFailureFingerprint() != null) test.addProperty("fingerprint", result.getFailureFingerprint());
            tests.add(test);
        }
        run.add("tests", tests);
        return run;
    }

    private List<JsonObject> observationsFor(List<JsonObject> history, String testId, String taskName) {
        List<JsonObject> observations = new ArrayList<>();
        for (JsonObject run : history) {
            if (taskName != null && !taskName.isBlank() && !taskName.equals(string(run, "taskName"))) continue;
            JsonArray tests = run.getAsJsonArray("tests");
            if (tests == null) continue;
            for (JsonElement test : tests) {
                JsonObject record = test.getAsJsonObject();
                if (testId.equals(string(record, "testId"))) observations.add(record);
            }
        }
        return observations;
    }

    private List<JsonObject> readHistory() {
        if (!Files.exists(historyFile)) return new ArrayList<>();
        try {
            return Files.readAllLines(historyFile, StandardCharsets.UTF_8).stream()
                .filter(line -> !line.isBlank())
                .map(line -> GSON.fromJson(line, JsonObject.class))
                .collect(Collectors.toCollection(ArrayList::new));
        } catch (IOException | RuntimeException ignored) {
            return new ArrayList<>();
        }
    }

    private void writeHistory(List<JsonObject> history) {
        try {
            Files.createDirectories(historyFile.getParent());
            Files.write(historyFile, history.stream().map(GSON::toJson).collect(Collectors.toList()), StandardCharsets.UTF_8);
        } catch (IOException ignored) {
            // Intelligence must never fail a test task because local state cannot be written.
        }
    }

    private static String status(JsonObject observation) { return string(observation, "status"); }
    private static String string(JsonObject object, String name) {
        return object.has(name) && !object.get(name).isJsonNull() ? object.get(name).getAsString() : "";
    }
    private static boolean isPassing(String status) { return "PASS".equalsIgnoreCase(status) || "PASSED".equalsIgnoreCase(status); }
    private static boolean isFailure(String status) { return "FAIL".equalsIgnoreCase(status) || "FAILED".equalsIgnoreCase(status); }

    public record HistorySnapshot(List<HistoryRun> runs) {
        public HistorySnapshot { runs = List.copyOf(runs); }
    }

    public record HistoryRun(String runId, String timestamp, String taskName, List<HistoryTest> tests) {
        public HistoryRun { tests = List.copyOf(tests); }
        public String displayTask() { return taskName == null || taskName.isBlank() ? "Unknown task" : taskName; }
    }

    public record HistoryTest(String testId, String testClassId, String className, String testName,
                              String status, String fingerprint) {
        public String displayName() { return testName == null || testName.isBlank() ? shortId(testId) : testName; }
        public String displayClass() { return className == null || className.isBlank() ? shortId(testClassId) : className; }
        private static String shortId(String value) {
            if (value == null || value.isBlank()) return "unknown";
            return value.length() <= 14 ? value : value.substring(0, 14);
        }
    }

    public static String fingerprint(String message, String trace) {
        String exceptionType = trace == null ? "" : trace.lines()
            .map(String::trim).filter(line -> !line.isBlank()).findFirst()
            .map(line -> line.replaceFirst("^Caused by:\\s*", ""))
            .map(line -> line.replaceFirst(":.*$", "").trim())
            .orElse("");
        String normalizedMessage = message == null ? "" : message.replaceAll("\\d+", "#").replaceAll("\\s+", " ").trim();
        String normalizedTrace = trace == null ? "" : trace.lines()
            .filter(line -> line.contains(" at ") && !line.contains("org.junit") && !line.contains("org.gradle") && !line.contains("java.base"))
            .limit(8)
            .map(line -> line.replaceAll(":\\d+", ":#").trim())
            .collect(Collectors.joining("|"));
        return sha256(exceptionType + "|" + normalizedMessage + "|" + normalizedTrace);
    }

    private static String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder();
            for (byte b : digest) builder.append(String.format(Locale.ROOT, "%02x", b));
            return builder.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }
}
