package io.github.mov2day.unifiedtest.intelligence;

import io.github.mov2day.unifiedtest.framework.FrameworkDetector;
import java.io.IOException;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.api.tasks.testing.Test;
import org.gradle.api.tasks.testing.junit.JUnitOptions;
import org.gradle.api.tasks.testing.junitplatform.JUnitPlatformOptions;
import org.gradle.api.tasks.testing.testng.TestNGOptions;
import org.gradle.testing.jacoco.tasks.JacocoReport;
import org.gradle.testing.jacoco.plugins.JacocoTaskExtension;

/** Registers opt-in JaCoCo profile and advisory impact tasks. */
public final class AutopilotTasks {
    private static final Pattern PACKAGE = Pattern.compile("(?m)^\\s*package\\s+([A-Za-z0-9_.]+)");
    private AutopilotTasks() {}

    public static void register(Project project, IntelligenceConfig config, String configuredFramework) {
        project.getPluginManager().apply("jacoco");
        List<CoverageMapStore.ProfileReport> reports = new ArrayList<>();
        List<Test> testTasks = project.getTasks().withType(Test.class).stream()
            .filter(task -> !task.getName().startsWith("unifiedTestProfile"))
            .toList();
        boolean completeCoverage = testTasks.stream().noneMatch(AutopilotTasks::hasMethodSelection);
        for (Test original : testTasks) {
            for (String testClass : discoverTestClasses(project, original)) {
                if (!isSelectedByFilter(original, testClass)) continue;
                String id = TestIdentity.shortHash(original.getName() + "|" + testClass);
                String profileName = "unifiedTestProfile" + id;
                String reportName = profileName + "Report";
                Path executionData = project.getLayout().getBuildDirectory().file("jacoco/unifiedtest-" + id + ".exec").get().getAsFile().toPath();
                Path testResults = project.getLayout().getBuildDirectory().dir("unifiedtest/intelligence/profile-results/" + id).get().getAsFile().toPath();
                TaskProvider<Test> profileTask = project.getTasks().register(profileName, Test.class,
                    task -> configureProfileTask(project, configuredFramework, original, task, testClass, executionData, testResults));
                Path xml = project.getLayout().getBuildDirectory().file("unifiedtest/intelligence/profiles/" + id + ".xml").get().getAsFile().toPath();
                TaskProvider<JacocoReport> reportTask = project.getTasks().register(reportName, JacocoReport.class, task -> {
                    task.setGroup("verification");
                    task.setDescription("Generates JaCoCo XML for " + testClass);
                    task.dependsOn(profileTask);
                    task.executionData(executionData.toFile());
                    SourceSet main = mainSourceSet(project);
                    if (main != null) {
                        task.getClassDirectories().from(main.getOutput().getClassesDirs());
                        task.getSourceDirectories().from(main.getAllJava().getSrcDirs());
                    }
                    task.getReports().getXml().getRequired().set(true);
                    task.getReports().getXml().getOutputLocation().set(project.getLayout().getBuildDirectory().file("unifiedtest/intelligence/profiles/" + id + ".xml"));
                    task.getReports().getHtml().getRequired().set(false);
                    task.getReports().getCsv().getRequired().set(false);
                });
                reports.add(new CoverageMapStore.ProfileReport(testClass, original.getName(), xml, testResults));
            }
        }
        TaskProvider<Task> profile = project.getTasks().register("unifiedTestProfileCoverage", task -> {
            task.setGroup("verification");
            task.setDescription("Profiles test-class coverage for UnifiedTest Autopilot.");
            task.dependsOn(project.getTasks().matching(candidate -> candidate.getName().startsWith("unifiedTestProfile") && candidate.getName().endsWith("Report")));
            task.doLast(ignored -> {
                Path state = stateDirectory(project);
                try {
                    new CoverageMapStore(state).write(reports, CoverageMapStore.inventoryFingerprint(project.getProjectDir().toPath()), completeCoverage);
                    project.getLogger().lifecycle("[UnifiedTest Autopilot] Coverage map refreshed with {} test classes.", reports.size());
                } catch (IOException e) {
                    throw new org.gradle.api.GradleException("UnifiedTest Autopilot coverage profile was incomplete: " + e.getMessage(), e);
                }
            });
        });
        project.getTasks().register("unifiedTestImpact", task -> {
            task.setGroup("verification");
            task.setDescription("Recommends impacted tests from the local UnifiedTest coverage map.");
            task.doLast(ignored -> {
                CoverageMapStore store = new CoverageMapStore(stateDirectory(project));
                GitImpactAnalyzer.ImpactResult result = GitImpactAnalyzer.analyze(project.getProjectDir().toPath(), config.getBaselineRef().get(), store.read());
                Path output = project.getLayout().getBuildDirectory().file("unifiedtest/intelligence/impact.json").get().getAsFile().toPath();
                try {
                    GitImpactAnalyzer.write(output, result);
                } catch (IOException e) {
                    throw new org.gradle.api.GradleException("Unable to write UnifiedTest impact report", e);
                }
                project.getLogger().lifecycle("[UnifiedTest Autopilot] {}: {}", result.decision(), result.reason());
                result.testsByTask().forEach((testTask, classes) -> project.getLogger().lifecycle("  ./gradlew {} {}", testTask,
                    classes.stream().sorted().map(testClass -> "--tests " + testClass).reduce("", (left, right) -> left + " " + right).trim()));
                project.getLogger().lifecycle("  Report: {}", output);
            });
        });
    }

