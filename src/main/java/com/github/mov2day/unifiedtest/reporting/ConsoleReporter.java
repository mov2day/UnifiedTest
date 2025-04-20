package com.github.mov2day.unifiedtest.agent.reporting;

public class ConsoleReporter {
    public enum Theme { STANDARD, MOCHA, MINIMAL }
    private final Theme theme;
    public ConsoleReporter(String themeName) {
        if ("mocha".equalsIgnoreCase(themeName)) this.theme = Theme.MOCHA;
        else if ("minimal".equalsIgnoreCase(themeName)) this.theme = Theme.MINIMAL;
        else this.theme = Theme.STANDARD;
    }
    public void testRunning(String display) {
        if (theme == Theme.MINIMAL) return;
        System.out.println("[RUNNING] " + display);
    }
    public void testResult(String display, String status) {
        if (theme == Theme.MINIMAL) return;
        String symbol = "PASS".equals(status) ? "\u2705" : ("FAIL".equals(status) ? "\u274C" : "\u23ED");
        System.out.println("[" + symbol + "] " + display + " - " + status);
    }
    public void summary(int total, int passed, int failed, int skipped) {
        System.out.println(formatSummary(total, passed, failed, skipped));
    }
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
