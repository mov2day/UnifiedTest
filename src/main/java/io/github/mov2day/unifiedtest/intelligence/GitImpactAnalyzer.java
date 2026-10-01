package io.github.mov2day.unifiedtest.intelligence;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Conservative, shell-free Git diff to test-class recommendation engine. */
public final class GitImpactAnalyzer {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private GitImpactAnalyzer() {}

    public static ImpactResult analyze(Path projectDir, String baselineRef, CoverageMapStore.CoverageMap map) {
        String expectedFingerprint = CoverageMapStore.inventoryFingerprint(projectDir);
        if (map == null) return ImpactResult.full("MISSING_COVERAGE_MAP");
        if (!expectedFingerprint.equals(map.inventoryFingerprint())) return ImpactResult.full("STALE_COVERAGE_MAP");
        if (!map.completeCoverage()) return ImpactResult.full("INCOMPLETE_COVERAGE_MAP");
        List<String> changed = changedFiles(projectDir, baselineRef);
        if (changed == null) return ImpactResult.full("UNAVAILABLE_BASELINE");
        if (changed.isEmpty()) return ImpactResult.full("NO_CHANGED_FILES");
        Set<String> testClasses = new LinkedHashSet<>();
        for (String file : changed) {
            String productionClass = productionClassFor(file);
            if (productionClass != null) {
                Set<String> tests = map.productionToTests().get(productionClass);
                if (tests == null || tests.isEmpty()) return ImpactResult.full("UNMAPPED_CLASS:" + productionClass);
                testClasses.addAll(tests);
                continue;
            }
            String testClass = testClassFor(file);
            if (testClass != null) {
                testClasses.add(testClass);
                continue;
            }
            return ImpactResult.full("UNSUPPORTED_CHANGE:" + file);
        }
        if (testClasses.isEmpty()) return ImpactResult.full("NO_IMPACTED_TESTS");
        Map<String, Set<String>> byTask = new LinkedHashMap<>();
        for (String testClass : testClasses) {
            Set<String> tasks = map.testTasksByClass().get(testClass);
            if (tasks == null || tasks.isEmpty()) return ImpactResult.full("MISSING_TEST_TASK:" + testClass);
            for (String task : tasks) {
                byTask.computeIfAbsent(task, ignored -> new LinkedHashSet<>()).add(testClass);
            }
        }
        return ImpactResult.selected(changed, byTask);
    }

    public static void write(Path output, ImpactResult result) throws IOException {
        JsonObject json = new JsonObject();
        json.addProperty("schemaVersion", 1);
        json.addProperty("decision", result.decision());
        json.addProperty("reason", result.reason());
        JsonArray changed = new JsonArray();
        result.changedFiles().forEach(changed::add);
        json.add("changedFiles", changed);
        JsonObject tasks = new JsonObject();
        result.testsByTask().forEach((task, tests) -> {
            JsonArray values = new JsonArray();
            tests.forEach(values::add);
            tasks.add(task, values);
        });
        json.add("testsByTask", tasks);
        Files.createDirectories(output.getParent());
        Files.writeString(output, GSON.toJson(json), StandardCharsets.UTF_8);
    }

    private static List<String> changedFiles(Path projectDir, String baselineRef) {
        List<String> all = new ArrayList<>();
        for (List<String> command : List.of(
            List.of("git", "diff", "--name-only", baselineRef + "...HEAD"),
            List.of("git", "diff", "--name-only", "--cached"),
            List.of("git", "diff", "--name-only")
        )) {
            ProcessResult result = run(projectDir, command);
            if (result.exitCode != 0) return null;
            for (String line : result.output.lines().toList()) if (!line.isBlank() && !all.contains(line)) all.add(line.trim());
        }
        ProcessResult untracked = run(projectDir, List.of("git", "ls-files", "--others", "--exclude-standard"));
        if (untracked.exitCode != 0) return null;
        for (String line : untracked.output.lines().toList()) {
            String path = line.trim();
            if (!path.isBlank() && !isGenerated(path) && !all.contains(path)) all.add(path);
        }
        return all;
    }

    private static boolean isGenerated(String path) {
        return path.startsWith("build/") || path.startsWith(".gradle/") || path.startsWith("out/");
    }

    private static ProcessResult run(Path projectDir, List<String> command) {
        try {
            Process process = new ProcessBuilder(command).directory(projectDir.toFile()).redirectErrorStream(true).start();
            String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            return new ProcessResult(process.waitFor(), output);
        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
            return new ProcessResult(1, "");
        }
    }

    private static String productionClassFor(String path) {
        for (String root : List.of("src/main/java/", "src/main/groovy/")) {
            if (path.startsWith(root) && (path.endsWith(".java") || path.endsWith(".groovy"))) {
                return path.substring(root.length(), path.lastIndexOf('.')).replace('/', '.');
            }
        }
        return null;
    }

    private static String testClassFor(String path) {
        for (String root : List.of("src/test/java/", "src/test/groovy/")) {
            if (path.startsWith(root) && (path.endsWith(".java") || path.endsWith(".groovy"))) {
                return path.substring(root.length(), path.lastIndexOf('.')).replace('/', '.');
            }
        }
        return null;
    }

    public record ImpactResult(String decision, String reason, List<String> changedFiles, Map<String, Set<String>> testsByTask) {
        static ImpactResult full(String reason) { return new ImpactResult("RUN_FULL_SUITE", reason, List.of(), Map.of()); }
        static ImpactResult selected(List<String> changed, Map<String, Set<String>> byTask) { return new ImpactResult("RUN_SELECTED_TESTS", "HIGH_CONFIDENCE_COVERAGE_MAP", List.copyOf(changed), byTask); }
    }
    private record ProcessResult(int exitCode, String output) {}
}
