package io.github.mov2day.unifiedtest.intelligence;

import io.github.mov2day.unifiedtest.collector.UnifiedTestResult;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RunHistoryStoreTest {
    @TempDir Path tempDir;

    @Test
    void classifiesRegressionsFlakesAndKnownFailuresWithoutPersistingRawFailures() throws Exception {
        RunHistoryStore store = new RunHistoryStore(tempDir);
        assertTrue(store.analyzeAndAppend("r1", List.of(result("PASS", null)), 30).getSignalCounts().containsKey("NEW_TEST"));
        UnifiedTestResult regression = result("FAIL", "connection 42 refused");
        store.analyzeAndAppend("r2", List.of(regression), 30);
        assertTrue(regression.getIntelligenceSignals().contains("REGRESSION"));
        store.analyzeAndAppend("r3", List.of(result("PASS", null)), 30);
        UnifiedTestResult flaky = result("FAIL", "connection 99 refused");
        store.analyzeAndAppend("r4", List.of(flaky), 30);
        assertTrue(flaky.getIntelligenceSignals().contains("FLAKY"));

        RunHistoryStore knownStore = new RunHistoryStore(tempDir.resolve("known"));
        knownStore.analyzeAndAppend("k1", List.of(result("FAIL", "same failure")), 30);
        knownStore.analyzeAndAppend("k2", List.of(result("FAIL", "same failure")), 30);
        UnifiedTestResult known = result("FAIL", "same failure");
        knownStore.analyzeAndAppend("k3", List.of(known), 30);
        assertTrue(known.getIntelligenceSignals().contains("KNOWN_FAILURE"));
        String history = java.nio.file.Files.readString(tempDir.resolve("known/runs.jsonl"));
        assertTrue(!history.contains("same failure"));
    }

    @Test
    void stableIdentityIncludesTheParameterizedDisplayName() {
        assertEquals(TestIdentity.testId("JUnit5", "example.SampleTest", "adds[1]"),
            TestIdentity.testId("JUnit5", "example.SampleTest", "adds[1]"));
        assertTrue(!TestIdentity.testId("JUnit5", "example.SampleTest", "adds[1]")
            .equals(TestIdentity.testId("JUnit5", "example.SampleTest", "adds[2]")));
    }

    @Test
    void fingerprintsIncludeTheExceptionType() {
        String state = "java.lang.IllegalStateException: unavailable\\n at example.SampleTest.checksConnection(SampleTest.java:10)";
        String argument = "java.lang.IllegalArgumentException: unavailable\\n at example.SampleTest.checksConnection(SampleTest.java:10)";
        assertNotEquals(RunHistoryStore.fingerprint("unavailable", state), RunHistoryStore.fingerprint("unavailable", argument));
    }

    @Test
    void persistsSafeDisplayLabelsAndReadsLegacyRecords() throws Exception {
        RunHistoryStore store = new RunHistoryStore(tempDir);
        store.analyzeAndAppend("r1", "integrationTest", List.of(result("PASS", null)), 2);
        RunHistoryStore.HistorySnapshot current = store.readSnapshot();
        assertEquals("integrationTest", current.runs().get(0).taskName());
        assertEquals("example.SampleTest", current.runs().get(0).tests().get(0).className());
        assertEquals("checksConnection", current.runs().get(0).tests().get(0).testName());

        java.nio.file.Files.writeString(tempDir.resolve("legacy.jsonl"), """
            {"runId":"legacy","timestamp":"2026-01-01T00:00:00Z","tests":[{"testId":"ut-legacy","testClassId":"utc-legacy","status":"FAIL"}]}
            """);
        RunHistoryStore legacy = new RunHistoryStore(tempDir.resolve("legacy"));
        java.nio.file.Files.createDirectories(tempDir.resolve("legacy"));
        java.nio.file.Files.move(tempDir.resolve("legacy.jsonl"), tempDir.resolve("legacy/runs.jsonl"));
        RunHistoryStore.HistoryTest legacyTest = legacy.readSnapshot().runs().get(0).tests().get(0);
        assertEquals("ut-legacy", legacyTest.displayName());
        assertEquals("utc-legacy", legacyTest.displayClass());

        store.analyzeAndAppend("r2", "test", List.of(result("PASS", null)), 2);
        store.analyzeAndAppend("r3", "test", List.of(result("PASS", null)), 2);
        assertEquals(2, store.readSnapshot().runs().size());
    }

    @Test
    void keepsFailureSignalsWithinTheirOriginatingTestTask() {
        RunHistoryStore store = new RunHistoryStore(tempDir);
        store.analyzeAndAppend("unit-pass", "test", List.of(result("PASS", null)), 30);
        UnifiedTestResult integrationFailure = result("FAIL", "connection refused");
        store.analyzeAndAppend("integration-fail", "integrationTest", List.of(integrationFailure), 30);

        assertTrue(!integrationFailure.getIntelligenceSignals().contains("REGRESSION"),
            "A pass from another task must not classify this task's first failure as a regression");
    }

    private UnifiedTestResult result(String status, String message) {
        return new UnifiedTestResult("example.SampleTest", "checksConnection", status, message,
            message == null ? null : "java.lang.IllegalStateException: " + message + "\n at example.SampleTest.checksConnection(SampleTest.java:10)", 10L, "JUnit5");
    }
}
