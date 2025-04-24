package io.github.mov2day.unifiedtest.collector;

import org.gradle.api.tasks.testing.TestDescriptor;
import org.gradle.api.tasks.testing.TestResult;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for UnifiedTestResultCollector.
 * Verifies test result collection and storage functionality.
 */
public class UnifiedTestResultCollectorTest {
    @Test
    void collectsTestResults() {
        UnifiedTestResultCollector collector = new UnifiedTestResultCollector();
        TestDescriptor descriptor = mock(TestDescriptor.class);
        when(descriptor.getName()).thenReturn("testName");
        when(descriptor.getClassName()).thenReturn("TestClass");
        TestResult result = mock(TestResult.class);
        when(result.getResultType()).thenReturn(TestResult.ResultType.SUCCESS);
        collector.afterTest(descriptor, result);
        assertEquals(1, collector.getResults().size());
        UnifiedTestResult r = collector.getResults().get(0);
        assertEquals("TestClass", r.className);
        assertEquals("testName", r.testName);
        assertEquals("SUCCESS", r.status);
    }
}
