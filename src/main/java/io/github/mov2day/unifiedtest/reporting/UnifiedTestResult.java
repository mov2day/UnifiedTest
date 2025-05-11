package io.github.mov2day.unifiedtest.reporting;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

/**
 * Represents a unified test result that can be pushed to various test management systems.
 */
public class UnifiedTestResult {
    private final String name;
    private final String status;
    private final Instant startTime;
    private final Instant endTime;
    private final String failureMessage;
    private final String stackTrace;
    private final List<TestStep> steps;
    private final List<Attachment> attachments;
    private final Map<String, String> metadata;

    public UnifiedTestResult(String name, String status, Instant startTime, Instant endTime) {
        this.name = name;
        this.status = status;
        this.startTime = startTime;
        this.endTime = endTime;
        this.failureMessage = null;
        this.stackTrace = null;
        this.steps = new ArrayList<>();
        this.attachments = new ArrayList<>();
        this.metadata = new HashMap<>();
    }

    public UnifiedTestResult(String name, String status, Instant startTime, Instant endTime,
                           String failureMessage, String stackTrace) {
        this.name = name;
        this.status = status;
        this.startTime = startTime;
        this.endTime = endTime;
        this.failureMessage = failureMessage;
        this.stackTrace = stackTrace;
        this.steps = new ArrayList<>();
        this.attachments = new ArrayList<>();
        this.metadata = new HashMap<>();
    }

    public String getName() {
        return name;
    }

    public String getStatus() {
        return status;
    }

    public Instant getStartTime() {
        return startTime;
    }

    public Instant getEndTime() {
        return endTime;
    }

    public String getFailureMessage() {
        return failureMessage;
    }

    public String getStackTrace() {
        return stackTrace;
    }

    public List<TestStep> getSteps() {
        return steps;
    }

    public List<Attachment> getAttachments() {
        return attachments;
    }

    public Map<String, String> getMetadata() {
        return metadata;
    }

    public void addStep(TestStep step) {
        steps.add(step);
    }

    public void addAttachment(Attachment attachment) {
        attachments.add(attachment);
    }

    public void addMetadata(String key, String value) {
        metadata.put(key, value);
    }

    public static class TestStep {
        private final String name;
        private final String status;
        private final Instant startTime;
        private final Instant endTime;
        private final String failureMessage;
        private final String stackTrace;

        public TestStep(String name, String status, Instant startTime, Instant endTime) {
            this(name, status, startTime, endTime, null, null);
        }

        public TestStep(String name, String status, Instant startTime, Instant endTime,
                       String failureMessage, String stackTrace) {
            this.name = name;
            this.status = status;
            this.startTime = startTime;
            this.endTime = endTime;
            this.failureMessage = failureMessage;
            this.stackTrace = stackTrace;
        }

        public String getName() {
            return name;
        }

        public String getStatus() {
            return status;
        }

        public Instant getStartTime() {
            return startTime;
        }

        public Instant getEndTime() {
            return endTime;
        }

        public String getFailureMessage() {
            return failureMessage;
        }

        public String getStackTrace() {
            return stackTrace;
        }
    }

    public static class Attachment {
        private final String name;
        private final String type;
        private final byte[] content;

        public Attachment(String name, String type, byte[] content) {
            this.name = name;
            this.type = type;
            this.content = content;
        }

        public String getName() {
            return name;
        }

        public String getType() {
            return type;
        }

        public byte[] getContent() {
            return content;
        }
    }
} 