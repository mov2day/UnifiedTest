package io.github.mov2day.unifiedtest.reporting;

import io.github.mov2day.unifiedtest.collector.UnifiedTestResult;
import io.github.mov2day.unifiedtest.collector.UnifiedTestResultCollector;
import org.gradle.api.Project;
import org.gradle.api.tasks.testing.Test;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Generates the UnifiedTest HTML report.
 */
public class HtmlReportGenerator {
    public static void generate(Project project, Test testTask, UnifiedTestResultCollector collector) {
        generate(project, testTask, collector, project.getName(), "", false, "");
    }

    public static void generate(Project project, Test testTask, UnifiedTestResultCollector collector,
                                String serviceName, String runId, boolean telemetryEnabled, String traceBaseUrl) {
        File reportFile = project.getLayout()
            .getBuildDirectory()
            .file("unifiedtest/reports/index.html")
            .get()
            .getAsFile();
        reportFile.getParentFile().mkdirs();

        try (FileWriter writer = new FileWriter(reportFile)) {
            writer.write(render(project, testTask, collector.getResults(), serviceName, runId, telemetryEnabled, traceBaseUrl));
            project.getLogger().lifecycle("UnifiedTest HTML report generated at: {}", reportFile.getAbsolutePath());
        } catch (IOException e) {
            project.getLogger().error("Failed to write UnifiedTest HTML report", e);
        }
    }

    private static String render(Project project, Test testTask, List<UnifiedTestResult> results,
                                 String serviceName, String runId, boolean telemetryEnabled, String traceBaseUrl) {
        long passed = countStatus(results, "PASS");
        long failed = countStatus(results, "FAIL");
        long skipped = countStatus(results, "SKIP");
        long duration = results.stream().mapToLong(r -> r.duration).sum();
        double passRate = results.isEmpty() ? 0 : (passed * 100.0 / results.size());

        Set<String> frameworks = new TreeSet<>();
        for (UnifiedTestResult result : results) {
            frameworks.add(result.framework == null ? "unknown" : result.framework);
        }

        AllureReportReader allureReader = new AllureReportReader(project);
        Map<String, AllureReportReader.AllureTestResult> allureResults = allureReader.hasAllureReports()
            ? allureReader.readAllureResults()
            : Collections.emptyMap();
        EvidenceSummary evidence = summarizeEvidence(allureResults);
        long maxDuration = results.stream().mapToLong(r -> r.duration).max().orElse(0L);

        StringBuilder html = new StringBuilder(50000);
        html.append("<!DOCTYPE html><html lang=\"en\"><head><meta charset=\"UTF-8\">");
        html.append("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">");
        html.append("<title>UnifiedTest Report</title>");
        html.append("<style>").append(styles()).append("</style></head><body>");
        html.append("<main class=\"shell\">");

        html.append("<header class=\"hero\"><div class=\"hero-copy\">");
        html.append("<p class=\"eyebrow\">").append(esc(serviceName)).append("</p>");
        html.append("<h1>UnifiedTest Report</h1>");
        html.append("<p class=\"meta\">Task ").append(esc(testTask.getName()))
            .append(" | Run ").append(esc(runId.isBlank() ? "local" : runId))
            .append(" | Generated ")
            .append(esc(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))))
            .append("</p>");
        html.append("<div class=\"hero-badges\">");
        if (telemetryEnabled) {
            html.append("<span>OpenTelemetry enabled</span>");
        }
        html.append("<span>").append(evidence.allureTests).append(" Allure results</span>");
        html.append("<span>").append(evidence.images).append(" screenshots</span>");
        html.append("</div></div>");
        html.append("<div class=\"health\" style=\"--score:").append(String.format(Locale.US, "%.1f", passRate)).append("%\"><strong>")
            .append(String.format(Locale.US, "%.1f%%", passRate)).append("</strong><span>pass rate</span></div>");
        html.append("</header>");

        html.append("<section class=\"summary\">")
            .append(stat("Total", results.size(), formatDuration(duration), "total"))
            .append(stat("Passed", passed, pct(passed, results.size()), "pass"))
            .append(stat("Failed", failed, pct(failed, results.size()), "fail"))
            .append(stat("Skipped", skipped, pct(skipped, results.size()), "skip"))
            .append(stat("Steps", evidence.steps, "from Allure", "steps"))
            .append(stat("Evidence", evidence.attachments, evidence.images + " images", "evidence"))
            .append("</section>");

        renderRunMetadata(html, project, testTask, serviceName, runId, telemetryEnabled, traceBaseUrl, frameworks);
        renderInsights(html, results, allureResults, maxDuration);

        html.append("<section class=\"toolbar\" aria-label=\"Report filters\">")
            .append("<input id=\"search\" type=\"search\" aria-label=\"Search tests\" placeholder=\"Search class, test, owner, feature, evidence\">")
            .append("<select id=\"statusFilter\" aria-label=\"Filter by status\"><option value=\"all\">All statuses</option><option value=\"PASS\">Passed</option><option value=\"FAIL\">Failed</option><option value=\"SKIP\">Skipped</option></select>")
            .append("<select id=\"frameworkFilter\" aria-label=\"Filter by framework\"><option value=\"all\">All frameworks</option>");
        for (String framework : frameworks) {
            html.append("<option value=\"").append(attr(framework)).append("\">").append(esc(framework)).append("</option>");
        }
        html.append("</select><select id=\"sortBy\" aria-label=\"Sort tests\"><option value=\"name\">Name</option><option value=\"duration\">Duration</option><option value=\"status\">Status</option><option value=\"evidence\">Evidence</option></select>")
            .append("<label class=\"toggle\"><input id=\"evidenceOnly\" type=\"checkbox\">Evidence only</label>")
            .append("</section>");

