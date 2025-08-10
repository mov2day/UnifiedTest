package io.github.mov2day.unifiedtest.framework;

import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ConfigurationContainer;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.DependencySet;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class FrameworkDetectorTest {

    @Mock
    private Project project;
    @Mock
    private ConfigurationContainer configurations;
    @Mock
    private Configuration testConfiguration;
    @Mock
    private DependencySet dependencySet;

    @Test
    void testDetectJUnit5() {
        when(project.getConfigurations()).thenReturn(configurations);
        when(configurations.stream()).thenReturn(Collections.singletonList(testConfiguration).stream());
        when(testConfiguration.getName()).thenReturn("testImplementation");
        when(testConfiguration.getAllDependencies()).thenReturn(dependencySet);
        Dependency jupiterDep = mock(Dependency.class);
        when(jupiterDep.getGroup()).thenReturn("org.junit.jupiter");
        when(dependencySet.stream()).thenReturn(Stream.of(jupiterDep));

        List<String> frameworks = FrameworkDetector.detect(project);
        assertTrue(frameworks.contains("JUnit5"));
    }

    @Test
    void testDetectSpock() {
        when(project.getConfigurations()).thenReturn(configurations);
        when(configurations.stream()).thenReturn(Collections.singletonList(testConfiguration).stream());
        when(testConfiguration.getName()).thenReturn("testImplementation");
        when(testConfiguration.getAllDependencies()).thenReturn(dependencySet);
        Dependency spockDep = mock(Dependency.class);
        when(spockDep.getGroup()).thenReturn("org.spockframework");
        when(dependencySet.stream()).thenReturn(Stream.of(spockDep));

        List<String> frameworks = FrameworkDetector.detect(project);
        assertTrue(frameworks.contains("Spock"));
    }

    @Test
    void testGetAdapters() {
        List<TestFrameworkAdapter> adapters = FrameworkDetector.getAdapters();
        assertEquals(5, adapters.size());
        assertTrue(adapters.stream().anyMatch(a -> a instanceof JUnit4Adapter));
        assertTrue(adapters.stream().anyMatch(a -> a instanceof JUnit5Adapter));
        assertTrue(adapters.stream().anyMatch(a -> a instanceof TestNGAdapter));
        assertTrue(adapters.stream().anyMatch(a -> a instanceof SpockAdapter));
        assertTrue(adapters.stream().anyMatch(a -> a instanceof CucumberAdapter));
    }
}
