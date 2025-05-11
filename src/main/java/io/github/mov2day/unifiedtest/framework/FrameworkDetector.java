package io.github.mov2day.unifiedtest.framework;

import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;

import java.util.ArrayList;
import java.util.List;

/**
 * Utility class for detecting test frameworks in a Gradle project.
 * Analyzes project dependencies to determine which test frameworks are present.
 */
public class FrameworkDetector {
    /**
     * Detects test frameworks present in the project's dependencies.
     * @param project the Gradle project to analyze
     * @return list of detected test framework names
     */
    public static List<String> detect(Project project) {
        List<String> frameworks = new ArrayList<>();
        Configuration testImplementation = project.getConfigurations().findByName("testImplementation");
        if (testImplementation != null) {
            testImplementation.getAllDependencies().forEach(dep -> {
                String group = dep.getGroup() != null ? dep.getGroup() : "";
                String name = dep.getName() != null ? dep.getName() : "";
                if (group.equals("junit") && name.equals("junit")) {
                    frameworks.add("JUnit4");
                } else if (group.equals("org.junit.jupiter")) {
                    frameworks.add("JUnit5");
                } else if (group.equals("org.testng")) {
                    frameworks.add("TestNG");
                }
            });
        }
        return frameworks;
    }
}