    private static void configureProfileTask(Project project, String configuredFramework, Test original, Test task,
                                             String testClass, Path executionData, Path testResults) {
        task.setGroup("verification");
        task.setDescription("Profiles " + testClass + " for UnifiedTest Autopilot.");
        copyExecutionConfiguration(project, configuredFramework, original, task);
        task.setMaxParallelForks(1);
        task.getFilter().includeTestsMatching(testClass);
        task.getFilter().setExcludePatterns(original.getFilter().getExcludePatterns().toArray(String[]::new));
        task.getFilter().setFailOnNoMatchingTests(false);
        task.getExtensions().getByType(JacocoTaskExtension.class).setDestinationFile(executionData.toFile());
        task.getReports().getJunitXml().getOutputLocation().set(testResults.toFile());
    }

    private static void copyExecutionConfiguration(Project project, String configuredFramework, Test original, Test task) {
        task.setTestClassesDirs(original.getTestClassesDirs());
        task.setClasspath(original.getClasspath());
        task.setSystemProperties(original.getSystemProperties());
        task.setEnvironment(original.getEnvironment());
        task.setWorkingDir(original.getWorkingDir());
        if (original.getExecutable() != null) task.setExecutable(original.getExecutable());
        task.setBootstrapClasspath(original.getBootstrapClasspath());
        task.setJvmArgs(original.getJvmArgs().stream().filter(argument -> !isJaCoCoAgent(argument)).toList());
        task.getJvmArgumentProviders().addAll(original.getJvmArgumentProviders().stream()
            .filter(provider -> !provider.getClass().getName().toLowerCase(Locale.ROOT).contains("jacoco")).toList());
        task.setMinHeapSize(original.getMinHeapSize());
        task.setMaxHeapSize(original.getMaxHeapSize());
        task.setDefaultCharacterEncoding(original.getDefaultCharacterEncoding());
        task.setEnableAssertions(original.getEnableAssertions());
        task.setDebug(original.getDebug());
        task.setFailFast(original.getFailFast());
        task.setForkEvery(original.getForkEvery());
        task.setIncludes(original.getIncludes());
        task.setExcludes(original.getExcludes());
        task.setScanForTestClasses(original.isScanForTestClasses());
        task.getDryRun().set(original.getDryRun());
        task.getJavaLauncher().set(original.getJavaLauncher());
        copyFrameworkConfiguration(project, configuredFramework, original, task);
        // Include filters are applied before registration because Gradle combines them
        // as alternatives. Exclusions can be copied safely because they remain restrictive.
    }

    private static void copyFrameworkConfiguration(Project project, String configuredFramework, Test original, Test task) {
        String framework = effectiveFramework(project, configuredFramework);
        if ("JUnit5".equals(framework) || "Spock".equals(framework) || "Cucumber".equals(framework)) {
            if (original.getOptions() instanceof JUnitPlatformOptions options) {
                task.useJUnitPlatform(target -> target.copyFrom(options));
            } else {
                task.useJUnitPlatform();
            }
        } else if ("JUnit4".equals(framework)) {
            if (original.getOptions() instanceof JUnitOptions options) {
                task.useJUnit(target -> target.copyFrom(options));
            } else {
                task.useJUnit();
            }
        } else if ("TestNG".equals(framework)) {
            if (original.getOptions() instanceof TestNGOptions options) {
                task.useTestNG(target -> target.copyFrom(options));
            } else {
                task.useTestNG();
            }
        } else {
            throw new org.gradle.api.GradleException("UnifiedTest Autopilot cannot safely profile the unsupported test framework on task " + original.getPath());
        }
    }

