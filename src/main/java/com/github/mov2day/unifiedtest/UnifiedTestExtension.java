package com.github.mov2day.unifiedtest.agent;

import org.gradle.api.Project;
import org.gradle.api.tasks.testing.Test;

/**
 * Extension point for UnifiedTest plugin.
 * Users can implement this interface to add custom reporting or observability logic.
 */
public interface UnifiedTestExtension {
    void afterTestExecution(Project project, Test testTask);
}