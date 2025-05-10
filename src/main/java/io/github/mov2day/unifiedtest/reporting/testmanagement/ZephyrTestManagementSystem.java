package io.github.mov2day.unifiedtest.reporting.testmanagement;

import io.github.mov2day.unifiedtest.reporting.UnifiedTestResult;
import io.github.mov2day.unifiedtest.extension.TestManagementExtension.ZephyrConfig;
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
 * Implementation of TestManagementSystem for Jira Zephyr.
 * Supports both Zephyr Cloud and Data Server API.
 */
public class ZephyrTestManagementSystem extends AbstractTestManagementSystem {
    private static final Logger logger = Logging.getLogger(ZephyrTestManagementSystem.class);
    private static final String CLOUD_API_VERSION = "v2";
    private static final String STATUS_PASSED = "PASS";
    private static final String STATUS_FAILED = "FAIL";
    private static final String STATUS_SKIPPED = "SKIP";
    private static final String STATUS_BLOCKED = "BLOCKED";

    private HttpClient httpClient;
    private String baseUrl;
    private String apiKey;
    private String projectKey;
    private String testCycleName;
    private boolean createTestCases;
    private String apiType;
    private String apiVersion;
    private String username;
    private Gson gson;

    public ZephyrTestManagementSystem() {
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
        this.gson = new Gson();
    }

    @Override
    protected boolean validateConfig() {
        if (!(config instanceof ZephyrConfig)) {
            logger.error("Invalid configuration type for Zephyr");
            return false;
        }

        ZephyrConfig zephyrConfig = (ZephyrConfig) config;
        if (!zephyrConfig.getServerUrl().isPresent() || !zephyrConfig.getApiKey().isPresent() ||
            !zephyrConfig.getProjectKey().isPresent()) {
            logger.error("Missing required Zephyr configuration");
            return false;
        }

        this.baseUrl = zephyrConfig.getServerUrl().get();
        this.apiKey = zephyrConfig.getApiKey().get();
        this.projectKey = zephyrConfig.getProjectKey().get();
        this.testCycleName = zephyrConfig.getTestCycleName().getOrElse("Automated Test Run");
        this.createTestCases = zephyrConfig.getCreateTestCases().getOrElse(false);
        this.apiType = zephyrConfig.getApiType().get();
        this.apiVersion = zephyrConfig.getApiVersion().get();
        this.username = zephyrConfig.getUsername().getOrElse("");

        if ("server".equals(apiType) && username.isEmpty()) {
            logger.error("Username is required for Zephyr Server API");
            return false;
        }

        return true;
    }

    @Override
    protected Map<String, String> doPushResults(List<UnifiedTestResult> results) throws Exception {
        Map<String, String> testIds = new HashMap<>();
        
        // For server API, we need to create a test run first and then add all results to it
        if ("server".equals(apiType)) {
            String testRunKey = createServerTestRun();
            for (UnifiedTestResult result : results) {
                String testCaseKey = getOrCreateTestCase(result);
                testIds.put(result.getName(), testCaseKey);
                addTestResultToRun(testRunKey, testCaseKey, result);
            }
            updateTestRunStatus(testRunKey);
        } else {
            // Cloud API handling remains the same
            String testCycleId = createCloudTestCycle();
            for (UnifiedTestResult result : results) {
                String testCaseId = getOrCreateCloudTestCase(result);
                testIds.put(result.getName(), testCaseId);
                createCloudTestExecution(testCaseId, testCycleId, result);
            }
        }
        
        return testIds;
    }

    private String createTestCycle() throws Exception {
        if ("cloud".equals(apiType)) {
            return createCloudTestCycle();
        } else {
            return createServerTestCycle();
        }
    }

