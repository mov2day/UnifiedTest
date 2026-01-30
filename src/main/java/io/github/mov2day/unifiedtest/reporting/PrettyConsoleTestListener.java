package io.github.mov2day.unifiedtest.reporting;

import org.gradle.api.Project;
import org.gradle.api.tasks.testing.TestDescriptor;
import org.gradle.api.tasks.testing.TestListener;
import org.gradle.api.tasks.testing.TestResult;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

import io.github.mov2day.unifiedtest.collector.UnifiedTestResultCollector;
import io.github.mov2day.unifiedtest.collector.UnifiedTestResult;

/**
 * Provides pretty-printed console output for test execution events.
 * Supports different themes for console output formatting.
 */
public class PrettyConsoleTestListener implements TestListener {
    private final Project project;
    private final Map<String, String> testStatus = new ConcurrentHashMap<>();
    private int total = 0, passed = 0, failed = 0, skipped = 0;
    private final UnifiedTestResultCollector collector;

    private static final boolean FORCE_ANSI = Boolean.parseBoolean(System.getenv().getOrDefault("UNIFIEDTEST_FORCE_ANSI", "false"));
    private static final boolean IS_WINDOWS = System.getProperty("os.name").toLowerCase().contains("win");
    private static final String RESET = (IS_WINDOWS && !FORCE_ANSI) ? "" : "\u001B[0m";
    private static final String GREEN = (IS_WINDOWS && !FORCE_ANSI) ? "" : "\u001B[32m";
    private static final String RED = (IS_WINDOWS && !FORCE_ANSI) ? "" : "\u001B[31m";
    private static final String YELLOW = (IS_WINDOWS && !FORCE_ANSI) ? "" : "\u001B[33m";
    private static final String CYAN = (IS_WINDOWS && !FORCE_ANSI) ? "" : "\u001B[36m";
    private static final String BOLD = (IS_WINDOWS && !FORCE_ANSI) ? "" : "\u001B[1m";
    private static final String PASS_SYMBOL = (IS_WINDOWS && !FORCE_ANSI) ? "[PASS]" : "✅";
    private static final String FAIL_SYMBOL = (IS_WINDOWS && !FORCE_ANSI) ? "[FAIL]" : "❌";
    private static final String SKIP_SYMBOL = (IS_WINDOWS && !FORCE_ANSI) ? "[SKIP]" : "⏭";
    private static final String RUNNING_SYMBOL = (IS_WINDOWS && !FORCE_ANSI) ? "[RUN]" : "⏳";

    /**
     * Creates a new PrettyConsoleTestListener with the specified theme.
     * @param project the Gradle project
     * @param theme the console output theme to use
     * @param collector the UnifiedTestResultCollector to add results to
     */
    public PrettyConsoleTestListener(Project project, String theme, UnifiedTestResultCollector collector) {
        this.project = project;
        this.collector = collector;
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
        project.getLogger().lifecycle(CYAN + RUNNING_SYMBOL + " [RUNNING] " + display + RESET);
    }

    @Override
    public void afterTest(TestDescriptor testDescriptor, TestResult result) {
        String display = formatTestName(testDescriptor);
        String status;
        String color;
        String symbol;
        String message = null;
        String trace = null;
        
        switch (result.getResultType()) {
            case SUCCESS:
                status = "PASS";
                color = GREEN;
                symbol = PASS_SYMBOL;
                passed++;
                break;
            case FAILURE:
                status = "FAIL";
                color = RED;
                symbol = FAIL_SYMBOL;
                failed++;
                if (result.getException() != null) {
                    message = result.getException().getMessage();
                    StringWriter sw = new StringWriter();
                    result.getException().printStackTrace(new PrintWriter(sw));
                    trace = sw.toString();
                }
                break;
            case SKIPPED:
                status = "SKIP";
                color = YELLOW;
                symbol = SKIP_SYMBOL;
                skipped++;
                break;
            default:
                status = result.getResultType().toString();
                color = RESET;
                symbol = "*";
        }
        total++;
        testStatus.put(display, status);
        
        long durationMs = result.getEndTime() - result.getStartTime();
        String duration = String.format("(%.2fs)", durationMs / 1000.0);
        
        project.getLogger().lifecycle(color + symbol + " " + display + " - " + BOLD + status + RESET + 
            color + " " + duration + RESET);

        if (result.getResultType() == TestResult.ResultType.FAILURE && result.getException() != null) {
            project.getLogger().error("\n" + RED + "Failure Details:" + RESET);
            project.getLogger().error(RED + "Message: " + RESET + result.getException().getMessage());
            project.getLogger().error(RED + "Stack Trace:" + RESET);
            StringWriter sw = new StringWriter();
            result.getException().printStackTrace(new PrintWriter(sw));
            String[] stackTraceLines = sw.toString().split("\\n");
            for (String line : stackTraceLines) {
                project.getLogger().error("  " + line);
            }
            project.getLogger().lifecycle(""); // Empty line for better readability
        }

        // Add result to collector
        collector.addResult(new UnifiedTestResult(
            testDescriptor.getClassName(),
            testDescriptor.getName(),
            status,
            message,
            trace,
            durationMs
        ));
    }

    @Override public void beforeSuite(TestDescriptor suite) {}

    @Override
    public void afterSuite(TestDescriptor suite, TestResult result) {
        if (suite.getParent() == null) { // root suite
            long totalTimeMillis = result.getEndTime() - result.getStartTime();
            String formattedTime = formatDuration(totalTimeMillis);
            String summary = String.format("\n%sUnifiedTest Summary%s\n" +
                "%s Passed: %-4d %s Failed: %-4d %s Skipped: %-4d\n" +
                "%sTotal Tests: %-4d Time: %s%s\n" +
                "Status Distribution: PASS: %5.1f%% (%d)  FAIL: %5.1f%% (%d)  SKIP: %5.1f%% (%d)\n" +
                "==============================================\n",
                BOLD, RESET,
                GREEN, passed, RED, failed, YELLOW, skipped,
                CYAN, total, formattedTime, RESET,
                (passed * 100.0 / Math.max(1, total)), passed,
                (failed * 100.0 / Math.max(1, total)), failed,
                (skipped * 100.0 / Math.max(1, total)), skipped);
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
