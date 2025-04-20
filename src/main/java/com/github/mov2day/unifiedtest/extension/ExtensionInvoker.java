package com.github.mov2day.unifiedtest.agent.extension;

import com.github.mov2day.unifiedtest.agent.UnifiedTestExtension;
import org.gradle.api.Project;
import org.gradle.api.tasks.testing.Test;
import java.util.ServiceLoader;

public class ExtensionInvoker {
    public static void invoke(Project project, Test testTask) {
        ServiceLoader<UnifiedTestExtension> loader = ServiceLoader.load(UnifiedTestExtension.class);
        for (UnifiedTestExtension ext : loader) {
            ext.afterTestExecution(project, testTask);
        }
    }
}
