package io.github.mov2day.unifiedtest.reporting;

import org.gradle.api.Project;
import org.gradle.api.tasks.testing.TestDescriptor;
import org.gradle.api.tasks.testing.TestListener;
import org.gradle.api.tasks.testing.TestResult;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Provides pretty-printed console output for test execution events.
 * Supports different themes for console output formatting.
 */
public class PrettyConsoleTestListener implements TestListener {
    private final Project project;
    private final Map<String, String> testStatus = new ConcurrentHashMap<>();
    private int total = 0, passed = 0, failed = 0, skipped = 0;

    // ANSI color codes
    private static final String RESET = "\u001B[0m";
    private static final String GREEN = "\u001B[32m";
    private static final String RED = "\u001B[31m";
    private static final String YELLOW = "\u001B[33m";
    private static final String CYAN = "\u001B[36m";
    private static final String BOLD = "\u001B[1m";

    /**
     * Creates a new PrettyConsoleTestListener with the specified theme.
     * @param project the Gradle project
     * @param theme the console output theme to use
     */
    public PrettyConsoleTestListener(Project project, String theme) {
        this.project = project;
    }

    /**
     * Formats the test name to be more readable by removing package name
     * and adding proper spacing.
     */
    private String formatTestName(TestDescriptor testDescriptor) {
        String className = testDescriptor.getClassName();
        String methodName = testDescriptor.getName();
        
        // Extract just the class name without package
        className = className.substring(className.lastIndexOf('.') + 1);
        
        // Format as "ClassName › testMethodName"
        return className + " › " + methodName;
    }

    @Override
    public void beforeTest(TestDescriptor testDescriptor) {
        String display = formatTestName(testDescriptor);
        project.getLogger().lifecycle(CYAN + "⏳ [RUNNING] " + display + RESET);
    }

    @Override
    public void afterTest(TestDescriptor testDescriptor, TestResult result) {
        String display = formatTestName(testDescriptor);
        String status;
        String color;
        String symbol;
        
        switch (result.getResultType()) {
            case SUCCESS:
                status = "PASS";
                color = GREEN;
                symbol = "✅";
                passed++;
                break;
            case FAILURE:
                status = "FAIL";
                color = RED;
                symbol = "❌";
                failed++;
                break;
            case SKIPPED:
                status = "SKIP";
                color = YELLOW;
                symbol = "⏭";
                skipped++;
                break;
            default:
                status = result.getResultType().toString();
                color = RESET;
                symbol = "•";
        }
        total++;
        testStatus.put(display, status);
        
        long durationMs = result.getEndTime() - result.getStartTime();
        String duration = String.format("(%.2fs)", durationMs / 1000.0);
        
        project.getLogger().lifecycle(color + symbol + " " + display + " - " + BOLD + status + RESET + 
            color + " " + duration + RESET);
    }

    @Override public void beforeSuite(TestDescriptor suite) {}

    @Override 
    public void afterSuite(TestDescriptor suite, TestResult result) {
        if (suite.getParent() == null) { // root suite
            long totalTimeMillis = result.getEndTime() - result.getStartTime();
            String formattedTime = formatDuration(totalTimeMillis);

            String summary = String.format("\n%s══════════════ UnifiedTest Summary ══════════════%s\n" +
                "%s✅ Passed:%s %d    %s❌ Failed:%s %d    %s⏭ Skipped:%s %d\n" +
                "%s📊 Total Tests: %d    🕒 Total Time: %s%s\n\n" +
                "Status Distribution:\n" +
                "%s✅ PASS: %.1f%%%s (%d tests)\n" +
                "%s❌ FAIL: %.1f%%%s (%d tests)\n" +
                "%s⏭ SKIP: %.1f%%%s (%d tests)\n" +
                "═══════════════════════════════════════════════\n",
                BOLD, RESET,
                GREEN, RESET, passed,
                RED, RESET, failed,
                YELLOW, RESET, skipped,
                CYAN, total, formattedTime, RESET,
                GREEN, (passed * 100.0 / total), RESET, passed,
                RED, (failed * 100.0 / total), RESET, failed,
                YELLOW, (skipped * 100.0 / total), RESET, skipped);
            
            project.getLogger().lifecycle(summary);
        }
    }

    private String formatDuration(long millis) {
        long hours = millis / (60 * 60 * 1000);
        long minutes = (millis % (60 * 60 * 1000)) / (60 * 1000);
        long seconds = (millis % (60 * 1000)) / 1000;
        long ms = millis % 1000;
        
        StringBuilder timeStr = new StringBuilder();
        if (hours > 0) timeStr.append(hours).append("h ");
        if (minutes > 0) timeStr.append(minutes).append("m ");
        if (seconds > 0 || ms > 0) timeStr.append(String.format("%d.%03ds", seconds, ms));
        
        return timeStr.length() > 0 ? timeStr.toString() : "0s";
    }
}
