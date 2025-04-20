package com.github.mov2day.unifiedtest.framework;

import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ConfigurationContainer;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.DependencySet;
import org.junit.jupiter.api.Test;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class FrameworkDetectorTest {
    @Test
    void detectsJUnit4() {
        Project project = mock(Project.class);
        ConfigurationContainer configurations = mock(ConfigurationContainer.class);
        Configuration config = mock(Configuration.class);
        DependencySet depSet = mock(DependencySet.class);
        Dependency dep = mock(Dependency.class);
        when(dep.getGroup()).thenReturn("junit");
        when(dep.getName()).thenReturn("junit");
        when(depSet.iterator()).thenReturn(java.util.Collections.singleton(dep).iterator());
        when(config.getAllDependencies()).thenReturn(depSet);
        when(configurations.findByName("testImplementation")).thenReturn(config);
        when(project.getConfigurations()).thenReturn(configurations);
        Set<String> frameworks = FrameworkDetector.detect(project);
        assertTrue(frameworks.contains("JUnit4"));
    }
}
