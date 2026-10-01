package io.github.mov2day.unifiedtest.intelligence;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** Summary attached to a completed run without persisting raw failure data. */
public final class AutopilotAnalysis {
    private final boolean enabled;
    private final Map<String, Integer> signalCounts;

    public AutopilotAnalysis(boolean enabled, Map<String, Integer> signalCounts) {
        this.enabled = enabled;
        this.signalCounts = new LinkedHashMap<>(signalCounts);
    }

    public static AutopilotAnalysis disabled() { return new AutopilotAnalysis(false, Collections.emptyMap()); }
    public boolean isEnabled() { return enabled; }
    public Map<String, Integer> getSignalCounts() { return Collections.unmodifiableMap(signalCounts); }
    public int getFlakyCount() { return signalCounts.getOrDefault("FLAKY", 0); }
}
