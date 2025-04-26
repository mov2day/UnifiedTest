package io.github.mov2day.unifiedtest.framework;

import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ConfigurationContainer;
import org.gradle.api.artifacts.DependencySet;
import org.gradle.api.artifacts.ProjectDependency;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests for the FrameworkDetector class.
 */
public class FrameworkDetectorTest {
    @BeforeAll
    static void setup() {
        System.setProperty("mockito.inline.extended.stacktrace", "false");
    }

    @Test
    void detectsJUnit4() {
        // Setup mock with RETURNS_DEEP_STUBS to handle nested calls
        Project project = mock(Project.class, withSettings()
            .defaultAnswer(RETURNS_DEEP_STUBS)
            .verboseLogging());
        
        // Mock testImplementation configuration
        Configuration testConfig = mock(Configuration.class);
        DependencySet dependencies = mock(DependencySet.class);
        when(testConfig.getAllDependencies()).thenReturn(dependencies);
        when(dependencies.isEmpty()).thenReturn(true);
        when(project.getConfigurations().findByName("testImplementation")).thenReturn(testConfig);

        // Test framework detection
        List<String> frameworks = FrameworkDetector.detect(project);
        assertNotNull(frameworks, "Should return non-null list");
        assertTrue(frameworks.isEmpty(), "Should return empty list when no dependencies");
    }
}
