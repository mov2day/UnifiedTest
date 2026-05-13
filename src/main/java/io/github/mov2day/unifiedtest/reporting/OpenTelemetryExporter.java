package io.github.mov2day.unifiedtest.reporting;

import io.github.mov2day.unifiedtest.collector.UnifiedTestResult;
import io.github.mov2day.unifiedtest.collector.UnifiedTestResultCollector;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import org.gradle.api.Project;
import org.gradle.api.tasks.testing.Test;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Exports test results to OpenTelemetry for observability and monitoring.
 * Enables integration with OpenTelemetry-compatible monitoring systems.
 */
public class OpenTelemetryExporter {
    /**
     * Exports test results to an OpenTelemetry endpoint.
     * @param project the Gradle project
     * @param testTask the test task
     * @param endpoint the OpenTelemetry endpoint URL to export to
     */
    public static void export(Project project, Test testTask, String endpoint) {
        project.getLogger().warn("[UnifiedTest] Deprecated telemetry export API called without results. Skipping endpoint: {}", endpoint);
    }

    /**
     * Exports collected test results as OTLP spans. Export failures are logged and never fail the build.
     */
    public static void export(Project project, Test testTask, UnifiedTestResultCollector collector,
                              String endpoint, String serviceName, String runId, String traceBaseUrl) {
        if (endpoint == null || endpoint.isBlank()) {
            project.getLogger().warn("[UnifiedTest] OpenTelemetry enabled but no endpoint configured. Skipping export.");
            return;
        }

        List<UnifiedTestResult> results = collector.getResults();
        if (results.isEmpty()) {
            project.getLogger().lifecycle("[UnifiedTest] No test results to export to OpenTelemetry.");
            return;
        }

        OtlpGrpcSpanExporter exporter = null;
        SdkTracerProvider tracerProvider = null;
        try {
            exporter = OtlpGrpcSpanExporter.builder()
                .setEndpoint(endpoint)
                .build();
            BatchSpanProcessor spanProcessor = BatchSpanProcessor.builder(exporter).build();
            Resource resource = Resource.create(Attributes.of(
                AttributeKey.stringKey("service.name"), serviceName,
                AttributeKey.stringKey("unifiedtest.run_id"), runId
            ));
            tracerProvider = SdkTracerProvider.builder()
                .setResource(resource)
                .addSpanProcessor(spanProcessor)
                .build();
            OpenTelemetry openTelemetry = OpenTelemetrySdk.builder()
                .setTracerProvider(tracerProvider)
                .build();
            Tracer tracer = openTelemetry.getTracer("io.github.mov2day.unifiedtest");

            for (UnifiedTestResult result : results) {
                exportTestSpan(tracer, result, serviceName, runId, traceBaseUrl);
            }
            exportSummarySpan(tracer, testTask, results, serviceName, runId);

            tracerProvider.forceFlush().join(10, TimeUnit.SECONDS);
            project.getLogger().lifecycle("[UnifiedTest] Exported {} test spans to OpenTelemetry endpoint {}", results.size(), endpoint);
        } catch (Exception e) {
            project.getLogger().warn("[UnifiedTest] OpenTelemetry export failed: {}", e.getMessage());
        } finally {
            if (tracerProvider != null) {
                tracerProvider.shutdown().join(10, TimeUnit.SECONDS);
            } else if (exporter != null) {
                exporter.shutdown().join(10, TimeUnit.SECONDS);
            }
        }
    }

    private static void exportTestSpan(Tracer tracer, UnifiedTestResult result, String serviceName, String runId, String traceBaseUrl) {
        long endMillis = System.currentTimeMillis();
        long startMillis = Math.max(0, endMillis - Math.max(0, result.duration));
        Span span = tracer.spanBuilder("test " + result.className + "." + result.testName)
            .setStartTimestamp(startMillis, TimeUnit.MILLISECONDS)
            .startSpan();
        String traceId = span.getSpanContext().getTraceId();
        result.addMetadata("traceId", traceId);
        String traceUrl = toTraceUrl(traceBaseUrl, traceId);
        if (!traceUrl.isBlank()) {
            result.addMetadata("traceUrl", traceUrl);
        }

        span.setAttribute("service.name", serviceName);
        span.setAttribute("unifiedtest.run_id", runId);
        span.setAttribute("test.framework", result.framework);
        span.setAttribute("test.class", nullToUnknown(result.className));
        span.setAttribute("test.method", nullToUnknown(result.testName));
        span.setAttribute("test.status", nullToUnknown(result.status));
        span.setAttribute("test.duration_ms", result.duration);
        if (result.failureMessage != null && !result.failureMessage.isBlank()) {
            span.setAttribute("test.failure_message", result.failureMessage);
        }
        if ("FAIL".equalsIgnoreCase(result.status) || "FAILED".equalsIgnoreCase(result.status)) {
            span.setStatus(StatusCode.ERROR, result.failureMessage == null ? "Test failed" : result.failureMessage);
        }
        span.end(endMillis, TimeUnit.MILLISECONDS);
    }

    private static void exportSummarySpan(Tracer tracer, Test testTask, List<UnifiedTestResult> results, String serviceName, String runId) {
        long passed = results.stream().filter(r -> "PASS".equalsIgnoreCase(r.status) || "PASSED".equalsIgnoreCase(r.status)).count();
        long failed = results.stream().filter(r -> "FAIL".equalsIgnoreCase(r.status) || "FAILED".equalsIgnoreCase(r.status)).count();
        long skipped = results.stream().filter(r -> "SKIP".equalsIgnoreCase(r.status) || "SKIPPED".equalsIgnoreCase(r.status)).count();
        long totalDuration = results.stream().mapToLong(r -> r.duration).sum();

        Span summary = tracer.spanBuilder("unifiedtest run summary").startSpan();
        summary.setAttribute("service.name", serviceName);
        summary.setAttribute("unifiedtest.run_id", runId);
        summary.setAttribute("test.task", testTask.getName());
        summary.setAttribute("test.total", results.size());
        summary.setAttribute("test.passed", passed);
        summary.setAttribute("test.failed", failed);
        summary.setAttribute("test.skipped", skipped);
        summary.setAttribute("test.flaky", 0L);
        summary.setAttribute("test.duration_ms", totalDuration);
        if (failed > 0) {
            summary.setStatus(StatusCode.ERROR, "Test run had failures");
        }
        summary.end();
    }

    private static String toTraceUrl(String traceBaseUrl, String traceId) {
        if (traceBaseUrl == null || traceBaseUrl.isBlank()) {
            return "";
        }
        if (traceBaseUrl.contains("{traceId}")) {
            return traceBaseUrl.replace("{traceId}", traceId);
        }
        return traceBaseUrl.endsWith("/") ? traceBaseUrl + traceId : traceBaseUrl + "/" + traceId;
    }

    private static String nullToUnknown(String value) {
        return value == null || value.isBlank() ? "unknown" : value;
    }
}
