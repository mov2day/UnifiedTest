package io.github.mov2day.unifiedtest;

import org.gradle.testkit.runner.GradleRunner;
import org.gradle.testkit.runner.BuildResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.gradle.testkit.runner.TaskOutcome.SUCCESS;
import static org.gradle.testkit.runner.TaskOutcome.FAILED;
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
        assertTrue(!Files.exists(projectDir.resolve("build/unifiedtest/intelligence/dashboard.html")),
            "The Autopilot dashboard should remain opt-in with intelligence");
    }

    @Test
    void recordsAutopilotOutputsForAFailingTestTask() throws Exception {
        writeSettings();
        writeBuild("""
            plugins { id 'java'; id 'io.github.mov2day.unifiedtest' }
            repositories { mavenCentral() }
            dependencies { testImplementation 'org.junit.jupiter:junit-jupiter:5.10.2' }
            unifiedTest { intelligence { enabled = true } }
            """);
        write("src/test/java/example/FailingTest.java", """
            package example;
            import org.junit.jupiter.api.Test;
            import static org.junit.jupiter.api.Assertions.fail;
            class FailingTest { @Test void fails() { fail("expected failure"); } }
            """);

        BuildResult result = runGradleAndFail("test");

        assertEquals(FAILED, result.task(":test").getOutcome());
        assertTrue(Files.exists(projectDir.resolve("build/unifiedtest/intelligence/autopilot.json")));
        assertTrue(Files.readString(projectDir.resolve("build/unifiedtest/intelligence/autopilot.json")).contains("NEW_TEST"));
        assertTrue(Files.exists(projectDir.resolve(".gradle/unifiedtest/intelligence/runs.jsonl")));
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

    @Test
    void profilesCoverageAndRecommendsOnlyImpactedTests() throws Exception {
        writeSettings();
        writeBuild("""
            plugins {
                id 'java'
                id 'io.github.mov2day.unifiedtest'
            }

            repositories { mavenCentral() }
            dependencies { testImplementation 'org.junit.jupiter:junit-jupiter:5.10.2' }

            test {
                useJUnitPlatform { includeTags 'autopilot' }
                filter { includeTestsMatching 'CalculatorTest' }
                systemProperty 'autopilot.marker', 'preserved'
            }

            unifiedTest {
                framework = 'junit5'
                intelligence {
                    enabled = true
                    baselineRef = 'HEAD'
                }
            }
            """);
        write("src/main/java/example/Calculator.java", """
            package example;
            public class Calculator { public int add(int a, int b) { return a + b; } }
            """);
        write("src/main/java/example/Formatter.java", """
            package example;
            public class Formatter { public String value() { return "ok"; } }
            """);
        write("src/test/java/example/CalculatorTest.java", """
            package example;
            import org.junit.jupiter.api.Test;
            import org.junit.jupiter.api.Tag;
            import static org.junit.jupiter.api.Assertions.assertEquals;
            class CalculatorTest {
                @Test @Tag("autopilot") void adds() {
                    assertEquals("preserved", System.getProperty("autopilot.marker"));
                    assertEquals(3, new Calculator().add(1, 2));
                }
            }
            """);
        write("src/test/java/example/FormatterTest.java", """
            package example;
            import org.junit.jupiter.api.Test;
            import static org.junit.jupiter.api.Assertions.assertEquals;
            class FormatterTest { @Test void formats() { assertEquals("ok", new Formatter().value()); } }
            """);
        write("src/test/java/example/TestHelper.java", """
            package example;
            class TestHelper { String value() { return "helper"; } }
            """);

        runGit("init");
        runGit("add", ".");
        runGit("-c", "user.email=test@example.com", "-c", "user.name=Test", "commit", "-m", "baseline");
        BuildResult profile = runGradle("unifiedTestProfileCoverage");
        assertEquals(SUCCESS, profile.task(":unifiedTestProfileCoverage").getOutcome());
        Path map = projectDir.resolve(".gradle/unifiedtest/intelligence/coverage-map.json");
        assertTrue(Files.exists(map), "Profile should persist a local coverage map");
        assertTrue(Files.readString(map).contains("example.Calculator"));
        assertTrue(!Files.readString(map).contains("example.TestHelper"), "Non-runnable helper source must not become a selector");

        write("src/main/java/example/Calculator.java", """
            package example;
            public class Calculator { public int add(int a, int b) { return a + b + 0; } }
            """);
        write("src/test/java/example/CalculatorTest.java", """
            package example;
            import org.junit.jupiter.api.Test;
            import org.junit.jupiter.api.Tag;
            import static org.junit.jupiter.api.Assertions.assertEquals;
            class CalculatorTest {
                @Test @Tag("autopilot") void adds() {
                    assertEquals("preserved", System.getProperty("autopilot.marker"));
                    assertEquals(3, new Calculator().add(1, 2)); // body-only edit
                }
            }
            """);
        BuildResult impact = runGradle("unifiedTestImpact");
        assertEquals(SUCCESS, impact.task(":unifiedTestImpact").getOutcome());
        Path impactFile = projectDir.resolve("build/unifiedtest/intelligence/impact.json");
        String impactJson = Files.readString(impactFile);
        assertTrue(impactJson.contains("RUN_SELECTED_TESTS"));
        assertTrue(impactJson.contains("example.CalculatorTest"));
        assertTrue(!impactJson.contains("example.FormatterTest"));
    }

    @Test
    void generatesAutopilotHistoryDashboardForNormalTestTasks() throws Exception {
        writeSettings();
        writeBuild("""
            plugins {
                id 'java'
                id 'io.github.mov2day.unifiedtest'
            }
            repositories { mavenCentral() }
            dependencies { testImplementation 'org.junit.jupiter:junit-jupiter:5.10.2' }
            unifiedTest { intelligence { enabled = true } }
            """);
        write("src/test/java/example/DashboardTest.java", """
            package example;
            import org.junit.jupiter.api.Test;
            class DashboardTest { @Test void passes() {} }
            """);

        assertEquals(SUCCESS, runGradle("test").task(":test").getOutcome());
        Path dashboard = projectDir.resolve("build/unifiedtest/intelligence/dashboard.html");
        assertTrue(Files.exists(dashboard), "Autopilot should generate the local history dashboard");
        String content = Files.readString(dashboard);
        assertTrue(content.contains("<h1>Autopilot</h1>"));
        assertTrue(content.contains("role=\"tablist\""));
        assertTrue(content.contains(">JSON</a>"));
        assertTrue(Files.readString(projectDir.resolve(".gradle/unifiedtest/intelligence/runs.jsonl")).contains("DashboardTest"));
    }

    @Test
    void keepsCoverageSelectorsBoundToTheirOriginatingTestTasks() throws Exception {
        writeSettings();
        writeBuild("""
            plugins {
                id 'java'
                id 'io.github.mov2day.unifiedtest'
            }

            repositories { mavenCentral() }
            sourceSets {
                integrationTest {
                    java.srcDir 'src/integrationTest/java'
                    compileClasspath += sourceSets.main.output + configurations.testRuntimeClasspath
                    runtimeClasspath += output + compileClasspath
                }
            }
            configurations {
                integrationTestImplementation.extendsFrom testImplementation
                integrationTestRuntimeOnly.extendsFrom testRuntimeOnly
            }
            dependencies { testImplementation 'org.junit.jupiter:junit-jupiter:5.10.2' }
            tasks.register('integrationTest', Test) {
                testClassesDirs = sourceSets.integrationTest.output.classesDirs
                classpath = sourceSets.integrationTest.runtimeClasspath
                useJUnitPlatform()
            }
            unifiedTest { intelligence { enabled = true; baselineRef = 'HEAD' } }
            """);
        write("src/main/java/example/Calculator.java", """
            package example;
            public class Calculator { public int value() { return 1; } }
            """);
        write("src/test/java/example/CalculatorTest.java", """
            package example;
            import org.junit.jupiter.api.Test;
            import static org.junit.jupiter.api.Assertions.assertEquals;
            class CalculatorTest { @Test void unit() { assertEquals(1, new Calculator().value()); } }
            """);
        write("src/integrationTest/java/example/CalculatorIntegrationTest.java", """
            package example;
            import org.junit.jupiter.api.Test;
            import static org.junit.jupiter.api.Assertions.assertEquals;
            class CalculatorIntegrationTest { @Test void integration() { assertEquals(1, new Calculator().value()); } }
            """);
        runGit("init");
        runGit("add", ".");
        runGit("-c", "user.email=test@example.com", "-c", "user.name=Test", "commit", "-m", "baseline");

        assertEquals(SUCCESS, runGradle("unifiedTestProfileCoverage").task(":unifiedTestProfileCoverage").getOutcome());
        write("src/main/java/example/Calculator.java", """
            package example;
            public class Calculator { public int value() { return 2; } }
            """);
        assertEquals(SUCCESS, runGradle("unifiedTestImpact").task(":unifiedTestImpact").getOutcome());
        String impact = Files.readString(projectDir.resolve("build/unifiedtest/intelligence/impact.json"));
        assertTrue(impact.contains("example.CalculatorTest"));
        assertTrue(impact.contains("example.CalculatorIntegrationTest"));
        assertTrue(impact.contains("\"test\""));
        assertTrue(impact.contains("\"integrationTest\""));
    }

    @Test
    void fallsBackToTheFullSuiteForWildcardMethodFilteredSharedCoverage() throws Exception {
        writeSettings();
        writeBuild("""
            plugins {
                id 'java'
                id 'io.github.mov2day.unifiedtest'
            }

            repositories { mavenCentral() }
            sourceSets {
                integrationTest {
                    java.srcDir 'src/integrationTest/java'
                    compileClasspath += sourceSets.main.output + configurations.testRuntimeClasspath
                    runtimeClasspath += output + compileClasspath
                }
            }
            configurations {
                integrationTestImplementation.extendsFrom testImplementation
                integrationTestRuntimeOnly.extendsFrom testRuntimeOnly
            }
            dependencies { testImplementation 'org.junit.jupiter:junit-jupiter:5.10.2' }
            tasks.register('integrationTest', Test) {
                testClassesDirs = sourceSets.integrationTest.output.classesDirs
                classpath = sourceSets.integrationTest.runtimeClasspath
                useJUnitPlatform()
                filter { includeTestsMatching '*.adds' }
            }
            unifiedTest { intelligence { enabled = true; baselineRef = 'HEAD' } }
            """);
        write("src/main/java/example/Calculator.java", """
            package example;
            public class Calculator { public int add(int a, int b) { return a + b; } }
            """);
        write("src/test/java/example/CalculatorTest.java", """
            package example;
            import org.junit.jupiter.api.Test;
            import static org.junit.jupiter.api.Assertions.assertEquals;
            class CalculatorTest { @Test void unit() { assertEquals(3, new Calculator().add(1, 2)); } }
            """);
        write("src/integrationTest/java/example/CalculatorIntegrationTest.java", """
            package example;
            import org.junit.jupiter.api.Test;
            import static org.junit.jupiter.api.Assertions.assertEquals;
            class CalculatorIntegrationTest { @Test void adds() { assertEquals(3, new Calculator().add(1, 2)); } }
            """);
        runGit("init");
        runGit("add", ".");
        runGit("-c", "user.email=test@example.com", "-c", "user.name=Test", "commit", "-m", "baseline");

        assertEquals(SUCCESS, runGradle("unifiedTestProfileCoverage").task(":unifiedTestProfileCoverage").getOutcome());
        String coverageMap = Files.readString(projectDir.resolve(".gradle/unifiedtest/intelligence/coverage-map.json"));
        assertTrue(coverageMap.contains("\"completeCoverage\": false"),
            "The real wildcard method filter must make class-level coverage incomplete");
        write("src/main/java/example/Calculator.java", """
            package example;
            public class Calculator { public int add(int a, int b) { return a + b + 0; } }
            """);

        assertEquals(SUCCESS, runGradle("unifiedTestImpact").task(":unifiedTestImpact").getOutcome());
        String impact = Files.readString(projectDir.resolve("build/unifiedtest/intelligence/impact.json"));
        assertTrue(impact.contains("RUN_FULL_SUITE"));
        assertTrue(impact.contains("INCOMPLETE_COVERAGE_MAP"));
    }

    private BuildResult runGradle(String... args) {
        return GradleRunner.create()
            .withProjectDir(projectDir.toFile())
            .withPluginClasspath()
            .withArguments(args)
            .forwardOutput()
            .build();
    }

    private BuildResult runGradleAndFail(String... args) {
        return GradleRunner.create()
            .withProjectDir(projectDir.toFile())
            .withPluginClasspath()
            .withArguments(args)
            .forwardOutput()
            .buildAndFail();
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

    private void runGit(String... args) throws Exception {
        java.util.List<String> command = new java.util.ArrayList<>();
        command.add("git");
        java.util.Collections.addAll(command, args);
        Process process = new ProcessBuilder(command).directory(projectDir.toFile()).redirectErrorStream(true).start();
        assertEquals(0, process.waitFor(), new String(process.getInputStream().readAllBytes()));
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
