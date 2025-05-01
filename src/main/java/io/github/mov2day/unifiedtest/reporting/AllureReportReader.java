package io.github.mov2day.unifiedtest.reporting;

import org.gradle.api.Project;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

/**
 * Reads and parses Allure test reports.
 * Provides functionality to detect and process Allure report data.
 */
public class AllureReportReader {
    private static final String ALLURE_RESULTS_DIR = "allure-results";
    private static final String ALLURE_REPORT_DIR = "allure-report";
    private final Project project;
    private final Gson gson;
    private Map<String, AllureTestResult> allureResults;

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
        File allureResultsDir = new File(project.getBuildDir(), ALLURE_RESULTS_DIR);
        File allureReportDir = new File(project.getBuildDir(), ALLURE_REPORT_DIR);
        return allureResultsDir.exists() || allureReportDir.exists();
    }

    /**
     * Reads and parses Allure test results.
     * @return map of test results keyed by test name
     */
    public Map<String, AllureTestResult> readAllureResults() {
        if (!hasAllureReports()) {
            return Collections.emptyMap();
        }

        File allureResultsDir = new File(project.getBuildDir(), ALLURE_RESULTS_DIR);
        if (!allureResultsDir.exists()) {
            return Collections.emptyMap();
        }

        try {
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
            String content = Files.readString(resultFile);
            JsonObject result = gson.fromJson(content, JsonObject.class);
            
            // Extract full name including class name if available
            String name = result.get("name").getAsString();
            String fullName = name;
            if (result.has("fullName")) {
                fullName = result.get("fullName").getAsString();
            } else if (result.has("testClass")) {
                String testClass = result.get("testClass").getAsString();
                fullName = testClass + "." + name;
            }
            
            String status = result.get("status").getAsString();
            String stage = result.get("stage").getAsString();
            long start = result.get("start").getAsLong();
            long stop = result.get("stop").getAsLong();
            
            AllureTestResult testResult = new AllureTestResult(fullName, status, stage, start, stop);
            
            // Parse steps if present
            if (result.has("steps")) {
                JsonArray steps = result.getAsJsonArray("steps");
                steps.forEach(step -> {
                    JsonObject stepObj = step.getAsJsonObject();
                    String stepName = stepObj.get("name").getAsString();
                    String stepStatus = stepObj.get("status").getAsString();
                    testResult.addStep(stepName, stepStatus);
                });
            }
            
            // Parse attachments if present
            if (result.has("attachments")) {
                JsonArray attachments = result.getAsJsonArray("attachments");
                attachments.forEach(attachment -> {
                    JsonObject attObj = attachment.getAsJsonObject();
                    String source = attObj.get("source").getAsString();
                    String attachmentName = attObj.get("name").getAsString();
                    String type = attObj.get("type").getAsString();
                    testResult.addAttachment(source, attachmentName, type);
                });
            }

            // Add both by full name and simple name for better matching
            allureResults.put(fullName, testResult);
            allureResults.put(name, testResult);
            
            // Log for debugging
            project.getLogger().info("Parsed Allure result: fullName={}, name={}, status={}, steps={}, attachments={}", 
                fullName, name, status, testResult.getSteps().size(), testResult.getAttachments().size());
            
        } catch (IOException | RuntimeException e) {
            project.getLogger().error("Failed to parse Allure result file: " + resultFile, e);
        }
    }

    /**
     * Gets the path to the Allure report directory.
     * @return path to Allure report directory or null if not found
     */
    public String getAllureReportPath() {
        File allureReportDir = new File(project.getBuildDir(), ALLURE_REPORT_DIR);
        return allureReportDir.exists() ? allureReportDir.getAbsolutePath() : null;
    }

    /**
     * Represents a single Allure test result with its details.
     */
    public static class AllureTestResult {
        private final String name;
        private final String status;
        private final String stage;
        private final long startTime;
        private final long endTime;
        private final List<Step> steps;
        private final List<Attachment> attachments;

        public AllureTestResult(String name, String status, String stage, long startTime, long endTime) {
            this.name = name;
            this.status = status;
            this.stage = stage;
            this.startTime = startTime;
            this.endTime = endTime;
            this.steps = new ArrayList<>();
            this.attachments = new ArrayList<>();
        }

        public void addStep(String name, String status) {
            steps.add(new Step(name, status));
        }

        public void addAttachment(String source, String name, String type) {
            attachments.add(new Attachment(source, name, type));
        }

        public String getName() { return name; }
        public String getStatus() { return status; }
        public String getStage() { return stage; }
        public long getStartTime() { return startTime; }
        public long getEndTime() { return endTime; }
        public List<Step> getSteps() { return Collections.unmodifiableList(steps); }
        public List<Attachment> getAttachments() { return Collections.unmodifiableList(attachments); }
        public long getDuration() { return endTime - startTime; }

        public static class Step {
            private final String name;
            private final String status;

            public Step(String name, String status) {
                this.name = name;
                this.status = status;
            }

            public String getName() { return name; }
            public String getStatus() { return status; }
        }

        public static class Attachment {
            private final String source;
            private final String name;
            private final String type;

            public Attachment(String source, String name, String type) {
                this.source = source;
                this.name = name;
                this.type = type;
            }

            public String getSource() { return source; }
            public String getName() { return name; }
            public String getType() { return type; }
        }
    }
} 