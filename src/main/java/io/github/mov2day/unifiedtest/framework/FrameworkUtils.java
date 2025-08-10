package io.github.mov2day.unifiedtest.framework;

import io.github.mov2day.unifiedtest.UnifiedTestAgentPlugin;
import org.gradle.api.Project;

public class FrameworkUtils {

    public static String getThemeFromConfig(Project project) {
        try {
            return project.getExtensions().getByType(UnifiedTestAgentPlugin.UnifiedTestExtensionConfig.class).getTheme().get();
        } catch (Exception e) {
            return "standard"; // Default fallback
        }
    }
}
