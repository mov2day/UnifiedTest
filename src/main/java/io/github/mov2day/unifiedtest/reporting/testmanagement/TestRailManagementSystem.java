package io.github.mov2day.unifiedtest.reporting.testmanagement;

import io.github.mov2day.unifiedtest.reporting.UnifiedTestResult;
import io.github.mov2day.unifiedtest.extension.TestManagementExtension.TestRailConfig;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URI;
import java.time.Duration;
import java.util.Base64;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;

/**
 * Implementation of TestManagementSystem for TestRail.
 */
public class TestRailManagementSystem extends AbstractTestManagementSystem {
    private static final Logger logger = Logging.getLogger(TestRailManagementSystem.class);
    private static final String API_VERSION = "v2";
    private static final int STATUS_PASSED = 1;
    private static final int STATUS_FAILED = 5;
    private static final int STATUS_SKIPPED = 6;
    private static final int STATUS_BLOCKED = 2;

    private HttpClient httpClient;
    private String baseUrl;
    private String apiKey;
    private String projectId;
    private String suiteId;
    private boolean createTestCases;
    private Gson gson;

    public TestRailManagementSystem() {
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
        this.gson = new Gson();
    }

    @Override
    protected boolean validateConfig() {
        if (!(config instanceof TestRailConfig)) {
            logger.error("Invalid configuration type for TestRail");
            return false;
        }

        TestRailConfig testRailConfig = (TestRailConfig) config;
        if (!testRailConfig.getServerUrl().isPresent() || !testRailConfig.getApiKey().isPresent() ||
            !testRailConfig.getProjectId().isPresent() || !testRailConfig.getSuiteId().isPresent()) {
            logger.error("Missing required TestRail configuration");
            return false;
        }

        this.baseUrl = testRailConfig.getServerUrl().get();
        this.apiKey = testRailConfig.getApiKey().get();
        this.projectId = testRailConfig.getProjectId().get();
        this.suiteId = testRailConfig.getSuiteId().get();
        this.createTestCases = testRailConfig.getCreateTestCases().getOrElse(false);

        return true;
    }

    @Override
    protected Map<String, String> doPushResults(List<UnifiedTestResult> results) throws Exception {
        Map<String, String> testIds = new HashMap<>();
        String runId = createTestRun();

        for (UnifiedTestResult result : results) {
            String caseId = getOrCreateTestCase(result);
            testIds.put(result.getName(), caseId);
            addTestResult(caseId, runId, result);
        }

        return testIds;
    }

    private String createTestRun() throws Exception {
        JsonObject run = new JsonObject();
        run.addProperty("suite_id", suiteId);
        run.addProperty("name", "Automated Test Run");
        run.addProperty("description", "Test run created by UnifiedTest");
        run.addProperty("include_all", true);

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(baseUrl + "/index.php?/api/" + API_VERSION + "/add_run/" + projectId))
            .header("Authorization", "Basic " + Base64.getEncoder().encodeToString((apiKey + ":password").getBytes()))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(run)))
            .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new Exception("Failed to create test run: " + response.body());
        }

        JsonObject responseJson = gson.fromJson(response.body(), JsonObject.class);
        return responseJson.get("id").getAsString();
    }

    private String getOrCreateTestCase(UnifiedTestResult result) throws Exception {
        if (!createTestCases) {
            // Search for existing test case
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/index.php?/api/" + API_VERSION + "/get_cases/" + projectId + "&suite_id=" + suiteId))
                .header("Authorization", "Basic " + Base64.getEncoder().encodeToString((apiKey + ":password").getBytes()))
                .GET()
                .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                JsonArray cases = gson.fromJson(response.body(), JsonArray.class);
                for (int i = 0; i < cases.size(); i++) {
                    JsonObject testCase = cases.get(i).getAsJsonObject();
                    if (result.getName().equals(testCase.get("title").getAsString())) {
                        return testCase.get("id").getAsString();
                    }
                }
            }
        }

        // Create new test case
        JsonObject testCase = new JsonObject();
        testCase.addProperty("title", result.getName());
        testCase.addProperty("suite_id", suiteId);
        testCase.addProperty("type_id", 1); // Functional test case
        testCase.addProperty("priority_id", 2); // Medium priority

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(baseUrl + "/index.php?/api/" + API_VERSION + "/add_case/" + suiteId))
            .header("Authorization", "Basic " + Base64.getEncoder().encodeToString((apiKey + ":password").getBytes()))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(testCase)))
            .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new Exception("Failed to create test case: " + response.body());
        }

        JsonObject responseJson = gson.fromJson(response.body(), JsonObject.class);
        return responseJson.get("id").getAsString();
    }

    private void addTestResult(String caseId, String runId, UnifiedTestResult result) throws Exception {
        JsonObject testResult = new JsonObject();
        testResult.addProperty("status_id", convertStatus(result.getStatus()));
        testResult.addProperty("comment", result.getFailureMessage() != null ? result.getFailureMessage() : "");

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(baseUrl + "/index.php?/api/" + API_VERSION + "/add_result_for_case/" + runId + "/" + caseId))
            .header("Authorization", "Basic " + Base64.getEncoder().encodeToString((apiKey + ":password").getBytes()))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(testResult)))
            .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new Exception("Failed to add test result: " + response.body());
        }
    }

    @Override
    protected String convertStatus(String status) {
        switch (status.toUpperCase()) {
            case "PASSED":
            case "PASS":
                return String.valueOf(STATUS_PASSED);
            case "FAILED":
            case "FAIL":
                return String.valueOf(STATUS_FAILED);
            case "SKIPPED":
            case "SKIP":
                return String.valueOf(STATUS_SKIPPED);
            case "BLOCKED":
                return String.valueOf(STATUS_BLOCKED);
            default:
                return String.valueOf(STATUS_FAILED);
        }
    }

    @Override
    protected String convertFromStatus(String status) {
        int statusId = Integer.parseInt(status);
        switch (statusId) {
            case STATUS_PASSED:
                return "PASSED";
            case STATUS_FAILED:
                return "FAILED";
            case STATUS_SKIPPED:
                return "SKIPPED";
            case STATUS_BLOCKED:
                return "BLOCKED";
            default:
                return "FAILED";
        }
    }

    @Override
    public String getName() {
        return "testrail";
    }
} 