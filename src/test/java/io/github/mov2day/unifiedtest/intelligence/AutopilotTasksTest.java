package io.github.mov2day.unifiedtest.intelligence;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AutopilotTasksTest {
    @Test
    void recognizesSimpleAndQualifiedClassSelectors() {
        assertTrue(AutopilotTasks.matchesFilter("CalculatorTest", "example.CalculatorTest"));
        assertTrue(AutopilotTasks.matchesFilter("example.CalculatorTest", "example.CalculatorTest"));
        assertTrue(AutopilotTasks.matchesFilter("*CalculatorTest", "example.CalculatorTest"));
        assertTrue(AutopilotTasks.matchesFilter("example.*", "example.CalculatorTest"));
    }

    @Test
    void conservativelyTreatsDottedSelectorsAsIncompleteCoverage() {
        assertTrue(AutopilotTasks.isMethodSelection("example.CalculatorTest.adds"));
        assertTrue(AutopilotTasks.isMethodSelection("*.adds"));
        assertTrue(AutopilotTasks.isMethodSelection("*.Adds"));
        assertTrue(AutopilotTasks.isMethodSelection("*._adds"));
        assertTrue(AutopilotTasks.isMethodSelection("example.CalculatorTest"));
        assertFalse(AutopilotTasks.isMethodSelection("example.*"));
        assertFalse(AutopilotTasks.isMethodSelection("CalculatorTest"));
    }
}