    private String createCloudTestCycle() throws Exception {
        JsonObject cycle = new JsonObject();
        cycle.addProperty("name", testCycleName);
        cycle.addProperty("projectKey", projectKey);
        cycle.addProperty("description", "Test cycle created by UnifiedTest");

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(baseUrl + "/" + CLOUD_API_VERSION + "/testcycles"))
            .header("Authorization", "Bearer " + apiKey)
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(cycle)))
            .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 201) {
            throw new Exception("Failed to create test cycle: " + response.body());
        }

        JsonObject responseJson = gson.fromJson(response.body(), JsonObject.class);
        return responseJson.get("id").getAsString();
    }

    private String createServerTestCycle() throws Exception {
        // First, get the project ID
        String projectId = getProjectId();
        
        JsonObject cycle = new JsonObject();
        cycle.addProperty("name", testCycleName);
        cycle.addProperty("projectId", projectId);
        cycle.addProperty("description", "Test cycle created by UnifiedTest");
        cycle.addProperty("status", "ACTIVE");

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(baseUrl + "/rest/atm/1.0/testplan"))
            .header("Authorization", getAuthHeader())
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(cycle)))
            .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 201) {
            throw new Exception("Failed to create test cycle: " + response.body());
        }

        JsonObject responseJson = gson.fromJson(response.body(), JsonObject.class);
        return responseJson.get("id").getAsString();
    }

    private String getProjectId() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(baseUrl + "/rest/atm/1.0/testcase/search?projectKey=" + projectKey))
            .header("Authorization", getAuthHeader())
            .GET()
            .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new Exception("Failed to get project ID: " + response.body());
        }

        JsonObject responseJson = gson.fromJson(response.body(), JsonObject.class);
        return responseJson.get("projectId").getAsString();
    }

    private String getOrCreateTestCase(UnifiedTestResult result) throws Exception {
        if ("cloud".equals(apiType)) {
            return getOrCreateCloudTestCase(result);
        } else {
            return getOrCreateServerTestCase(result);
        }
    }

    private String getOrCreateCloudTestCase(UnifiedTestResult result) throws Exception {
        if (!createTestCases) {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/" + CLOUD_API_VERSION + "/testcases?projectKey=" + projectKey + "&name=" + result.getName()))
                .header("Authorization", "Bearer " + apiKey)
                .GET()
                .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                JsonObject responseJson = gson.fromJson(response.body(), JsonObject.class);
                JsonArray testCases = responseJson.getAsJsonArray("values");
                if (testCases.size() > 0) {
                    return testCases.get(0).getAsJsonObject().get("id").getAsString();
                }
            }
        }

        JsonObject testCase = new JsonObject();
        testCase.addProperty("name", result.getName());
        testCase.addProperty("projectKey", projectKey);
        testCase.addProperty("description", "Test case created by UnifiedTest");

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(baseUrl + "/" + CLOUD_API_VERSION + "/testcases"))
            .header("Authorization", "Bearer " + apiKey)
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(testCase)))
            .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 201) {
            throw new Exception("Failed to create test case: " + response.body());
        }

        JsonObject responseJson = gson.fromJson(response.body(), JsonObject.class);
        return responseJson.get("id").getAsString();
    }

    private String getOrCreateServerTestCase(UnifiedTestResult result) throws Exception {
        String projectId = getProjectId();
        
        if (!createTestCases) {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/rest/atm/1.0/testcase/search?query=" + result.getName()))
                .header("Authorization", getAuthHeader())
                .GET()
                .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                JsonObject responseJson = gson.fromJson(response.body(), JsonObject.class);
                JsonArray testCases = responseJson.getAsJsonArray("results");
                if (testCases.size() > 0) {
                    return testCases.get(0).getAsJsonObject().get("key").getAsString();
                }
            }
        }

        JsonObject testCase = new JsonObject();
        testCase.addProperty("name", result.getName());
        testCase.addProperty("projectId", projectId);
        testCase.addProperty("description", "Test case created by UnifiedTest");
        testCase.addProperty("status", "ACTIVE");
        testCase.addProperty("priority", "MEDIUM");

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(baseUrl + "/rest/atm/1.0/testcase"))
            .header("Authorization", getAuthHeader())
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(testCase)))
            .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 201) {
            throw new Exception("Failed to create test case: " + response.body());
        }

        JsonObject responseJson = gson.fromJson(response.body(), JsonObject.class);
        return responseJson.get("key").getAsString();
    }

    private void createTestExecution(String testCaseId, String testCycleId, UnifiedTestResult result) throws Exception {
        if ("cloud".equals(apiType)) {
            createCloudTestExecution(testCaseId, testCycleId, result);
        } else {
            createServerTestExecution(testCaseId, testCycleId, result);
        }
    }

    private void createCloudTestExecution(String testCaseId, String testCycleId, UnifiedTestResult result) throws Exception {
        JsonObject execution = new JsonObject();
        execution.addProperty("testCaseId", testCaseId);
        execution.addProperty("testCycleId", testCycleId);
        execution.addProperty("status", convertStatus(result.getStatus()));
        execution.addProperty("comment", result.getFailureMessage() != null ? result.getFailureMessage() : "");

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(baseUrl + "/" + CLOUD_API_VERSION + "/testexecutions"))
            .header("Authorization", "Bearer " + apiKey)
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(execution)))
            .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 201) {
            throw new Exception("Failed to create test execution: " + response.body());
        }
    }

    private String createServerTestRun() throws Exception {
        JsonObject testRun = new JsonObject();
        testRun.addProperty("projectKey", projectKey);
        testRun.addProperty("name", testCycleName);
        testRun.addProperty("status", "In Progress");
        
        // Add optional fields if available
        JsonObject customFields = new JsonObject();
        customFields.addProperty("automated", "true");
        customFields.addProperty("executionType", "Automated");
        testRun.add("customFields", customFields);
        
        // Add current date as planned start date
        String currentDate = java.time.OffsetDateTime.now().toString();
        testRun.addProperty("plannedStartDate", currentDate);
        
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(baseUrl + "/rest/atm/1.0/testrun"))
            .header("Authorization", getAuthHeader())
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(testRun)))
            .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 201) {
            throw new Exception("Failed to create test run: " + response.body());
        }

        JsonObject responseJson = gson.fromJson(response.body(), JsonObject.class);
        return responseJson.get("key").getAsString();
    }

    private void addTestResultToRun(String testRunKey, String testCaseKey, UnifiedTestResult result) throws Exception {
        JsonObject testResult = new JsonObject();
        testResult.addProperty("status", convertStatus(result.getStatus()));
        testResult.addProperty("comment", result.getFailureMessage() != null ? result.getFailureMessage() : "");
        testResult.addProperty("userKey", username);
        testResult.addProperty("executionDate", java.time.OffsetDateTime.now().toString());

        // Add custom fields
        JsonObject customFields = new JsonObject();
        customFields.addProperty("automated", "true");
        customFields.addProperty("executionType", "Automated");
        testResult.add("customFields", customFields);

        // Add environment info if available
        if (System.getProperty("os.name") != null) {
            testResult.addProperty("environment", System.getProperty("os.name"));
        }

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(baseUrl + "/rest/atm/1.0/testrun/" + testRunKey + "/testcase/" + testCaseKey + "/testresult"))
            .header("Authorization", getAuthHeader())
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(testResult)))
            .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 201) {
            throw new Exception("Failed to add test result: " + response.body());
        }
    }

    private void updateTestRunStatus(String testRunKey) throws Exception {
        JsonObject update = new JsonObject();
        update.addProperty("status", "Done");
        update.addProperty("plannedEndDate", java.time.OffsetDateTime.now().toString());

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(baseUrl + "/rest/atm/1.0/testrun/" + testRunKey))
            .header("Authorization", getAuthHeader())
            .header("Content-Type", "application/json")
            .PUT(HttpRequest.BodyPublishers.ofString(gson.toJson(update)))
            .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new Exception("Failed to update test run status: " + response.body());
        }
    }

    private void createServerTestExecution(String testCaseKey, String testRunKey, UnifiedTestResult result) throws Exception {
        // Create test run items array
        JsonArray items = new JsonArray();
        JsonObject item = new JsonObject();
        item.addProperty("testCaseKey", testCaseKey);
        item.addProperty("status", convertStatus(result.getStatus()));
        item.addProperty("comment", result.getFailureMessage() != null ? result.getFailureMessage() : "");
        item.addProperty("userKey", username);
        item.addProperty("executionDate", java.time.OffsetDateTime.now().toString());
        
        // Add custom fields
        JsonObject customFields = new JsonObject();
        customFields.addProperty("automated", "true");
        customFields.addProperty("executionType", "Automated");
        item.add("customFields", customFields);

        items.add(item);

        // Update test run with the new items
        JsonObject update = new JsonObject();
        update.add("items", items);

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(baseUrl + "/rest/atm/1.0/testrun/" + testRunKey))
            .header("Authorization", getAuthHeader())
            .header("Content-Type", "application/json")
            .PUT(HttpRequest.BodyPublishers.ofString(gson.toJson(update)))
            .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new Exception("Failed to update test run with results: " + response.body());
        }
    }

    private String getAuthHeader() {
        if ("cloud".equals(apiType)) {
            return "Bearer " + apiKey;
        } else {
            return "Basic " + Base64.getEncoder().encodeToString((username + ":" + apiKey).getBytes());
        }
    }

    @Override
    protected String convertStatus(String status) {
        if ("server".equals(apiType)) {
            // Server API uses different status values
            switch (status.toUpperCase()) {
                case "PASSED":
                case "PASS":
                    return "Pass";
                case "FAILED":
                case "FAIL":
                    return "Fail";
                case "SKIPPED":
                case "SKIP":
                    return "Not Executed";
                case "BLOCKED":
                    return "Blocked";
                default:
                    return "Fail";
            }
        } else {
            // Cloud API status values
            switch (status.toUpperCase()) {
                case "PASSED":
                case "PASS":
                    return STATUS_PASSED;
                case "FAILED":
                case "FAIL":
                    return STATUS_FAILED;
                case "SKIPPED":
                case "SKIP":
                    return STATUS_SKIPPED;
                case "BLOCKED":
                    return STATUS_BLOCKED;
                default:
                    return STATUS_FAILED;
            }
        }
    }

    @Override
    protected String convertFromStatus(String status) {
        switch (status.toUpperCase()) {
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
        return "zephyr";
    }
} 