package io.github.mov2day.unifiedtest.reporting;

import org.gradle.api.Project;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * Reads Allure result files and exposes the detail needed by the UnifiedTest HTML report.
 */
public class AllureReportReader {
    private static final String ALLURE_RESULTS_DIR = "allure-results";
    private static final String ALLURE_REPORT_DIR = "allure-report";
    private final Project project;
    private final Gson gson;
    private final Map<String, AllureTestResult> allureResults;

    public AllureReportReader(Project project) {
        this.project = project;
        this.gson = new Gson();
        this.allureResults = new HashMap<>();
    }

    /**
     * Checks if Allure reports are available in the project.
     * @return true if Allure reports are found
     */
    public boolean hasAllureReports() {
        File buildDir = project.getLayout().getBuildDirectory().get().getAsFile();
        return new File(buildDir, ALLURE_RESULTS_DIR).exists() || new File(buildDir, ALLURE_REPORT_DIR).exists();
    }

    /**
     * Reads and parses Allure test results.
     * @return map of test results keyed by common Allure and framework names
     */
    public Map<String, AllureTestResult> readAllureResults() {
        if (!hasAllureReports()) {
            return Collections.emptyMap();
        }

        File allureResultsDir = new File(project.getLayout().getBuildDirectory().get().getAsFile(), ALLURE_RESULTS_DIR);
        if (!allureResultsDir.exists()) {
            return Collections.emptyMap();
        }

        try {
            allureResults.clear();
            Files.walk(allureResultsDir.toPath())
                .filter(path -> path.toString().endsWith("-result.json"))
                .forEach(this::parseAllureResult);
            return allureResults;
        } catch (IOException e) {
            project.getLogger().error("Failed to read Allure results", e);
            return Collections.emptyMap();
        }
    }

    private void parseAllureResult(Path resultFile) {
        try {
            JsonObject result = gson.fromJson(Files.readString(resultFile), JsonObject.class);
            if (result == null) {
                return;
            }

            String name = stringValue(result, "name", resultFile.getFileName().toString());
            String fullName = stringValue(result, "fullName", name);
            AllureTestResult testResult = new AllureTestResult(
                stringValue(result, "uuid", ""),
                stringValue(result, "historyId", ""),
                stringValue(result, "testCaseId", ""),
                name,
                fullName,
                stringValue(result, "status", "unknown"),
                stringValue(result, "stage", "unknown"),
                stringValue(result, "description", ""),
                stringValue(result, "descriptionHtml", ""),
                longValue(result, "start", 0L),
                longValue(result, "stop", 0L)
            );

            parseLabels(result, testResult);
            parseLinks(result, testResult);
            parseParameters(result, testResult);
            parseStatusDetails(result, testResult);
            parseAttachments(result, resultFile.getParent(), testResult);
            parseSteps(result, resultFile.getParent(), testResult);
            indexResult(name, fullName, testResult);

            project.getLogger().info("Parsed Allure result: fullName={}, name={}, status={}, steps={}, attachments={}",
                fullName, name, testResult.getStatus(), testResult.getTotalStepCount(), testResult.getAllAttachments().size());
        } catch (IOException | RuntimeException e) {
            project.getLogger().error("Failed to parse Allure result file: " + resultFile, e);
        }
    }

    private void indexResult(String name, String fullName, AllureTestResult testResult) {
        putIfNotBlank(fullName, testResult);
        putIfNotBlank(name, testResult);
        putIfNotBlank(simpleName(fullName), testResult);
        String feature = testResult.getFirstLabel("feature");
        if (!feature.isBlank()) {
            putIfNotBlank(feature + "." + name, testResult);
            putIfNotBlank(feature + "." + fullName, testResult);
        }
    }

    private void putIfNotBlank(String key, AllureTestResult result) {
        if (key != null && !key.isBlank()) {
            allureResults.put(key, result);
        }
    }

    private String simpleName(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        int index = value.lastIndexOf('.');
        return index >= 0 ? value.substring(index + 1) : value;
    }

    private void parseLabels(JsonObject result, AllureTestResult testResult) {
        JsonArray labels = arrayValue(result, "labels");
        if (labels == null) {
            return;
        }
        for (JsonElement element : labels) {
            JsonObject label = element.getAsJsonObject();
            testResult.addLabel(stringValue(label, "name", ""), stringValue(label, "value", ""));
        }
    }