    private static String effectiveFramework(Project project, String configuredFramework) {
        if (configuredFramework != null && !configuredFramework.isBlank()) {
            return switch (configuredFramework.trim().toLowerCase(Locale.ROOT)) {
                case "junit", "junit5", "junit-platform" -> "JUnit5";
                case "junit4" -> "JUnit4";
                case "testng" -> "TestNG";
                case "spock" -> "Spock";
                case "cucumber" -> "Cucumber";
                default -> configuredFramework.trim();
            };
        }
        List<String> detected = FrameworkDetector.detect(project);
        for (String candidate : List.of("Spock", "Cucumber", "JUnit4", "JUnit5", "TestNG")) {
            if (detected.contains(candidate)) return candidate;
        }
        throw new org.gradle.api.GradleException("UnifiedTest Autopilot cannot safely profile a test task without a supported framework");
    }

    private static boolean isJaCoCoAgent(String argument) {
        return argument != null
            && argument.startsWith("-javaagent:")
            && argument.toLowerCase(Locale.ROOT).contains("jacoco");
    }

    private static boolean isSelectedByFilter(Test task, String testClass) {
        Set<String> includes = task.getFilter().getIncludePatterns();
        if (includes.isEmpty()) return true;
        return includes.stream().anyMatch(pattern -> matchesFilter(pattern, testClass));
    }

    static boolean matchesFilter(String pattern, String testClass) {
        StringBuilder regex = new StringBuilder("^");
        for (int index = 0; index < pattern.length(); index++) {
            char character = pattern.charAt(index);
            if (character == '*') regex.append(".*");
            else if (character == '?') regex.append('.');
            else regex.append(java.util.regex.Pattern.quote(String.valueOf(character)));
        }
        regex.append('$');
        String simpleName = testClass.substring(testClass.lastIndexOf('.') + 1);
        return testClass.matches(regex.toString()) || simpleName.matches(regex.toString());
    }

    static boolean isMethodSelection(String pattern) {
        int lastSeparator = pattern.lastIndexOf('.');
        // Gradle filters do not expose whether a dotted pattern targets a class
        // or a method. Treat every non-package dotted selector as incomplete:
        // this includes wildcard method selectors such as "*.adds", "*.Adds",
        // and "*._adds" without relying on Java naming conventions. Class
        // matching remains independent so qualified class filters are still
        // profiled; they merely cannot make the map claim completeness.
        return lastSeparator >= 0 && !pattern.endsWith(".*");
    }

    private static boolean hasMethodSelection(Test task) {
        return task.getFilter().getIncludePatterns().stream().anyMatch(AutopilotTasks::isMethodSelection);
    }

    private static SourceSet mainSourceSet(Project project) {
        SourceSetContainer sourceSets = project.getExtensions().findByType(SourceSetContainer.class);
        return sourceSets == null ? null : sourceSets.findByName(SourceSet.MAIN_SOURCE_SET_NAME);
    }

    private static List<String> discoverTestClasses(Project project, Test testTask) {
        List<String> classes = new ArrayList<>();
        for (Path sourceRoot : testSourceRoots(project, testTask)) {
            try (var files = Files.walk(sourceRoot)) {
                files.filter(Files::isRegularFile)
                    .filter(file -> file.toString().endsWith(".java") || file.toString().endsWith(".groovy"))
                    .forEach(file -> className(sourceRoot, file).ifPresent(classes::add));
            } catch (IOException ignored) {
                // A missing source root means there is nothing to profile.
            }
        }
        return classes.stream().distinct().sorted().toList();
    }

    private static List<Path> testSourceRoots(Project project, Test testTask) {
        SourceSetContainer sourceSets = project.getExtensions().findByType(SourceSetContainer.class);
        if (sourceSets == null) return List.of();
        Set<Path> taskClasses = testTask.getTestClassesDirs().getFiles().stream()
            .map(File::toPath).map(path -> path.toAbsolutePath().normalize()).collect(java.util.stream.Collectors.toSet());
        Set<Path> roots = new LinkedHashSet<>();
        sourceSets.forEach(sourceSet -> {
            Set<Path> sourceSetClasses = sourceSet.getOutput().getClassesDirs().getFiles().stream()
                .map(File::toPath).map(path -> path.toAbsolutePath().normalize()).collect(java.util.stream.Collectors.toSet());
            if (sourceSetClasses.equals(taskClasses)) {
                sourceSet.getAllSource().getSrcDirs().stream().map(File::toPath)
                    .filter(Files::isDirectory).forEach(roots::add);
            }
        });
        return roots.stream().sorted().toList();
    }

    private static java.util.Optional<String> className(Path root, Path source) {
        try {
            String fileName = source.getFileName().toString();
            String simpleName = fileName.substring(0, fileName.lastIndexOf('.'));
            Matcher matcher = PACKAGE.matcher(Files.readString(source));
            return java.util.Optional.of((matcher.find() ? matcher.group(1) + "." : "") + simpleName);
        } catch (IOException e) {
            return java.util.Optional.empty();
        }
    }

    public static Path stateDirectory(Project project) {
        return project.getProjectDir().toPath().resolve(".gradle/unifiedtest/intelligence");
    }
}
