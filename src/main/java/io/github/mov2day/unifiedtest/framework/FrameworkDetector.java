package io.github.mov2day.unifiedtest.framework;

import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;

import java.util.List;
import java.util.LinkedHashSet;
import java.util.Set;

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
        Set<String> frameworks = new LinkedHashSet<>();
        List<String> configurationNames = List.of("testImplementation", "testRuntimeOnly", "testCompileOnly", "implementation");
        for (String configurationName : configurationNames) {
            Configuration configuration = project.getConfigurations().findByName(configurationName);
            if (configuration == null) {
                continue;
            }
            configuration.getAllDependencies().forEach(dep -> {
                String group = dep.getGroup() != null ? dep.getGroup() : "";
                String name = dep.getName() != null ? dep.getName() : "";
                if (group.equals("junit") && name.equals("junit")) {
                    frameworks.add("JUnit4");
                } else if (group.equals("org.junit.jupiter")) {
                    frameworks.add("JUnit5");
                } else if (group.equals("org.testng")) {
                    frameworks.add("TestNG");
                } else if (group.equals("org.spockframework") || name.startsWith("spock-")) {
                    frameworks.add("Spock");
                } else if (group.equals("io.cucumber") || name.startsWith("cucumber-")) {
                    frameworks.add("Cucumber");
                }
            });
        }
        return List.copyOf(frameworks);
    }
}
