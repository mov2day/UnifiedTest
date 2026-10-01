package io.github.mov2day.unifiedtest.intelligence;

import javax.inject.Inject;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.Property;

/** Opt-in configuration for local UnifiedTest Autopilot decisions. */
public class IntelligenceConfig {
    private final Property<Boolean> enabled;
    private final Property<String> baselineRef;
    private final Property<Integer> historyLimit;

    @Inject
    public IntelligenceConfig(ObjectFactory objects) {
        enabled = objects.property(Boolean.class).convention(false);
        baselineRef = objects.property(String.class).convention("origin/main");
        historyLimit = objects.property(Integer.class).convention(30);
    }

    public Property<Boolean> getEnabled() { return enabled; }
    public Property<String> getBaselineRef() { return baselineRef; }
    public Property<Integer> getHistoryLimit() { return historyLimit; }
    public void setEnabled(boolean value) { enabled.set(value); }
    public void setBaselineRef(String value) { baselineRef.set(value); }
    public void setHistoryLimit(int value) { historyLimit.set(value); }
}
