package io.github.mov2day.unifiedtest.reporting.testmanagement;

import io.github.mov2day.unifiedtest.extension.TestManagementExtension;
import java.util.HashMap;
import java.util.Map;
import java.util.List;

/**
 * Factory class for creating and managing test management system instances.
 */
public class TestManagementSystemFactory {
    private final Map<String, TestManagementSystem> systems;

    public TestManagementSystemFactory() {
        this.systems = new HashMap<>();
    }

    /**
     * Initialize test management systems from the extension configuration.
     * @param extension TestManagementExtension containing the configuration
     */
    public void initialize(TestManagementExtension extension) {
        if (!extension.getEnabled().get()) {
            return;
        }

        for (TestManagementExtension.TestManagementSystem config : extension.getSystems()) {
            TestManagementSystem system = createSystem(config.getType());
            if (system != null) {
                system.initialize(config.getConfig());
                systems.put(config.getType(), system);
            }
        }
    }

    /**
     * Get a test management system by type.
     * @param type System type (e.g., "zephyr", "testrail")
     * @return TestManagementSystem instance or null if not found
     */
    public TestManagementSystem getSystem(String type) {
        return systems.get(type);
    }

    /**
     * Get all configured test management systems.
     * @return List of configured test management systems
     */
    public List<TestManagementSystem> getAllSystems() {
        return List.copyOf(systems.values());
    }

    private TestManagementSystem createSystem(String type) {
        switch (type.toLowerCase()) {
            case "zephyr":
                return new ZephyrTestManagementSystem();
            case "testrail":
                return new TestRailManagementSystem();
            default:
                return null;
        }
    }
} 