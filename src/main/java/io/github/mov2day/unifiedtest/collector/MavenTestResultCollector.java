package io.github.mov2day.unifiedtest.collector;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Maven-compatible test result collector.
 * This implementation has no Gradle dependencies, making it suitable for Maven projects.
 */
public class MavenTestResultCollector implements ITestResultCollector {
    private final List<UnifiedTestResult> results = new ArrayList<>();
    private final Map<String, UnifiedTestResult> resultMap = new ConcurrentHashMap<>();
    
    /**
     * Default constructor required for ServiceLoader.
     */
    public MavenTestResultCollector() {
        // Initialize with default settings
        System.out.println("UnifiedTest: MavenTestResultCollector created");
    }

    @Override
    public void addResult(UnifiedTestResult result) {
        String key = result.className + "." + result.testName;
        if (!resultMap.containsKey(key)) {
            resultMap.put(key, result);
            results.add(result);
            System.out.println("UnifiedTest: Added result for test: " + key + " with status: " + result.status);
        }
    }

    @Override
    public List<UnifiedTestResult> getResults() {
        return new ArrayList<>(results);
    }

    @Override
    public boolean hasResult(String className, String testName) {
        return resultMap.containsKey(className + "." + testName);
    }
} 