    private void parseLinks(JsonObject result, AllureTestResult testResult) {
        JsonArray links = arrayValue(result, "links");
        if (links == null) {
            return;
        }
        for (JsonElement element : links) {
            JsonObject link = element.getAsJsonObject();
            testResult.addLink(
                stringValue(link, "name", stringValue(link, "url", "")),
                stringValue(link, "url", ""),
                stringValue(link, "type", "link")
            );
        }
    }

    private void parseParameters(JsonObject result, HasParameters target) {
        JsonArray parameters = arrayValue(result, "parameters");
        if (parameters == null) {
            return;
        }
        for (JsonElement element : parameters) {
            JsonObject parameter = element.getAsJsonObject();
            target.addParameter(
                stringValue(parameter, "name", ""),
                stringValue(parameter, "value", ""),
                stringValue(parameter, "mode", "default"),
                booleanValue(parameter, "excluded", false)
            );
        }
    }

    private void parseStatusDetails(JsonObject result, AllureTestResult testResult) {
        JsonObject details = objectValue(result, "statusDetails");
        if (details == null) {
            return;
        }
        testResult.setStatusDetails(
            stringValue(details, "message", ""),
            stringValue(details, "trace", ""),
            booleanValue(details, "known", false),
            booleanValue(details, "muted", false),
            booleanValue(details, "flaky", false)
        );
    }

    private void parseAttachments(JsonObject source, Path resultDir, HasAttachments target) {
        JsonArray attachments = arrayValue(source, "attachments");
        if (attachments == null) {
            return;
        }
        for (JsonElement element : attachments) {
            JsonObject attachment = element.getAsJsonObject();
            target.addAttachment(new Attachment(
                stringValue(attachment, "source", ""),
                stringValue(attachment, "name", "attachment"),
                stringValue(attachment, "type", "application/octet-stream"),
                resultDir
            ));
        }
    }

    private void parseSteps(JsonObject result, Path resultDir, AllureTestResult testResult) {
        JsonArray steps = arrayValue(result, "steps");
        if (steps == null) {
            return;
        }
        for (JsonElement element : steps) {
            testResult.addStep(parseStep(element.getAsJsonObject(), resultDir));
        }
    }

    private Step parseStep(JsonObject stepObject, Path resultDir) {
        Step step = new Step(
            stringValue(stepObject, "name", "step"),
            stringValue(stepObject, "status", "unknown"),
            stringValue(stepObject, "stage", "unknown"),
            longValue(stepObject, "start", 0L),
            longValue(stepObject, "stop", 0L)
        );
        parseParameters(stepObject, step);
        parseAttachments(stepObject, resultDir, step);

        JsonArray nested = arrayValue(stepObject, "steps");
        if (nested != null) {
            for (JsonElement element : nested) {
                step.addStep(parseStep(element.getAsJsonObject(), resultDir));
            }
        }
        return step;
    }

    /**
     * Gets the path to the Allure report directory.
     * @return path to Allure report directory or null if not found
     */
    public String getAllureReportPath() {
        File allureReportDir = new File(project.getLayout().getBuildDirectory().get().getAsFile(), ALLURE_REPORT_DIR);
        return allureReportDir.exists() ? allureReportDir.getAbsolutePath() : null;
    }

    private static JsonArray arrayValue(JsonObject object, String key) {
        return object.has(key) && object.get(key).isJsonArray() ? object.getAsJsonArray(key) : null;
    }

    private static JsonObject objectValue(JsonObject object, String key) {
        return object.has(key) && object.get(key).isJsonObject() ? object.getAsJsonObject(key) : null;
    }

    private static String stringValue(JsonObject object, String key, String defaultValue) {
        return object.has(key) && !object.get(key).isJsonNull() ? object.get(key).getAsString() : defaultValue;
    }

    private static long longValue(JsonObject object, String key, long defaultValue) {
        return object.has(key) && !object.get(key).isJsonNull() ? object.get(key).getAsLong() : defaultValue;
    }

    private static boolean booleanValue(JsonObject object, String key, boolean defaultValue) {
        return object.has(key) && !object.get(key).isJsonNull() ? object.get(key).getAsBoolean() : defaultValue;
    }

