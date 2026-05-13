package io.github.mov2day.unifiedtest;

import org.gradle.testkit.runner.GradleRunner;
import org.gradle.testkit.runner.BuildResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.gradle.testkit.runner.TaskOutcome.SUCCESS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UnifiedTestPluginFunctionalTest {
    @TempDir
    Path projectDir;

    @Test
    void generatesReportsForJUnit5Project() throws Exception {
        writeSettings();
        writeBuild("""
            plugins {
                id 'java'
                id 'io.github.mov2day.unifiedtest'
            }

            repositories { mavenCentral() }

            dependencies {
                testImplementation 'org.junit.jupiter:junit-jupiter:5.10.2'
            }

            unifiedTest {
                theme = 'minimal'
                telemetry {
                    serviceName = 'junit5-service'
                }
            }
            """);
        write("src/test/java/example/SampleTest.java", """
            package example;
            import org.junit.jupiter.api.Test;
            import static org.junit.jupiter.api.Assertions.assertTrue;
            class SampleTest {
                @Test void passes() { assertTrue(true); }
            }
            """);

        BuildResult result = runGradle("test");

        assertEquals(SUCCESS, result.task(":test").getOutcome());
        assertGeneratedReportsContain("junit5-service", "SampleTest");
    }

    @Test
    void detectsSpockProjectAndGeneratesReports() throws Exception {
        writeSettings();
        writeBuild("""
            plugins {
                id 'groovy'
                id 'io.github.mov2day.unifiedtest'
            }

            repositories { mavenCentral() }

            dependencies {
                testImplementation localGroovy()
                testImplementation 'org.spockframework:spock-core:2.3-groovy-3.0'
            }
            """);
        write("src/test/groovy/example/SampleSpec.groovy", """
            package example
            import spock.lang.Specification
            class SampleSpec extends Specification {
                def "adds numbers"() {
                    expect:
                    1 + 1 == 2
                }
            }
            """);

        BuildResult result = runGradle("test");

        assertEquals(SUCCESS, result.task(":test").getOutcome());
        assertGeneratedReportsContain("Spock", "SampleSpec");
    }

    @Test
    void detectsCucumberProjectAndGeneratesReports() throws Exception {
        writeSettings();
        writeBuild("""
            plugins {
                id 'java'
                id 'io.github.mov2day.unifiedtest'
            }

            repositories { mavenCentral() }

            dependencies {
                testImplementation 'io.cucumber:cucumber-java:7.20.1'
                testImplementation 'io.cucumber:cucumber-junit-platform-engine:7.20.1'
                testImplementation 'org.junit.platform:junit-platform-suite:1.10.2'
            }

            test {
                useJUnitPlatform()
            }
            """);
        write("src/test/resources/features/sample.feature", """
            Feature: sample
              Scenario: happy path
                Given a passing step
            """);
        write("src/test/resources/junit-platform.properties", "cucumber.glue=example\n");
        write("src/test/java/example/RunCucumberTest.java", """
            package example;
            import org.junit.platform.suite.api.ConfigurationParameter;
            import org.junit.platform.suite.api.IncludeEngines;
            import org.junit.platform.suite.api.SelectClasspathResource;
            import org.junit.platform.suite.api.Suite;
            import static io.cucumber.junit.platform.engine.Constants.GLUE_PROPERTY_NAME;
            @Suite
            @IncludeEngines("cucumber")
            @SelectClasspathResource("features")
            @ConfigurationParameter(key = GLUE_PROPERTY_NAME, value = "example")
            public class RunCucumberTest {}
            """);
        write("src/test/java/example/Steps.java", """
            package example;
            import io.cucumber.java.en.Given;
            public class Steps {
                @Given("a passing step")
                public void passingStep() {}
            }
            """);

        BuildResult result = runGradle("test");

        assertEquals(SUCCESS, result.task(":test").getOutcome());
        assertGeneratedReportsContain("Cucumber", "happy path");
    }

    private BuildResult runGradle(String... args) {
        return GradleRunner.create()
            .withProjectDir(projectDir.toFile())
            .withPluginClasspath()
            .withArguments(args)
            .forwardOutput()
            .build();
    }

    private void writeSettings() throws Exception {
        write("settings.gradle", "rootProject.name = 'sample-service'\n");
    }

    private void writeBuild(String body) throws Exception {
        write("build.gradle", body);
    }

    private void write(String relativePath, String content) throws Exception {
        Path path = projectDir.resolve(relativePath);
        Files.createDirectories(path.getParent());
        Files.writeString(path, content);
    }

    private void assertGeneratedReportsContain(String first, String second) throws Exception {
        File html = projectDir.resolve("build/unifiedtest/reports/index.html").toFile();
        File json = projectDir.resolve("build/unifiedtest/reports/results.json").toFile();
        File dashboard = projectDir.resolve("build/unifiedtest/dashboard/grafana-dashboard.json").toFile();

        assertTrue(html.exists(), "HTML report should exist");
        assertTrue(json.exists(), "JSON report should exist");
        assertTrue(dashboard.exists(), "Dashboard JSON should exist");

        String combined = Files.readString(html.toPath()) + Files.readString(json.toPath()) + Files.readString(dashboard.toPath());
        assertTrue(combined.contains(first), "Reports should contain " + first);
        assertTrue(combined.contains(second), "Reports should contain " + second);
    }
}
