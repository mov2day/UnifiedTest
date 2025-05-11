package io.github.mov2day.unifiedtest.reporting.testmanagement;

/**
 * Represents the status of an operation performed by a test management system.
 */
public class OperationStatus {
    private final boolean success;
    private final String message;
    private final Throwable error;

    public OperationStatus(boolean success, String message) {
        this(success, message, null);
    }

    public OperationStatus(boolean success, String message, Throwable error) {
        this.success = success;
        this.message = message;
        this.error = error;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public Throwable getError() {
        return error;
    }

    public static OperationStatus success(String message) {
        return new OperationStatus(true, message);
    }

    public static OperationStatus failure(String message) {
        return new OperationStatus(false, message);
    }

    public static OperationStatus failure(String message, Throwable error) {
        return new OperationStatus(false, message, error);
    }
} 