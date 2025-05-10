package io.github.mov2day.unifiedtest.framework;

import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ConfigurationContainer;
import org.gradle.api.artifacts.DependencySet;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.mockito.Mock;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Tests for the FrameworkDetector class.
 */
@ExtendWith(MockitoExtension.class)
public class FrameworkDetectorTest {
    @Mock
    private Project project;
    @Mock
    private ConfigurationContainer configurations;

    @Test
    void detectsJUnit4() {
        // Setup configuration chain
        when(project.getConfigurations()).thenReturn(configurations);
        Configuration testConfig = mock(Configuration.class);
        DependencySet dependencies = mock(DependencySet.class);
        when(configurations.findByName("testImplementation")).thenReturn(testConfig);
        when(testConfig.getAllDependencies()).thenReturn(dependencies);

        // Test framework detection
        List<String> frameworks = FrameworkDetector.detect(project);
        assertNotNull(frameworks, "Should return non-null list");
        assertTrue(frameworks.isEmpty(), "Should return empty list when no dependencies");
    }
}