    private interface HasParameters {
        void addParameter(String name, String value, String mode, boolean excluded);
    }

    private interface HasAttachments {
        void addAttachment(Attachment attachment);
    }

    /**
     * Represents one Allure result with metadata, steps, parameters, and evidence.
     */
    public static class AllureTestResult implements HasParameters, HasAttachments {
        private final String uuid;
        private final String historyId;
        private final String testCaseId;
        private final String name;
        private final String fullName;
        private final String status;
        private final String stage;
        private final String description;
        private final String descriptionHtml;
        private final long startTime;
        private final long endTime;
        private final Map<String, List<String>> labels = new LinkedHashMap<>();
        private final List<Link> links = new ArrayList<>();
        private final List<Parameter> parameters = new ArrayList<>();
        private final List<Step> steps = new ArrayList<>();
        private final List<Attachment> attachments = new ArrayList<>();
        private String statusMessage = "";
        private String statusTrace = "";
        private boolean known;
        private boolean muted;
        private boolean flaky;

        public AllureTestResult(String uuid, String historyId, String testCaseId, String name, String fullName,
                                String status, String stage, String description, String descriptionHtml,
                                long startTime, long endTime) {
            this.uuid = uuid;
            this.historyId = historyId;
            this.testCaseId = testCaseId;
            this.name = name;
            this.fullName = fullName;
            this.status = status;
            this.stage = stage;
            this.description = description;
            this.descriptionHtml = descriptionHtml;
            this.startTime = startTime;
            this.endTime = endTime;
        }

        public void addLabel(String name, String value) {
            if (name != null && !name.isBlank() && value != null && !value.isBlank()) {
                labels.computeIfAbsent(name, ignored -> new ArrayList<>()).add(value);
            }
        }

        public void addLink(String name, String url, String type) {
            if (url != null && !url.isBlank()) {
                links.add(new Link(name, url, type));
            }
        }

        @Override
        public void addParameter(String name, String value, String mode, boolean excluded) {
            if (name != null && !name.isBlank()) {
                parameters.add(new Parameter(name, value, mode, excluded));
            }
        }

        public void addStep(Step step) {
            steps.add(step);
        }

        @Override
        public void addAttachment(Attachment attachment) {
            attachments.add(attachment);
        }

        public void setStatusDetails(String message, String trace, boolean known, boolean muted, boolean flaky) {
            this.statusMessage = message;
            this.statusTrace = trace;
            this.known = known;
            this.muted = muted;
            this.flaky = flaky;
        }

        public String getUuid() { return uuid; }
        public String getHistoryId() { return historyId; }
        public String getTestCaseId() { return testCaseId; }
        public String getName() { return name; }
        public String getFullName() { return fullName; }
        public String getStatus() { return status; }
        public String getStage() { return stage; }
        public String getDescription() { return description; }
        public String getDescriptionHtml() { return descriptionHtml; }
        public long getStartTime() { return startTime; }
        public long getEndTime() { return endTime; }
        public String getStatusMessage() { return statusMessage; }
        public String getStatusTrace() { return statusTrace; }
        public boolean isKnown() { return known; }
        public boolean isMuted() { return muted; }
        public boolean isFlaky() { return flaky; }
        public Map<String, List<String>> getLabels() { return Collections.unmodifiableMap(labels); }
        public List<Link> getLinks() { return Collections.unmodifiableList(links); }
        public List<Parameter> getParameters() { return Collections.unmodifiableList(parameters); }
        public List<Step> getSteps() { return Collections.unmodifiableList(steps); }
        public List<Attachment> getAttachments() { return Collections.unmodifiableList(attachments); }
        public long getDuration() { return Math.max(0L, endTime - startTime); }

        public String getFirstLabel(String name) {
            List<String> values = labels.get(name);
            return values == null || values.isEmpty() ? "" : values.get(0);
        }

        public int getTotalStepCount() {
            int total = steps.size();
            for (Step step : steps) {
                total += step.getTotalStepCount();
            }
            return total;
        }

        public List<Attachment> getAllAttachments() {
            List<Attachment> all = new ArrayList<>(attachments);
            for (Step step : steps) {
                all.addAll(step.getAllAttachments());
            }
            return all;
        }