        html.append("<section class=\"test-list\" id=\"results\">");
        for (UnifiedTestResult result : results) {
            renderCard(html, result, findAllureResult(result, allureResults), maxDuration);
        }
        html.append("<p id=\"empty\" class=\"empty\" hidden>No matching tests.</p></section>");
        html.append("<script>").append(script()).append("</script>");
        html.append("</main></body></html>");
        return html.toString();
    }

    private static void renderRunMetadata(StringBuilder html, Project project, Test testTask, String serviceName, String runId,
                                          boolean telemetryEnabled, String traceBaseUrl, Set<String> frameworks) {
        html.append("<section class=\"run-meta\">");
        metaPill(html, "Service", serviceName);
        metaPill(html, "Project", project.getName());
        metaPill(html, "Task", testTask.getName());
        metaPill(html, "Run ID", runId.isBlank() ? "local" : runId);
        metaPill(html, "Frameworks", frameworks.isEmpty() ? "unknown" : String.join(", ", frameworks));
        metaPill(html, "Telemetry", telemetryEnabled ? "enabled" : "disabled");
        if (traceBaseUrl != null && !traceBaseUrl.isBlank()) {
            metaPill(html, "Trace base", traceBaseUrl);
        }
        html.append("</section>");
    }

    private static void renderInsights(StringBuilder html, List<UnifiedTestResult> results,
                                       Map<String, AllureReportReader.AllureTestResult> allureResults,
                                       long maxDuration) {
        html.append("<section class=\"insights\">");
        renderFailureSpotlight(html, results);
        renderSlowTests(html, results, maxDuration);
        renderSuiteBreakdown(html, results);
        renderFrameworkBreakdown(html, results);
        renderEvidenceBreakdown(html, results, allureResults);
        html.append("</section>");
    }

    private static void renderFailureSpotlight(StringBuilder html, List<UnifiedTestResult> results) {
        html.append("<article class=\"panel\"><h2>Failure spotlight</h2>");
        List<UnifiedTestResult> failed = filterByStatus(results, "FAIL");
        if (failed.isEmpty()) {
            html.append("<p class=\"muted\">No failing tests in this run.</p>");
        } else {
            html.append("<ol class=\"ranked\">");
            for (UnifiedTestResult result : failed.subList(0, Math.min(5, failed.size()))) {
                html.append("<li><b>").append(esc(result.testName)).append("</b><span>")
                    .append(esc(simpleClass(result.className))).append("</span>");
                if (result.failureMessage != null && !result.failureMessage.isBlank()) {
                    html.append("<em>").append(esc(result.failureMessage)).append("</em>");
                }
                html.append("</li>");
            }
            html.append("</ol>");
        }
        html.append("</article>");
    }

    private static void renderSlowTests(StringBuilder html, List<UnifiedTestResult> results, long maxDuration) {
        html.append("<article class=\"panel\"><h2>Slowest tests</h2>");
        if (results.isEmpty()) {
            html.append("<p class=\"muted\">No duration data captured.</p>");
        } else {
            List<UnifiedTestResult> sorted = new ArrayList<>(results);
            sorted.sort(Comparator.comparingLong((UnifiedTestResult r) -> r.duration).reversed());
            html.append("<ol class=\"ranked duration-list\">");
            for (UnifiedTestResult result : sorted.subList(0, Math.min(5, sorted.size()))) {
                html.append("<li><b>").append(esc(result.testName)).append("</b><span>")
                    .append(formatDuration(result.duration)).append("</span>")
                    .append(durationBar(result.duration, maxDuration))
                    .append("</li>");
            }
            html.append("</ol>");
        }
        html.append("</article>");
    }

    private static void renderSuiteBreakdown(StringBuilder html, List<UnifiedTestResult> results) {
        html.append("<article class=\"panel\"><h2>Suites</h2>");
        Map<String, StatusCounts> counts = new LinkedHashMap<>();
        for (UnifiedTestResult result : results) {
            counts.computeIfAbsent(simpleClass(result.className), ignored -> new StatusCounts()).add(result.status);
        }
        renderBreakdownRows(html, counts);
        html.append("</article>");
    }

    private static void renderFrameworkBreakdown(StringBuilder html, List<UnifiedTestResult> results) {
        html.append("<article class=\"panel\"><h2>Frameworks</h2>");
        Map<String, StatusCounts> counts = new LinkedHashMap<>();
        for (UnifiedTestResult result : results) {
            counts.computeIfAbsent(result.framework, ignored -> new StatusCounts()).add(result.status);
        }
        renderBreakdownRows(html, counts);
        html.append("</article>");
    }

    private static void renderEvidenceBreakdown(StringBuilder html, List<UnifiedTestResult> results,
                                                Map<String, AllureReportReader.AllureTestResult> allureResults) {
        html.append("<article class=\"panel\"><h2>Evidence</h2>");
        int testsWithEvidence = 0;
        int screenshots = 0;
        int attachments = 0;
        int steps = 0;
        for (UnifiedTestResult result : results) {
            AllureReportReader.AllureTestResult allure = findAllureResult(result, allureResults);
            int metadataImages = metadataImageEntries(result.getMetadata()).size();
            if (allure != null) {
                screenshots += allure.getImageAttachments().size();
                attachments += allure.getAllAttachments().size();
                steps += allure.getTotalStepCount();
            }
            if ((allure != null && !allure.getAllAttachments().isEmpty()) || metadataImages > 0) {
                testsWithEvidence++;
            }
            screenshots += metadataImages;
        }
        html.append("<div class=\"evidence-stats\">")
            .append("<span><b>").append(testsWithEvidence).append("</b> tests with evidence</span>")
            .append("<span><b>").append(screenshots).append("</b> screenshots</span>")
            .append("<span><b>").append(attachments).append("</b> attachments</span>")
            .append("<span><b>").append(steps).append("</b> steps</span>")
            .append("</div></article>");
    }

    private static void renderBreakdownRows(StringBuilder html, Map<String, StatusCounts> counts) {
        if (counts.isEmpty()) {
            html.append("<p class=\"muted\">No data captured.</p>");
            return;
        }
        html.append("<div class=\"breakdown\">");
        counts.entrySet().stream()
            .sorted((left, right) -> Integer.compare(right.getValue().total(), left.getValue().total()))
            .limit(6)
            .forEach(entry -> {
                StatusCounts c = entry.getValue();
                html.append("<div><span><b>").append(esc(entry.getKey())).append("</b><em>")
                    .append(c.total()).append(" tests</em></span><div class=\"stack\">");
                segment(html, "pass", c.passed, c.total());
                segment(html, "fail", c.failed, c.total());
                segment(html, "skip", c.skipped, c.total());
                html.append("</div></div>");
            });
        html.append("</div>");
    }

    private static void segment(StringBuilder html, String type, int value, int total) {
        if (value <= 0 || total <= 0) {
            return;
        }
        html.append("<i class=\"").append(type).append("\" style=\"width:")
            .append(String.format(Locale.US, "%.1f", value * 100.0 / total)).append("%\"></i>");
    }

    private static void metaPill(StringBuilder html, String label, String value) {
        if (value != null && !value.isBlank()) {
            String widthClass = value.length() > 34 || label.length() > 9 ? " wide" : "";
            html.append("<span class=\"meta-pill").append(widthClass).append("\"><b>").append(esc(label)).append("</b>").append(esc(value)).append("</span>");
        }
    }

    private static List<UnifiedTestResult> filterByStatus(List<UnifiedTestResult> results, String status) {
        List<UnifiedTestResult> filtered = new ArrayList<>();
        for (UnifiedTestResult result : results) {
            if (status.equalsIgnoreCase(result.status)) {
                filtered.add(result);
            }
        }
        return filtered;
    }

    private static void renderCard(StringBuilder html, UnifiedTestResult result, AllureReportReader.AllureTestResult allure,
                                   long maxDuration) {
        String key = result.className + "." + result.testName;
        int evidenceCount = (allure == null ? 0 : allure.getAllAttachments().size()) + metadataImageEntries(result.getMetadata()).size();
        String searchText = key + " " + result.framework + " " + labelSearch(allure);

        html.append("<article class=\"test-card row\" data-status=\"").append(attr(result.status))
            .append("\" data-framework=\"").append(attr(result.framework))
            .append("\" data-duration=\"").append(result.duration)
            .append("\" data-evidence=\"").append(evidenceCount)
            .append("\" data-name=\"").append(attr(key))
            .append("\" data-search=\"").append(attr(searchText)).append("\">");
        html.append("<div class=\"test-main\">");
        html.append("<div class=\"test-title\"><span class=\"badge ").append(statusClass(result.status)).append("\">")
            .append(esc(result.status)).append("</span><div><h2>").append(esc(result.testName)).append("</h2><p>")
            .append(esc(result.className)).append("</p></div></div>");
        html.append("<div class=\"test-metrics\"><span>").append(esc(result.framework)).append("</span><span>")
            .append(formatDuration(result.duration)).append("</span><span>").append(evidenceCount).append(" files</span>")
            .append(durationBar(result.duration, maxDuration)).append("</div>");
        html.append("</div>");

        renderQuickMeta(html, result, allure);
        renderDetails(html, result, allure);
        html.append("</article>");
    }

    private static void renderQuickMeta(StringBuilder html, UnifiedTestResult result, AllureReportReader.AllureTestResult allure) {
        html.append("<div class=\"quick-meta\">");
        if (allure != null) {
            addChip(html, "Allure", allure.getStatus());
            addChip(html, "Stage", allure.getStage());
            addChip(html, "Owner", allure.getFirstLabel("owner"));
            addChip(html, "Severity", allure.getFirstLabel("severity"));
            addChip(html, "Feature", allure.getFirstLabel("feature"));
            addChip(html, "Story", allure.getFirstLabel("story"));
            addChip(html, "Steps", String.valueOf(allure.getTotalStepCount()));
            addChip(html, "Screenshots", String.valueOf(allure.getImageAttachments().size()));
            if (allure.isFlaky()) addChip(html, "Flag", "flaky");
            if (allure.isKnown()) addChip(html, "Flag", "known");
            if (allure.isMuted()) addChip(html, "Flag", "muted");
        } else {
            addChip(html, "Source", "UnifiedTest");
        }
        html.append("</div>");
    }

    private static void renderDetails(StringBuilder html, UnifiedTestResult result, AllureReportReader.AllureTestResult allure) {
        boolean hasDetails = result.failureMessage != null || result.stackTrace != null || allure != null || result.getMetadata().containsKey("traceId");
        if (!hasDetails) {
            html.append("<p class=\"muted compact\">No extra details captured.</p>");
            return;
        }

        html.append("<details class=\"detail\"><summary>Details and evidence</summary>");
        if (result.failureMessage != null) {
            html.append("<p class=\"failure\">").append(esc(result.failureMessage)).append("</p>");
        }
        if (result.stackTrace != null) {
            html.append("<pre>").append(esc(result.stackTrace)).append("</pre>");
        }
        renderUnifiedOverview(html, result);
        renderMetadataScreenshots(html, result.getMetadata());

        if (allure != null) {
            renderAllureOverview(html, allure);
            renderImages(html, allure.getImageAttachments());
            renderAttachments(html, allure.getAllAttachments());
            renderSteps(html, allure.getSteps());
        }

        String traceUrl = result.getMetadata().get("traceUrl");
        String traceId = result.getMetadata().get("traceId");
        if (traceUrl != null) {
            html.append("<a class=\"trace\" href=\"").append(attr(traceUrl)).append("\" target=\"_blank\" rel=\"noreferrer\">Open trace</a>");
        } else if (traceId != null) {
            html.append("<code>").append(esc(traceId)).append("</code>");
        }
        html.append("</details>");
    }

    private static void renderUnifiedOverview(StringBuilder html, UnifiedTestResult result) {
        html.append("<section class=\"unified-panel\"><h3>UnifiedTest metadata</h3><div class=\"meta-grid\">");
        metaItem(html, "Class", result.className);
        metaItem(html, "Method", result.testName);
        metaItem(html, "Framework", result.framework);
        metaItem(html, "Status", result.status);
        metaItem(html, "Duration", formatDuration(result.duration));
        html.append("</div>");
        if (!result.getMetadata().isEmpty()) {
            html.append("<h4>Captured metadata</h4><div class=\"label-grid\">");
            for (Map.Entry<String, String> entry : result.getMetadata().entrySet()) {
                html.append("<span><b>").append(esc(entry.getKey())).append("</b>")
                    .append(esc(entry.getValue())).append("</span>");
            }
            html.append("</div>");
        }
        html.append("</section>");
    }

    private static void renderAllureOverview(StringBuilder html, AllureReportReader.AllureTestResult allure) {
        html.append("<section class=\"allure-panel\"><h3>Allure 3 metadata</h3>");
        html.append("<div class=\"meta-grid\">");
        metaItem(html, "Name", allure.getName());
        metaItem(html, "Full name", allure.getFullName());
        metaItem(html, "UUID", allure.getUuid());
        metaItem(html, "History ID", allure.getHistoryId());
        metaItem(html, "Test case ID", allure.getTestCaseId());
        metaItem(html, "Status", allure.getStatus());
        metaItem(html, "Stage", allure.getStage());
        metaItem(html, "Duration", formatDuration(allure.getDuration()));
        html.append("</div>");

        if (!allure.getDescription().isBlank() || !allure.getDescriptionHtml().isBlank()) {
            html.append("<div class=\"description\">").append(esc(!allure.getDescription().isBlank()
                ? allure.getDescription()
                : allure.getDescriptionHtml())).append("</div>");
        }
        if (!allure.getStatusMessage().isBlank()) {
            html.append("<p class=\"failure\">").append(esc(allure.getStatusMessage())).append("</p>");
        }
        if (!allure.getStatusTrace().isBlank()) {
            html.append("<pre>").append(esc(allure.getStatusTrace())).append("</pre>");
        }
        renderLabels(html, allure);
        renderParameters(html, allure.getParameters());
        renderLinks(html, allure.getLinks());
        html.append("</section>");
    }

    private static void renderLabels(StringBuilder html, AllureReportReader.AllureTestResult allure) {
        if (allure.getLabels().isEmpty()) {
            return;
        }
        html.append("<h4>Labels</h4><div class=\"label-grid\">");
        for (Map.Entry<String, List<String>> entry : allure.getLabels().entrySet()) {
            html.append("<span><b>").append(esc(entry.getKey())).append("</b> ")
                .append(esc(String.join(", ", entry.getValue()))).append("</span>");
        }
        html.append("</div>");
    }

    private static void renderParameters(StringBuilder html, List<AllureReportReader.Parameter> parameters) {
        if (parameters.isEmpty()) {
            return;
        }
        html.append("<h4>Parameters</h4><div class=\"param-grid\">");
        for (AllureReportReader.Parameter parameter : parameters) {
            String value = "masked".equalsIgnoreCase(parameter.getMode()) ? "******" : parameter.getValue();
            html.append("<span><b>").append(esc(parameter.getName())).append("</b> ").append(esc(value));
            if (parameter.isExcluded()) {
                html.append(" <em>excluded</em>");
            }
            html.append("</span>");
        }
        html.append("</div>");
    }

    private static void renderLinks(StringBuilder html, List<AllureReportReader.Link> links) {
        if (links.isEmpty()) {
            return;
        }
        html.append("<h4>Links</h4><div class=\"links\">");
        for (AllureReportReader.Link link : links) {
            html.append("<a href=\"").append(attr(link.getUrl())).append("\" target=\"_blank\" rel=\"noreferrer\">")
                .append(esc(link.getType())).append(": ").append(esc(link.getName())).append("</a>");
        }
        html.append("</div>");
    }

    private static void renderImages(StringBuilder html, List<AllureReportReader.Attachment> images) {
        if (images.isEmpty()) {
            return;
        }
        html.append("<h4>Screenshot evidence</h4><div class=\"evidence-gallery\">");
        for (AllureReportReader.Attachment attachment : images) {
            html.append("<figure><a href=\"").append(attr(attachment.getFileUri())).append("\" target=\"_blank\" rel=\"noreferrer\">")
                .append("<img loading=\"lazy\" src=\"").append(attr(attachment.getFileUri())).append("\" alt=\"")
                .append(attr(attachment.getName())).append("\"></a><figcaption>")
                .append(esc(attachment.getName())).append("<small>").append(esc(attachment.getType())).append("</small></figcaption></figure>");
        }
        html.append("</div>");
    }

    private static void renderMetadataScreenshots(StringBuilder html, Map<String, String> metadata) {
        List<Map.Entry<String, String>> images = metadataImageEntries(metadata);
        if (images.isEmpty()) {
            return;
        }
        html.append("<h4>Screenshot evidence</h4><div class=\"evidence-gallery\">");
        for (Map.Entry<String, String> image : images) {
            String url = evidenceUrl(image.getValue());
            html.append("<figure><a href=\"").append(attr(url)).append("\" target=\"_blank\" rel=\"noreferrer\">")
                .append("<img loading=\"lazy\" src=\"").append(attr(url)).append("\" alt=\"")
                .append(attr(image.getKey())).append("\"></a><figcaption>")
                .append(esc(image.getKey())).append("<small>").append(esc(image.getValue())).append("</small></figcaption></figure>");
        }
        html.append("</div>");
    }

    private static void renderAttachments(StringBuilder html, List<AllureReportReader.Attachment> attachments) {
        List<AllureReportReader.Attachment> nonImages = new ArrayList<>();
        for (AllureReportReader.Attachment attachment : attachments) {
            if (!attachment.isImage()) {
                nonImages.add(attachment);
            }
        }
        if (nonImages.isEmpty()) {
            return;
        }
        html.append("<h4>Attachments</h4><div class=\"attachments\">");
        for (AllureReportReader.Attachment attachment : nonImages) {
            html.append("<a href=\"").append(attr(attachment.getFileUri())).append("\" target=\"_blank\" rel=\"noreferrer\">")
                .append("<b>").append(esc(attachment.getName())).append("</b><span>")
                .append(esc(attachment.getType())).append("</span></a>");
        }
        html.append("</div>");
    }

    private static void renderSteps(StringBuilder html, List<AllureReportReader.Step> steps) {
        if (steps.isEmpty()) {
            return;
        }
        html.append("<h4>Steps</h4><ol class=\"steps\">");
        for (AllureReportReader.Step step : steps) {
            renderStep(html, step);
        }
        html.append("</ol>");
    }

    private static void renderStep(StringBuilder html, AllureReportReader.Step step) {
        html.append("<li><span class=\"step-head\"><b>").append(esc(step.getName())).append("</b><em>")
            .append(esc(step.getStatus())).append(" | ").append(formatDuration(step.getDuration())).append("</em></span>");
        renderParameters(html, step.getParameters());
        renderImages(html, imageAttachments(step.getAttachments()));
        renderAttachments(html, step.getAttachments());
        if (!step.getSteps().isEmpty()) {
            html.append("<ol>");
            for (AllureReportReader.Step nested : step.getSteps()) {
                renderStep(html, nested);
            }
            html.append("</ol>");
        }
        html.append("</li>");
    }

    private static List<AllureReportReader.Attachment> imageAttachments(List<AllureReportReader.Attachment> attachments) {
        List<AllureReportReader.Attachment> images = new ArrayList<>();
        for (AllureReportReader.Attachment attachment : attachments) {
            if (attachment.isImage()) {
                images.add(attachment);
            }
        }
        return images;
    }

    private static List<Map.Entry<String, String>> metadataImageEntries(Map<String, String> metadata) {
        List<Map.Entry<String, String>> images = new ArrayList<>();
        for (Map.Entry<String, String> entry : metadata.entrySet()) {
            String key = entry.getKey() == null ? "" : entry.getKey().toLowerCase(Locale.ROOT);
            String value = entry.getValue();
            if ((key.contains("screenshot") || key.contains("image") || key.contains("evidence")) && isImageReference(value)) {
                images.add(entry);
            }
        }
        return images;
    }

    private static boolean isImageReference(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }
        String normalized = value.toLowerCase(Locale.ROOT);
        return normalized.startsWith("data:image/")
            || normalized.endsWith(".png")
            || normalized.endsWith(".jpg")
            || normalized.endsWith(".jpeg")
            || normalized.endsWith(".gif")
            || normalized.endsWith(".webp")
            || normalized.endsWith(".svg");
    }

    private static String evidenceUrl(String value) {
        if (value == null) {
            return "";
        }
        if (value.startsWith("http://") || value.startsWith("https://") || value.startsWith("file:") || value.startsWith("data:")) {
            return value;
        }
        File file = new File(value);
        return file.isAbsolute() ? file.toURI().toString() : value;
    }

    private static AllureReportReader.AllureTestResult findAllureResult(UnifiedTestResult result,
                                                                         Map<String, AllureReportReader.AllureTestResult> allureResults) {
        String key = result.className + "." + result.testName;
        AllureReportReader.AllureTestResult match = allureResults.get(key);
        if (match == null) match = allureResults.get(result.testName);
        if (match == null) match = allureResults.get(simpleName(result.testName));
        return match;
    }

    private static String labelSearch(AllureReportReader.AllureTestResult allure) {
        if (allure == null) {
            return "";
        }
        StringBuilder search = new StringBuilder();
        for (Map.Entry<String, List<String>> entry : allure.getLabels().entrySet()) {
            search.append(' ').append(entry.getKey()).append(' ').append(String.join(" ", entry.getValue()));
        }
        for (AllureReportReader.Attachment attachment : allure.getAllAttachments()) {
            search.append(' ').append(attachment.getName()).append(' ').append(attachment.getType());
        }
        return search.toString();
    }

    private static EvidenceSummary summarizeEvidence(Map<String, AllureReportReader.AllureTestResult> allureResults) {
        EvidenceSummary summary = new EvidenceSummary();
        for (AllureReportReader.AllureTestResult result : uniqueResults(allureResults)) {
            summary.allureTests++;
            summary.steps += result.getTotalStepCount();
            summary.attachments += result.getAllAttachments().size();
            summary.images += result.getImageAttachments().size();
        }
        return summary;
    }

    private static List<AllureReportReader.AllureTestResult> uniqueResults(Map<String, AllureReportReader.AllureTestResult> allureResults) {
        List<AllureReportReader.AllureTestResult> unique = new ArrayList<>();
        for (AllureReportReader.AllureTestResult result : allureResults.values()) {
            if (!unique.contains(result)) {
                unique.add(result);
            }
        }
        return unique;
    }

    private static long countStatus(List<UnifiedTestResult> results, String status) {
        return results.stream().filter(r -> status.equals(r.status)).count();
    }

    private static void addChip(StringBuilder html, String label, String value) {
        if (value != null && !value.isBlank()) {
            html.append("<span><b>").append(esc(label)).append("</b>").append(esc(value)).append("</span>");
        }
    }

    private static void metaItem(StringBuilder html, String label, String value) {
        if (value != null && !value.isBlank()) {
            html.append("<span><b>").append(esc(label)).append("</b>").append(esc(value)).append("</span>");
        }
    }

    private static String stat(String label, long value, String note, String kind) {
        return "<article class=\"stat " + kind + "\"><span>" + esc(label) + "</span><strong>" + value + "</strong><small>" + esc(note) + "</small></article>";
    }

    private static String statusClass(String status) {
        if ("PASS".equalsIgnoreCase(status) || "passed".equalsIgnoreCase(status)) return "pass";
        if ("FAIL".equalsIgnoreCase(status) || "failed".equalsIgnoreCase(status) || "broken".equalsIgnoreCase(status)) return "fail";
        if ("SKIP".equalsIgnoreCase(status) || "skipped".equalsIgnoreCase(status)) return "skip";
        return "unknown";
    }

    private static String pct(long value, long total) {
        return total == 0 ? "0.0%" : String.format(Locale.US, "%.1f%%", value * 100.0 / total);
    }

    private static String durationBar(long duration, long maxDuration) {
        double width = maxDuration <= 0 ? 0 : Math.max(4, duration * 100.0 / maxDuration);
        return "<span class=\"duration-bar\"><i style=\"width:" + String.format(Locale.US, "%.1f", width) + "%\"></i></span>";
    }

    private static String formatDuration(long millis) {
        if (millis < 1000) return millis + "ms";
        if (millis < 60000) return String.format(Locale.US, "%.2fs", millis / 1000.0);
        return String.format(Locale.US, "%dm %02ds", millis / 60000, (millis / 1000) % 60);
    }

    private static String simpleName(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        int index = value.lastIndexOf('.');
        return index >= 0 ? value.substring(index + 1) : value;
    }

    private static String simpleClass(String value) {
        return simpleName(value);
    }

    private static String esc(String value) {
        if (value == null) return "";
        return value.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#39;");
    }

    private static String attr(String value) {
        return esc(value == null ? "" : value);
    }

    private static String styles() {
        return """
            :root{--bg:#f3f7f5;--ink:#101828;--muted:#667085;--line:#d7dfdc;--soft:#eef4f1;--card:#fff;--brand:#1f6f5b;--brand-2:#2b7db8;--pass:#237a3b;--fail:#b42318;--skip:#9a6700;--shadow:0 14px 36px rgba(16,40,34,.08)}
            *{box-sizing:border-box}body{margin:0;background:linear-gradient(135deg,#f7fbf8 0%,#eef5f2 46%,#f7f4ec 100%);background-attachment:fixed;color:var(--ink);font:14px/1.5 "Aptos","Avenir Next","Trebuchet MS",sans-serif}body:before{content:"";position:fixed;inset:0;pointer-events:none;background-image:linear-gradient(rgba(31,111,91,.045) 1px,transparent 1px),linear-gradient(90deg,rgba(31,111,91,.04) 1px,transparent 1px);background-size:42px 42px;mask-image:linear-gradient(#000,transparent 82%)}
            .shell{max-width:1360px;margin:0 auto;padding:28px;position:relative}.hero{display:grid;grid-template-columns:minmax(0,1fr) 156px;gap:28px;align-items:center;margin-bottom:0;padding:28px 30px;border:1px solid rgba(31,111,91,.18);border-radius:14px 14px 0 0;background:linear-gradient(135deg,rgba(255,255,255,.96) 0%,rgba(241,249,245,.94) 58%,rgba(247,244,236,.92) 100%);box-shadow:var(--shadow)}
            .eyebrow{margin:0 0 5px;color:var(--brand);font-weight:800;text-transform:uppercase;font-size:12px}h1{margin:0;font-size:38px;font-weight:800;letter-spacing:0}.meta,.muted{color:var(--muted)}.compact{margin:8px 0 0}.test-card>.compact{padding:0 18px 18px}.hero-badges,.quick-meta,.links,.attachments{display:flex;flex-wrap:wrap;gap:8px}.hero-badges{margin-top:13px;gap:14px;color:#344054}.hero-badges span{padding:0;background:transparent;border:0}.hero-badges span+span:before{content:"";display:inline-block;width:5px;height:5px;margin:0 14px 2px 0;border-radius:999px;background:#9aa8a2}.quick-meta span{border:1px solid var(--line);border-radius:999px;padding:5px 9px;background:#fff;color:#344054;font-size:12px}.quick-meta b{margin-right:5px;color:#667085}.health{min-width:148px;aspect-ratio:1;border-radius:999px;display:flex;flex-direction:column;align-items:center;justify-content:center;gap:4px;background:conic-gradient(var(--pass) var(--score),#dfe7e3 0);position:relative;text-align:center}.health:after{content:"";position:absolute;inset:13px;border-radius:999px;background:#fff}.health strong,.health span{position:relative;z-index:1;line-height:1.1}.health strong{font-size:27px}.health span{font-size:12px;color:var(--muted);margin:0}
            .summary{display:grid;grid-template-columns:repeat(6,minmax(0,1fr));gap:0;margin:0 0 14px;background:rgba(255,255,255,.92);border:1px solid rgba(31,111,91,.18);border-top:0;border-radius:0 0 14px 14px;box-shadow:var(--shadow);overflow:hidden}.stat{background:transparent;border:0;border-left:1px solid var(--line);border-radius:0;padding:14px 16px}.stat:first-child{border-left:0}.stat span{display:block;color:var(--muted);font-weight:800;font-size:12px;text-transform:uppercase}.stat strong{display:block;font-size:27px;margin:1px 0}.stat.pass strong{color:var(--pass)}.stat.fail strong{color:var(--fail)}.stat.skip strong{color:var(--skip)}.stat.evidence strong{color:var(--brand-2)}
            .run-meta{display:grid;grid-template-columns:repeat(6,minmax(118px,1fr));gap:8px;margin:0 0 14px}.meta-pill{min-width:0;background:rgba(255,255,255,.82);border:1px solid var(--line);border-radius:9px;padding:8px 10px;color:#344054;overflow-wrap:anywhere}.meta-pill.wide{grid-column:span 2}.meta-pill b{display:block;color:var(--muted);font-size:11px;text-transform:uppercase}
            .insights{display:grid;grid-template-columns:repeat(12,minmax(0,1fr));gap:12px;margin:14px 0}.panel{grid-column:span 4;background:rgba(255,255,255,.92);border:1px solid var(--line);border-radius:12px;padding:15px;min-width:0}.panel:nth-child(-n+2){grid-column:span 6}.panel h2{margin:0 0 10px;font-size:15px}.ranked{list-style:none;margin:0;padding:0;display:grid;gap:8px}.ranked li{display:grid;gap:3px;border-top:1px solid var(--soft);padding-top:8px}.ranked b,.breakdown b{overflow:hidden;text-overflow:ellipsis;white-space:nowrap}.ranked span,.ranked em,.breakdown em{color:var(--muted);font-style:normal;font-size:12px}.breakdown{display:grid;gap:10px}.breakdown span{display:grid;grid-template-columns:minmax(0,1fr) auto;gap:10px}.stack{display:flex;height:8px;overflow:hidden;border-radius:999px;background:#dfe7e3;margin-top:4px}.stack i.pass{background:var(--pass)}.stack i.fail{background:var(--fail)}.stack i.skip{background:var(--skip)}.evidence-stats{display:grid;grid-template-columns:1fr 1fr;gap:8px}.evidence-stats span{background:var(--soft);border-radius:8px;padding:9px;color:#344054}.evidence-stats b{display:block;font-size:20px;color:var(--brand)}
            .toolbar{position:sticky;top:0;z-index:5;display:grid;grid-template-columns:minmax(260px,1fr) minmax(140px,160px) minmax(160px,190px) minmax(130px,150px) auto;gap:10px;align-items:center;margin:16px 0;padding:10px;background:rgba(243,247,245,.88);border:1px solid rgba(215,223,220,.9);border-radius:12px;backdrop-filter:blur(12px)}input,select{height:42px;border:1px solid var(--line);border-radius:8px;background:#fff;color:var(--ink);padding:0 10px;font:inherit}input:focus,select:focus,.toggle:focus-within,summary:focus-visible{outline:2px solid rgba(31,111,91,.32);outline-offset:2px}.toggle{height:42px;border:1px solid var(--line);border-radius:8px;background:#fff;display:flex;align-items:center;gap:8px;padding:0 10px;color:#344054;white-space:nowrap}
            .test-list{display:grid;gap:12px}.test-card{background:rgba(255,255,255,.96);border:1px solid var(--line);border-left-width:5px;border-radius:12px;overflow:hidden}.test-card[data-status="PASS"]{border-left-color:var(--pass)}.test-card[data-status="FAIL"]{border-left-color:var(--fail)}.test-card[data-status="SKIP"]{border-left-color:var(--skip)}.test-main{display:grid;grid-template-columns:minmax(0,1fr) auto;gap:16px;align-items:flex-start;padding:16px 18px}.test-title{display:flex;gap:12px;align-items:flex-start;min-width:0}.test-title h2{font-size:16px;margin:0;overflow-wrap:anywhere}.test-title p{margin:3px 0 0;color:var(--muted);overflow-wrap:anywhere}.test-metrics{display:grid;grid-template-columns:auto auto auto 120px;gap:8px;align-items:center;justify-content:end}.test-metrics span{background:var(--soft);border-radius:999px;padding:5px 9px;color:#344054;font-size:12px;white-space:nowrap}.duration-bar{display:block;width:120px;height:8px;background:#dfe7e3;border-radius:999px;overflow:hidden}.test-metrics .duration-bar{padding:0;background:#dfe7e3}.duration-bar i{display:block;height:100%;background:var(--brand-2);border-radius:999px}.badge{display:inline-flex;flex:0 0 auto;border-radius:999px;padding:4px 10px;font-weight:800;font-size:12px}.badge.pass{background:#dcfce7;color:var(--pass)}.badge.fail{background:#fee2e2;color:var(--fail)}.badge.skip{background:#fef3c7;color:var(--skip)}.badge.unknown{background:#e5e7eb;color:#374151}.quick-meta{padding:0 18px 14px}
            .detail{border-top:1px solid var(--line);padding:12px 18px 18px}.detail summary{cursor:pointer;color:var(--brand);font-weight:800;padding:7px 0}.detail[open] summary{border-bottom:1px solid var(--soft);margin-bottom:14px}.allure-panel,.unified-panel{margin-top:14px;border:1px solid var(--line);border-radius:10px;padding:14px;background:#fbfcfe}.unified-panel{background:#fff}.allure-panel h3,.unified-panel h3,.detail h4{margin:0 0 10px}.meta-grid,.label-grid,.param-grid{display:grid;grid-template-columns:repeat(3,minmax(0,1fr));gap:8px;margin:10px 0}.meta-grid span,.label-grid span,.param-grid span{background:#fff;border:1px solid var(--line);border-radius:8px;padding:8px;overflow-wrap:anywhere}.meta-grid b,.label-grid b,.param-grid b{display:block;color:var(--muted);font-size:12px}.description{background:#fff;border:1px solid var(--line);border-radius:8px;padding:10px;margin:10px 0}.failure{color:var(--fail);font-weight:700}pre{max-height:300px;overflow:auto;background:#111827;color:#e5e7eb;padding:12px;border-radius:8px;white-space:pre-wrap}.trace{display:inline-flex;margin-top:8px;color:var(--brand);font-weight:800}.empty{padding:24px;text-align:center;color:var(--muted)}
            .evidence-gallery{display:grid;grid-template-columns:repeat(auto-fill,minmax(240px,1fr));gap:14px;margin:10px 0 16px}.evidence-gallery figure{margin:0;border:1px solid var(--line);border-radius:10px;overflow:hidden;background:#fff}.evidence-gallery img{display:block;width:100%;height:184px;object-fit:contain;background:#0f172a}.evidence-gallery figcaption{padding:10px;font-weight:700}.evidence-gallery small{display:block;color:var(--muted);font-weight:500;overflow-wrap:anywhere}.attachments a,.links a{border:1px solid var(--line);border-radius:8px;background:#fff;text-decoration:none;color:var(--brand);padding:8px 10px}.attachments a span{display:block;color:var(--muted);font-size:12px}.steps{padding-left:20px}.steps li{margin:8px 0;padding:10px;border-left:3px solid var(--line);background:#fff;border-radius:8px}.step-head{display:flex;justify-content:space-between;gap:10px}.step-head em{font-style:normal;color:var(--muted);white-space:nowrap}
            @media(prefers-reduced-motion:no-preference){.hero,.summary,.run-meta,.insights,.toolbar,.test-card{animation:rise .28s ease-out both}.health{animation:focusRing .55s ease-out both}.test-card,.panel{transition:transform .16s ease,box-shadow .16s ease,border-color .16s ease}.test-card:hover,.panel:hover{transform:translateY(-1px);box-shadow:0 12px 28px rgba(16,40,34,.07)}.detail[open]>*:not(summary){animation:detailIn .18s ease-out}@keyframes rise{from{opacity:0;transform:translateY(8px)}to{opacity:1;transform:none}}@keyframes focusRing{from{filter:saturate(.7);transform:scale(.97)}to{filter:saturate(1);transform:scale(1)}}@keyframes detailIn{from{opacity:0;transform:translateY(-4px)}to{opacity:1;transform:none}}}
            @media(max-width:1180px){.summary{grid-template-columns:repeat(3,minmax(0,1fr))}.stat:nth-child(4){border-left:0}.run-meta{grid-template-columns:repeat(3,minmax(0,1fr))}.meta-pill.wide{grid-column:auto}.insights{grid-template-columns:repeat(6,minmax(0,1fr))}.panel,.panel:nth-child(-n+2){grid-column:span 3}.panel:nth-child(5){grid-column:span 6}.toolbar{grid-template-columns:1fr 1fr 1fr}.toolbar .toggle{justify-content:center}}
            @media(max-width:760px){.shell{padding:14px}.hero{grid-template-columns:1fr;border-radius:14px;padding:22px}.health{width:138px;margin-top:4px}.summary,.run-meta,.insights,.toolbar{grid-template-columns:1fr}.stat,.stat:nth-child(4){border-left:0;border-top:1px solid var(--line)}.stat:first-child{border-top:0}.meta-pill.wide,.panel{grid-column:auto}.toolbar{position:static}.test-main{padding:14px}.test-metrics{grid-template-columns:auto auto auto;gap:7px}.test-metrics .duration-bar{grid-column:1/-1;width:100%}.quick-meta{padding:0 14px 12px}.detail{padding:10px 14px 14px}.meta-grid,.label-grid,.param-grid{grid-template-columns:1fr}.evidence-gallery{grid-template-columns:1fr}.step-head{display:grid}}
            """;
    }

    private static String script() {
        return """
            const rows=[...document.querySelectorAll('.row')];
            const search=document.getElementById('search');
            const statusFilter=document.getElementById('statusFilter');
            const frameworkFilter=document.getElementById('frameworkFilter');
            const sortBy=document.getElementById('sortBy');
            const evidenceOnly=document.getElementById('evidenceOnly');
            const empty=document.getElementById('empty');
            function apply(){
              const q=search.value.toLowerCase();
              const status=statusFilter.value;
              const framework=frameworkFilter.value;
              let visible=0;
              rows.sort((a,b)=>{
                if(sortBy.value==='duration') return Number(b.dataset.duration)-Number(a.dataset.duration);
                if(sortBy.value==='status') {
                  const rank={FAIL:0,SKIP:1,PASS:2};
                  return (rank[a.dataset.status]??99)-(rank[b.dataset.status]??99);
                }
                if(sortBy.value==='evidence') return Number(b.dataset.evidence)-Number(a.dataset.evidence);
                return a.dataset.name.localeCompare(b.dataset.name);
              }).forEach(row=>{
                row.parentNode.appendChild(row);
                const ok=(!q||row.dataset.search.toLowerCase().includes(q))&&(status==='all'||row.dataset.status===status)&&(framework==='all'||row.dataset.framework===framework)&&(!evidenceOnly.checked||Number(row.dataset.evidence)>0);
                row.hidden=!ok; if(ok) visible++;
              });
              empty.hidden=visible!==0;
            }
            [search,statusFilter,frameworkFilter,sortBy,evidenceOnly].forEach(el=>el.addEventListener('input',apply));
            apply();
            """;
    }

    private static class EvidenceSummary {
        long allureTests;
        long steps;
        long attachments;
        long images;
    }

    private static class StatusCounts {
        int passed;
        int failed;
        int skipped;

        void add(String status) {
            if ("PASS".equalsIgnoreCase(status)) {
                passed++;
            } else if ("FAIL".equalsIgnoreCase(status)) {
                failed++;
            } else if ("SKIP".equalsIgnoreCase(status)) {
                skipped++;
            }
        }

        int total() {
            return passed + failed + skipped;
        }
    }
}
