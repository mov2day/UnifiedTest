package io.github.mov2day.unifiedtest.reporting;

/**
 * ConsoleReporter provides formatted console output for test results and summaries.
 */
public class ConsoleReporter {
    /**
     * Theme options for console output formatting.
     */
    public enum Theme { 
        /** Standard console output theme with basic formatting */
        STANDARD, 
        /** Mocha-inspired theme with rich color scheme */
        MOCHA, 
        /** Minimal theme with essential formatting only */
        MINIMAL 
    }
    private final Theme theme;

    /**
     * Constructs a ConsoleReporter with the specified theme.
     * @param themeName the name of the theme
     */
    public ConsoleReporter(String themeName) {
        if ("mocha".equalsIgnoreCase(themeName)) this.theme = Theme.MOCHA;
        else if ("minimal".equalsIgnoreCase(themeName)) this.theme = Theme.MINIMAL;
        else this.theme = Theme.STANDARD;
    }

    /**
     * Prints a running test message.
     * @param display the test display name
     */
    public void testRunning(String display) {
        if (theme == Theme.MINIMAL) return;
        System.out.println("[RUNNING] " + display);
    }

    /**
     * Prints a test result message.
     * @param display the test display name
     * @param status the test status
     */
    public void testResult(String display, String status) {
        if (theme == Theme.MINIMAL) return;
        String symbol = "PASS".equals(status) ? "\u2705" : ("FAIL".equals(status) ? "\u274C" : "\u23ED");
        System.out.println("[" + symbol + "] " + display + " - " + status);
    }

    /**
     * Prints a summary of test results.
     * @param total total tests
     * @param passed passed tests
     * @param failed failed tests
     * @param skipped skipped tests
     */
    public void summary(int total, int passed, int failed, int skipped) {
        System.out.println(formatSummary(total, passed, failed, skipped));
    }

    /**
     * Formats the summary string.
     * @param total total tests
     * @param passed passed tests
     * @param failed failed tests
     * @param skipped skipped tests
     * @return formatted summary string
     */
    public String formatSummary(int total, int passed, int failed, int skipped) {
        if (theme == Theme.MOCHA) {
            return String.format("\n\uD83C\uDF6B === UnifiedTest Mocha Summary ===\n\u2705 %d Passed   \u274C %d Failed   \u23ED %d Skipped\n\u23F1 Total: %d\n==============================\n", passed, failed, skipped, total);
        } else if (theme == Theme.MINIMAL) {
            return String.format("[UnifiedTest] Passed: %d, Failed: %d, Skipped: %d, Total: %d", passed, failed, skipped, total);
        } else {
            return String.format("\n================ UnifiedTest Summary ================\nTotal: %d, Passed: %d, Failed: %d, Skipped: %d\n====================================================\n", total, passed, failed, skipped);
        }
    }
}
