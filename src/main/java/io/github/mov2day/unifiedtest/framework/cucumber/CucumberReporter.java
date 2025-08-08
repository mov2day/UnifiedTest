package io.github.mov2day.unifiedtest.framework.cucumber;

import io.cucumber.plugin.ConcurrentEventListener;
import io.cucumber.plugin.event.*;
import io.github.mov2day.unifiedtest.collector.UnifiedTestResult;
import io.github.mov2day.unifiedtest.collector.UnifiedTestResultCollector;

import java.io.PrintWriter;
import java.io.StringWriter;

public class CucumberReporter implements ConcurrentEventListener {

    private static UnifiedTestResultCollector collector;

    public static void setCollector(UnifiedTestResultCollector collector) {
        CucumberReporter.collector = collector;
    }

    @Override
    public void setEventPublisher(EventPublisher publisher) {
        publisher.registerHandlerFor(TestCaseFinished.class, this::handleTestCaseFinished);
    }

    private void handleTestCaseFinished(TestCaseFinished event) {
        TestCase testCase = event.getTestCase();
        Result result = event.getResult();
        String status = result.getStatus().toString();
        long duration = result.getDuration().toMillis();
        Throwable error = result.getError();

        String failureMessage = null;
        String stackTrace = null;
        if (error != null) {
            failureMessage = error.getMessage();
            StringWriter sw = new StringWriter();
            error.printStackTrace(new PrintWriter(sw));
            stackTrace = sw.toString();
        }

        UnifiedTestResult testResult = new UnifiedTestResult(
                testCase.getUri().toString(),
                testCase.getName(),
                status,
                failureMessage,
                stackTrace,
                duration
        );

        if (collector != null) {
            collector.addResult(testResult);
        }
    }
}
