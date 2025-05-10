package io.github.mov2day.unifiedtest.extension;

import org.gradle.api.provider.Property;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.Action;
import javax.inject.Inject;
import java.util.List;
import java.util.ArrayList;

/**
 * Extension for configuring test management system integrations.
 * Supports multiple test management systems like Jira Zephyr and TestRail.
 */
public class TestManagementExtension {
    private final Property<Boolean> enabled;
    private final List<TestManagementSystem> systems;
    private final ObjectFactory objects;

    @Inject
    public TestManagementExtension(ObjectFactory objects) {
        this.objects = objects;
        this.enabled = objects.property(Boolean.class).convention(false);
        this.systems = new ArrayList<>();
    }

    /**
     * Whether test management integration is enabled.
     */
    public Property<Boolean> getEnabled() {
        return enabled;
    }

    /**
     * Configure Jira Zephyr integration.
     */
    public void zephyr(Action<ZephyrConfig> action) {
        ZephyrConfig config = new ZephyrConfig(objects);
        action.execute(config);
        systems.add(new TestManagementSystem("zephyr", config));
    }

    /**
     * Configure TestRail integration.
     */
    public void testRail(Action<TestRailConfig> action) {
        TestRailConfig config = new TestRailConfig(objects);
        action.execute(config);
        systems.add(new TestManagementSystem("testrail", config));
    }

    /**
     * Get all configured test management systems.
     */
    public List<TestManagementSystem> getSystems() {
        return systems;
    }

    /**
     * Configuration for Jira Zephyr.
     */
    public static class ZephyrConfig {
        private final Property<String> serverUrl;
        private final Property<String> apiKey;
        private final Property<String> projectKey;
        private final Property<String> testCycleName;
        private final Property<Boolean> createTestCases;
        private final Property<String> apiType;
        private final Property<String> apiVersion;
        private final Property<String> username;

        public ZephyrConfig(ObjectFactory objects) {
            this.serverUrl = objects.property(String.class);
            this.apiKey = objects.property(String.class);
            this.projectKey = objects.property(String.class);
            this.testCycleName = objects.property(String.class);
            this.createTestCases = objects.property(Boolean.class).convention(false);
            this.apiType = objects.property(String.class).convention("cloud");
            this.apiVersion = objects.property(String.class).convention("v1");
            this.username = objects.property(String.class);
        }

        public Property<String> getServerUrl() { return serverUrl; }
        public Property<String> getApiKey() { return apiKey; }
        public Property<String> getProjectKey() { return projectKey; }
        public Property<String> getTestCycleName() { return testCycleName; }
        public Property<Boolean> getCreateTestCases() { return createTestCases; }
        public Property<String> getApiType() { return apiType; }
        public Property<String> getApiVersion() { return apiVersion; }
        public Property<String> getUsername() { return username; }
    }

    /**
     * Configuration for TestRail.
     */
    public static class TestRailConfig {
        private final Property<String> serverUrl;
        private final Property<String> apiKey;
        private final Property<String> projectId;
        private final Property<String> suiteId;
        private final Property<Boolean> createTestCases;

        public TestRailConfig(ObjectFactory objects) {
            this.serverUrl = objects.property(String.class);
            this.apiKey = objects.property(String.class);
            this.projectId = objects.property(String.class);
            this.suiteId = objects.property(String.class);
            this.createTestCases = objects.property(Boolean.class).convention(false);
        }

        public Property<String> getServerUrl() { return serverUrl; }
        public Property<String> getApiKey() { return apiKey; }
        public Property<String> getProjectId() { return projectId; }
        public Property<String> getSuiteId() { return suiteId; }
        public Property<Boolean> getCreateTestCases() { return createTestCases; }
    }

    /**
     * Represents a configured test management system.
     */
    public static class TestManagementSystem {
        private final String type;
        private final Object config;

        public TestManagementSystem(String type, Object config) {
            this.type = type;
            this.config = config;
        }

        public String getType() { return type; }
        public Object getConfig() { return config; }
    }
} 