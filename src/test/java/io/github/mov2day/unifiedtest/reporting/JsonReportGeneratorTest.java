package io.github.mov2day.unifiedtest.reporting;

import org.gradle.api.Project;
import org.junit.jupiter.api.Test;

import io.github.mov2day.unifiedtest.collector.UnifiedTestResult;

import java.io.File;
import java.util.Collections;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class JsonReportGeneratorTest {
    @Test
    void generatesJsonReportFile() {
        Project project = mock(Project.class);
        File buildDir = new File("build/test-output");
        when(project.getLayout().getBuildDirectory().get().getAsFile()).thenReturn(buildDir);
        UnifiedTestResult result = new UnifiedTestResult("TestClass", "testName", "SUCCESS");
        JsonReportGenerator.generate(project, Collections.singletonList(result));
        File reportFile = new File(buildDir, "unifiedtest/report.json");
        assertTrue(reportFile.exists());
        reportFile.delete();
    }
}
