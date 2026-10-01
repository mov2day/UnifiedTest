package io.github.mov2day.unifiedtest.intelligence;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/** Persists the trusted, local class-to-test coverage graph. */
public final class CoverageMapStore {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private final Path mapFile;

    public CoverageMapStore(Path stateDirectory) {
        this.mapFile = stateDirectory.resolve("coverage-map.json");
    }

    public void write(List<ProfileReport> reports, String inventoryFingerprint) throws IOException {
        write(reports, inventoryFingerprint, true);
    }

    public void write(List<ProfileReport> reports, String inventoryFingerprint, boolean completeCoverage) throws IOException {
        Map<String, Set<String>> productionToTests = new LinkedHashMap<>();
        Map<String, Set<String>> testTasksByClass = new LinkedHashMap<>();
        for (ProfileReport report : reports) {
            // A source file is not necessarily a runnable test (for example, a Cucumber step
            // definition or a shared fixture). Only let a profile contribute edges or a selector
            // after Gradle recorded at least one test execution for it.
            if (!executedTest(report.testResultsDirectory())) continue;
            if (!Files.exists(report.xmlFile())) throw new IOException("Missing JaCoCo profile: " + report.xmlFile());
            for (String productionClass : coveredClasses(report.xmlFile())) {
                productionToTests.computeIfAbsent(productionClass, ignored -> new LinkedHashSet<>()).add(report.testClass());
            }
            testTasksByClass.computeIfAbsent(report.testClass(), ignored -> new LinkedHashSet<>()).add(report.testTaskName());
        }
        JsonObject root = new JsonObject();
        root.addProperty("schemaVersion", 3);
        root.addProperty("inventoryFingerprint", inventoryFingerprint);
        root.addProperty("completeCoverage", completeCoverage);
        JsonObject edges = new JsonObject();
        productionToTests.forEach((production, tests) -> {
            JsonArray values = new JsonArray();
            tests.forEach(values::add);
            edges.add(production, values);
        });
        root.add("productionToTests", edges);
        JsonObject tasks = new JsonObject();
        testTasksByClass.forEach((testClass, taskNames) -> {
            JsonArray values = new JsonArray();
            taskNames.forEach(values::add);
            tasks.add(testClass, values);
        });
        root.add("testTasksByClass", tasks);
        Files.createDirectories(mapFile.getParent());
        Files.writeString(mapFile, GSON.toJson(root), StandardCharsets.UTF_8);
    }

    public CoverageMap read() {
        if (!Files.exists(mapFile)) return null;
        try {
            JsonObject root = GSON.fromJson(Files.readString(mapFile, StandardCharsets.UTF_8), JsonObject.class);
            if (!root.has("schemaVersion") || root.get("schemaVersion").getAsInt() != 3) return null;
            Map<String, Set<String>> edges = new LinkedHashMap<>();
            root.getAsJsonObject("productionToTests").entrySet().forEach(entry -> {
                Set<String> tests = new LinkedHashSet<>();
                for (JsonElement test : entry.getValue().getAsJsonArray()) tests.add(test.getAsString());
                edges.put(entry.getKey(), tests);
            });
            Map<String, Set<String>> tasks = new LinkedHashMap<>();
            JsonObject taskValues = root.getAsJsonObject("testTasksByClass");
            taskValues.entrySet().forEach(entry -> {
                Set<String> names = new LinkedHashSet<>();
                if (entry.getValue().isJsonArray()) {
                    entry.getValue().getAsJsonArray().forEach(value -> names.add(value.getAsString()));
                } else {
                    names.add(entry.getValue().getAsString());
                }
                tasks.put(entry.getKey(), names);
            });
            return new CoverageMap(root.get("inventoryFingerprint").getAsString(), edges, tasks,
                root.has("completeCoverage") && root.get("completeCoverage").getAsBoolean());
        } catch (IOException | RuntimeException e) {
            return null;
        }
    }

    public static Set<String> coveredClasses(Path xmlFile) throws IOException {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            // JaCoCo XML declares a local report.dtd. It is not needed for this data-only parse
            // and resolving it makes local analysis depend on the report task's working directory.
            factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            factory.setFeature("http://xml.org/sax/features/validation", false);
            NodeList classes = factory.newDocumentBuilder().parse(xmlFile.toFile()).getElementsByTagName("class");
            Set<String> covered = new LinkedHashSet<>();
            for (int i = 0; i < classes.getLength(); i++) {
                Element classElement = (Element) classes.item(i);
                NodeList counters = classElement.getElementsByTagName("counter");
                for (int j = 0; j < counters.getLength(); j++) {
                    Element counter = (Element) counters.item(j);
                    if ("LINE".equals(counter.getAttribute("type")) && Integer.parseInt(counter.getAttribute("covered")) > 0) {
                        covered.add(classElement.getAttribute("name").replace('/', '.'));
                        break;
                    }
                }
            }
            return covered;
        } catch (Exception e) {
            throw new IOException("Unable to parse JaCoCo XML " + xmlFile, e);
        }
    }

    public static String inventoryFingerprint(Path projectDirectory) {
        try {
            List<String> entries = new ArrayList<>();
            for (String root : List.of("src/test/java", "src/test/groovy")) {
                Path path = projectDirectory.resolve(root);
                if (Files.isDirectory(path)) {
                    try (var stream = Files.walk(path)) {
                        entries.addAll(stream.filter(Files::isRegularFile)
                            .filter(file -> file.toString().endsWith(".java") || file.toString().endsWith(".groovy"))
                            .map(file -> testClassInventoryEntry(projectDirectory, path, file)).toList());
                    }
                }
            }
            for (String buildFile : List.of("build.gradle", "build.gradle.kts", "settings.gradle", "settings.gradle.kts")) {
                Path path = projectDirectory.resolve(buildFile);
                if (Files.isRegularFile(path)) entries.add(projectDirectory.relativize(path) + ":" + Files.getLastModifiedTime(path).toMillis() + ":" + Files.size(path));
            }
            Collections.sort(entries);
            return TestIdentity.shortHash(String.join("|", entries));
        } catch (IOException e) {
            return "unavailable";
        }
    }

    private static boolean executedTest(Path testResultsDirectory) {
        if (!Files.isDirectory(testResultsDirectory)) return false;
        try (var files = Files.walk(testResultsDirectory)) {
            return files.filter(file -> file.toString().endsWith(".xml")).anyMatch(CoverageMapStore::containsTestCase);
        } catch (IOException e) {
            return false;
        }
    }

    private static boolean containsTestCase(Path xmlFile) {
        try {
            return DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(xmlFile.toFile())
                .getElementsByTagName("testcase").getLength() > 0;
        } catch (Exception e) {
            return false;
        }
    }

    private static String testClassInventoryEntry(Path projectDirectory, Path root, Path source) {
        try {
            String simpleName = source.getFileName().toString().replaceFirst("\\.[^.]+$", "");
            String packageName = Files.readString(source).lines()
                .map(String::trim)
                .filter(line -> line.startsWith("package "))
                .map(line -> line.substring("package ".length()).replaceAll("[;\\s].*", ""))
                .findFirst().orElse("");
            return projectDirectory.relativize(root) + ":" + (packageName.isEmpty() ? simpleName : packageName + "." + simpleName);
        } catch (IOException e) {
            return projectDirectory.relativize(source).toString();
        }
    }

    public record ProfileReport(String testClass, String testTaskName, Path xmlFile, Path testResultsDirectory) {}
    public record CoverageMap(String inventoryFingerprint, Map<String, Set<String>> productionToTests,
                              Map<String, Set<String>> testTasksByClass, boolean completeCoverage) {}
}
