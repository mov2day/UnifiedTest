package io.github.mov2day.unifiedtest.extension;

import io.github.mov2day.unifiedtest.UnifiedTestExtension;
import org.gradle.api.Project;
import org.gradle.api.tasks.testing.Test;
import java.util.ServiceLoader;

/**
 * Handles the invocation of UnifiedTest extensions.
 * Provides functionality to register and execute test extensions during the test lifecycle.
 */
public class ExtensionInvoker {
    /**
     * Invokes afterTestExecution for all UnifiedTestExtension implementations.
     * @param project the Gradle project
     * @param testTask the test task
     */
    public static void invoke(Project project, Test testTask) {
        ServiceLoader<UnifiedTestExtension> loader = ServiceLoader.load(UnifiedTestExtension.class);
        for (UnifiedTestExtension ext : loader) {
            ext.afterTestExecution(project, testTask);
        }
    }
}
