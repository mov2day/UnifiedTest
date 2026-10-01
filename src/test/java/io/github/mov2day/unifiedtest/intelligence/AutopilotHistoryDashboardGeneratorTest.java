package io.github.mov2day.unifiedtest.intelligence;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AutopilotHistoryDashboardGeneratorTest {
    @TempDir Path tempDir;

    @Test
    void rendersReliabilityTriageWithoutRawFailureData() throws Exception {
        RunHistoryStore.HistorySnapshot snapshot = new RunHistoryStore.HistorySnapshot(List.of(
            run("r1", "PASS", ""), run("r2", "FAIL", "fingerprint-1234567890"),
            run("r3", "PASS", ""), run("r4", "FAIL", "fingerprint-1234567890"),
            run("r5", "FAIL", "fingerprint-1234567890")));

        AutopilotHistoryDashboardGenerator.generate(tempDir, snapshot, true);
        String html = Files.readString(tempDir.resolve("unifiedtest/intelligence/dashboard.html"));

        assertTrue(html.contains("<h1>Autopilot</h1>"));
        assertTrue(html.contains("role=\"tablist\""));
        assertTrue(html.contains("id=\"overview-view\""));
        assertTrue(html.contains("id=\"triage-view\""));
        assertTrue(html.contains("Recent regressions"));
        assertTrue(html.contains("Flaky candidates"));
        assertTrue(html.contains("Known failures"));
        assertTrue(html.contains("Recurring failures"));
        assertTrue(html.contains("fingerprint-"));
        assertTrue(html.contains("&lt;script&gt;test&lt;/script&gt;"));
        assertTrue(html.contains("id=\"search\""));
        assertTrue(html.contains("../reports/index.html"));
        assertFalse(html.contains("database password"));
    }

    @Test
    void rendersAnEmptyStateBeforeTheFirstRun() {
        String html = AutopilotHistoryDashboardGenerator.render(new RunHistoryStore.HistorySnapshot(List.of()), false);
        assertTrue(html.contains("Awaiting observations"));
        assertTrue(html.contains("No raw failures or attachments are retained"));
        assertFalse(html.contains("../reports/index.html"));
    }

    private RunHistoryStore.HistoryRun run(String id, String status, String fingerprint) {
        return new RunHistoryStore.HistoryRun(id, "2026-07-22T10:0" + id.substring(1) + ":00Z", "test", List.of(
            new RunHistoryStore.HistoryTest("ut-1", "utc-1", "example.<Unsafe>", "<script>test</script>", status, fingerprint)));
    }
}
