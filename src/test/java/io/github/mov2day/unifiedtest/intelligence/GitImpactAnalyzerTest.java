package io.github.mov2day.unifiedtest.intelligence;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.assertEquals;

class GitImpactAnalyzerTest {
    @TempDir Path projectDir;

    @Test
    void fallsBackToTheFullSuiteWhenUntrackedFilesArePresent() throws Exception {
        runGit("init");
        runGit("-c", "user.email=test@example.com", "-c", "user.name=Test", "commit", "--allow-empty", "-m", "baseline");
        Files.writeString(projectDir.resolve("untracked-build-resource.txt"), "changed");
        CoverageMapStore.CoverageMap map = new CoverageMapStore.CoverageMap(
            CoverageMapStore.inventoryFingerprint(projectDir), Map.of(), Map.of(), true);

        GitImpactAnalyzer.ImpactResult result = GitImpactAnalyzer.analyze(projectDir, "HEAD", map);

        assertEquals("RUN_FULL_SUITE", result.decision());
        assertEquals("UNSUPPORTED_CHANGE:untracked-build-resource.txt", result.reason());
    }

    @Test
    void recommendsEveryTaskThatRunsAnImpactedSharedTestClass() throws Exception {
        Path source = projectDir.resolve("src/main/java/example/Subject.java");
        Files.createDirectories(source.getParent());
        Files.writeString(source, "package example; class Subject {}\n");
        runGit("init");
        runGit("add", ".");
        runGit("-c", "user.email=test@example.com", "-c", "user.name=Test", "commit", "-m", "baseline");
        Files.writeString(source, "package example; class Subject { int value() { return 1; } }\n");
        CoverageMapStore.CoverageMap map = new CoverageMapStore.CoverageMap(
            CoverageMapStore.inventoryFingerprint(projectDir),
            Map.of("example.Subject", Set.of("example.SharedTest")),
            Map.of("example.SharedTest", Set.of("test", "integrationTest")), true);

        GitImpactAnalyzer.ImpactResult result = GitImpactAnalyzer.analyze(projectDir, "HEAD", map);

        assertEquals("RUN_SELECTED_TESTS", result.decision());
        assertEquals(Set.of("example.SharedTest"), result.testsByTask().get("test"));
        assertEquals(Set.of("example.SharedTest"), result.testsByTask().get("integrationTest"));
    }

    @Test
    void fallsBackWhenAProfileCouldNotRepresentMethodOnlyFiltering() {
        CoverageMapStore.CoverageMap map = new CoverageMapStore.CoverageMap(
            CoverageMapStore.inventoryFingerprint(projectDir),
            Map.of("example.Subject", Set.of("example.SharedTest")),
            Map.of("example.SharedTest", Set.of("test")), false);

        GitImpactAnalyzer.ImpactResult result = GitImpactAnalyzer.analyze(projectDir, "HEAD", map);

        assertEquals("RUN_FULL_SUITE", result.decision());
        assertEquals("INCOMPLETE_COVERAGE_MAP", result.reason());
    }

    private void runGit(String... arguments) throws Exception {
        java.util.List<String> command = new java.util.ArrayList<>();
        command.add("git");
        java.util.Collections.addAll(command, arguments);
        Process process = new ProcessBuilder(command).directory(projectDir.toFile()).redirectErrorStream(true).start();
        assertEquals(0, process.waitFor(), new String(process.getInputStream().readAllBytes()));
    }
}