        public List<Attachment> getImageAttachments() {
            List<Attachment> images = new ArrayList<>();
            for (Attachment attachment : getAllAttachments()) {
                if (attachment.isImage()) {
                    images.add(attachment);
                }
            }
            return images;
        }
    }

    public static class Step implements HasParameters, HasAttachments {
        private final String name;
        private final String status;
        private final String stage;
        private final long startTime;
        private final long endTime;
        private final List<Parameter> parameters = new ArrayList<>();
        private final List<Attachment> attachments = new ArrayList<>();
        private final List<Step> steps = new ArrayList<>();

        public Step(String name, String status, String stage, long startTime, long endTime) {
            this.name = name;
            this.status = status;
            this.stage = stage;
            this.startTime = startTime;
            this.endTime = endTime;
        }

        @Override
        public void addParameter(String name, String value, String mode, boolean excluded) {
            if (name != null && !name.isBlank()) {
                parameters.add(new Parameter(name, value, mode, excluded));
            }
        }

        @Override
        public void addAttachment(Attachment attachment) {
            attachments.add(attachment);
        }

        public void addStep(Step step) {
            steps.add(step);
        }

        public String getName() { return name; }
        public String getStatus() { return status; }
        public String getStage() { return stage; }
        public long getStartTime() { return startTime; }
        public long getEndTime() { return endTime; }
        public long getDuration() { return Math.max(0L, endTime - startTime); }
        public List<Parameter> getParameters() { return Collections.unmodifiableList(parameters); }
        public List<Attachment> getAttachments() { return Collections.unmodifiableList(attachments); }
        public List<Step> getSteps() { return Collections.unmodifiableList(steps); }

        public int getTotalStepCount() {
            int total = steps.size();
            for (Step step : steps) {
                total += step.getTotalStepCount();
            }
            return total;
        }

        public List<Attachment> getAllAttachments() {
            List<Attachment> all = new ArrayList<>(attachments);
            for (Step step : steps) {
                all.addAll(step.getAllAttachments());
            }
            return all;
        }
    }

    public static class Attachment {
        private final String source;
        private final String name;
        private final String type;
        private final String fileUri;
        private final String absolutePath;

        public Attachment(String source, String name, String type, Path resultDir) {
            this.source = source;
            this.name = name;
            this.type = type == null || type.isBlank() ? "application/octet-stream" : type;
            if (source == null || source.isBlank()) {
                this.absolutePath = "";
                this.fileUri = "";
                return;
            }
            Path sourcePath = Path.of(source);
            Path resolved = sourcePath.isAbsolute() ? sourcePath : resultDir.resolve(source).normalize();
            this.absolutePath = resolved.toString();
            this.fileUri = resolved.toUri().toString();
        }

        public String getSource() { return source; }
        public String getName() { return name; }
        public String getType() { return type; }
        public String getFileUri() { return fileUri; }
        public String getAbsolutePath() { return absolutePath; }
        public boolean isImage() {
            String normalizedType = type.toLowerCase(Locale.ROOT);
            String normalizedSource = source == null ? "" : source.toLowerCase(Locale.ROOT);
            return normalizedType.startsWith("image/")
                || normalizedSource.endsWith(".png")
                || normalizedSource.endsWith(".jpg")
                || normalizedSource.endsWith(".jpeg")
                || normalizedSource.endsWith(".gif")
                || normalizedSource.endsWith(".webp")
                || normalizedSource.endsWith(".svg");
        }
    }

    public static class Parameter {
        private final String name;
        private final String value;
        private final String mode;
        private final boolean excluded;

        public Parameter(String name, String value, String mode, boolean excluded) {
            this.name = name;
            this.value = value;
            this.mode = mode;
            this.excluded = excluded;
        }

        public String getName() { return name; }
        public String getValue() { return value; }
        public String getMode() { return mode; }
        public boolean isExcluded() { return excluded; }
    }

    public static class Link {
        private final String name;
        private final String url;
        private final String type;

        public Link(String name, String url, String type) {
            this.name = name;
            this.url = url;
            this.type = type;
        }

        public String getName() { return name; }
        public String getUrl() { return url; }
        public String getType() { return type; }
    }
}
