package io.github.mov2day.unifiedtest.reporting;

import org.gradle.api.Project;
import org.gradle.api.tasks.testing.TestDescriptor;
import org.gradle.api.tasks.testing.TestListener;
import org.gradle.api.tasks.testing.TestResult;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

/**
 * Provides pretty-printed console output for test execution events.
 * Supports different themes for console output formatting.
 */
public class PrettyConsoleTestListener implements TestListener {
    private final Project project;
    private final String theme;
    private final Map<String, String> testStatus = new ConcurrentHashMap<>();
    private int total = 0, passed = 0, failed = 0, skipped = 0;

    /**
     * Creates a new PrettyConsoleTestListener with the specified theme.
     * @param project the Gradle project
     * @param theme the console output theme to use
     */
    public PrettyConsoleTestListener(Project project, String theme) {
        this.project = project;
        this.theme = theme;
    }

    @Override
    public void beforeTest(TestDescriptor testDescriptor) {
        String display = testDescriptor.getClassName() + "." + testDescriptor.getName();
        project.getLogger().lifecycle("[RUNNING] " + display);
    }

    @Override
    public void afterTest(TestDescriptor testDescriptor, TestResult result) {
        String display = testDescriptor.getClassName() + "." + testDescriptor.getName();
        String status;
        switch (result.getResultType()) {
            case SUCCESS:
                status = "PASS";
                passed++;
                break;
            case FAILURE:
                status = "FAIL";
                failed++;
                break;
            case SKIPPED:
                status = "SKIP";
                skipped++;
                break;
            default:
                status = result.getResultType().toString();
        }
        total++;
        testStatus.put(display, status);
        String symbol = "PASS".equals(status) ? "\u2705" : ("FAIL".equals(status) ? "\u274C" : "\u23ED");
        project.getLogger().lifecycle("[" + symbol + "] " + display + " - " + status);
    }

    @Override public void beforeSuite(TestDescriptor suite) {}

    @Override public void afterSuite(TestDescriptor suite, TestResult result) {
        if (suite.getParent() == null) { // root suite
            String summary;
            if ("mocha".equalsIgnoreCase(theme)) {
                summary = String.format("\n\uD83C\uDF6B === UnifiedTest Mocha Summary ===\n\u2705 %d Passed   \u274C %d Failed   \u23ED %d Skipped\n\u23F1 Total: %d\n==============================\n", passed, failed, skipped, total);
            } else if ("minimal".equalsIgnoreCase(theme)) {
                summary = String.format("[UnifiedTest] Passed: %d, Failed: %d, Skipped: %d, Total: %d", passed, failed, skipped, total);
            } else {
                summary = String.format("\n================ UnifiedTest Summary ================\nTotal: %d, Passed: %d, Failed: %d, Skipped: %d\n====================================================\n", total, passed, failed, skipped);
            }
            project.getLogger().lifecycle(summary);
        }
    }
}
