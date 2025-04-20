package io.github.mov2day.unifiedtest.framework;

import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import java.util.HashSet;
import java.util.Set;

/**
 * Detects supported test frameworks (JUnit4, JUnit5, TestNG) from project dependencies.
 */
public class FrameworkDetector {
    /**
     * Returns a set of detected test frameworks for the given project.
     * @param project the Gradle project
     * @return set of framework names (e.g., JUnit4, JUnit5, TestNG)
     */
    public static Set<String> detect(Project project) {
        Set<String> frameworks = new HashSet<>();
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
