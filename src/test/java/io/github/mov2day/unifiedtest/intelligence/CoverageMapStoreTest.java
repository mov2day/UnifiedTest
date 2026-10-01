package io.github.mov2day.unifiedtest.intelligence;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CoverageMapStoreTest {
    @TempDir Path tempDir;

    @Test
    void storesOnlyCoveredProductionClasses() throws Exception {
        Path xml = tempDir.resolve("profile.xml");
        Files.writeString(xml, """
            <report><package name="example">
              <class name="example/Covered"><counter type="LINE" missed="0" covered="3"/></class>
              <class name="example/Uncovered"><counter type="LINE" missed="3" covered="0"/></class>
            </package></report>
        """);
        CoverageMapStore store = new CoverageMapStore(tempDir.resolve("state"));
        Path results = tempDir.resolve("results");
        Files.createDirectories(results);
        Files.writeString(results.resolve("TEST-example.CoveredTest.xml"), "<testsuite><testcase name=\"covers\"/></testsuite>");
        store.write(List.of(new CoverageMapStore.ProfileReport("example.CoveredTest", "test", xml, results)), "inventory");
        CoverageMapStore.CoverageMap map = store.read();
        assertTrue(map.productionToTests().containsKey("example.Covered"));
        assertTrue(!map.productionToTests().containsKey("example.Uncovered"));
    }

    @Test
    void excludesProfilesThatDidNotExecuteATestAndIgnoresTestBodyEditsInTheManifest() throws Exception {
        Path xml = tempDir.resolve("profile.xml");
        Files.writeString(xml, "<report><package name=\"example\"><class name=\"example/Covered\"><counter type=\"LINE\" missed=\"0\" covered=\"1\"/></class></package></report>");
        Path executed = tempDir.resolve("executed");
        Files.createDirectories(executed);
        Files.writeString(executed.resolve("TEST-example.CoveredTest.xml"), "<testsuite><testcase name=\"covers\"/></testsuite>");
        Path skipped = tempDir.resolve("skipped");
        Files.createDirectories(skipped);

        CoverageMapStore store = new CoverageMapStore(tempDir.resolve("state"));
        store.write(List.of(
            new CoverageMapStore.ProfileReport("example.CoveredTest", "test", xml, executed),
            new CoverageMapStore.ProfileReport("example.TestHelper", "test", xml, skipped)), "inventory");
        CoverageMapStore.CoverageMap map = store.read();
        assertTrue(map.testTasksByClass().containsKey("example.CoveredTest"));
        assertTrue(!map.testTasksByClass().containsKey("example.TestHelper"));

        Path source = tempDir.resolve("src/test/java/example/CoveredTest.java");
        Files.createDirectories(source.getParent());
        Files.writeString(source, "package example; class CoveredTest { void first() {} }");
        String before = CoverageMapStore.inventoryFingerprint(tempDir);
        Files.writeString(source, "package example; class CoveredTest { void second() {} }");
        assertTrue(before.equals(CoverageMapStore.inventoryFingerprint(tempDir)));
    }

    @Test
    void keepsEveryTaskThatProfilesTheSameTestClass() throws Exception {
        Path xml = tempDir.resolve("profile.xml");
        Files.writeString(xml, "<report><package name=\"example\"><class name=\"example/Covered\"><counter type=\"LINE\" missed=\"0\" covered=\"1\"/></class></package></report>");
        Path first = tempDir.resolve("first");
        Path second = tempDir.resolve("second");
        Files.createDirectories(first);
        Files.createDirectories(second);
        Files.writeString(first.resolve("TEST-example.CoveredTest.xml"), "<testsuite><testcase name=\"covers\"/></testsuite>");
        Files.writeString(second.resolve("TEST-example.CoveredTest.xml"), "<testsuite><testcase name=\"covers\"/></testsuite>");

        CoverageMapStore store = new CoverageMapStore(tempDir.resolve("state"));
        store.write(List.of(
            new CoverageMapStore.ProfileReport("example.CoveredTest", "test", xml, first),
            new CoverageMapStore.ProfileReport("example.CoveredTest", "integrationTest", xml, second)), "inventory");

        assertTrue(store.read().testTasksByClass().get("example.CoveredTest").containsAll(Set.of("test", "integrationTest")));
    }

    @Test
    void invalidatesMapsFromTheSingleTaskSchema() throws Exception {
        Path state = tempDir.resolve("legacy-state");
        Files.createDirectories(state);
        Files.writeString(state.resolve("coverage-map.json"), """
            {"schemaVersion":1,"inventoryFingerprint":"inventory","productionToTests":{},"testTaskByClass":{}}
            """);

        assertTrue(new CoverageMapStore(state).read() == null);
    }
}
