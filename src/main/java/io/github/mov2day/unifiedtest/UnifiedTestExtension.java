package io.github.mov2day.unifiedtest;

import org.gradle.api.Project;
import org.gradle.api.tasks.testing.Test;

/**
 * Interface for UnifiedTest extensions that can be invoked after test execution.
 * Implementations can provide additional functionality like custom reporting or data processing.
 */
public interface UnifiedTestExtension {
    /**
     * Called after test execution completes.
     * Implementations can perform post-test processing like generating reports or exporting data.
     * @param project the Gradle project
     * @param testTask the test task that was executed
     */
    void afterTestExecution(Project project, Test testTask);
}