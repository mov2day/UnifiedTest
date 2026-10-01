package io.github.mov2day.unifiedtest.intelligence;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/** Creates deterministic, opaque identifiers without leaking failure details. */
public final class TestIdentity {
    private TestIdentity() {}

    public static String testId(String framework, String className, String testName) {
        return "ut-" + shortHash(normalize(framework) + "|" + normalize(className) + "|" + normalize(testName));
    }

    public static String testClassId(String framework, String className) {
        return "utc-" + shortHash(normalize(framework) + "|" + normalize(className));
    }

    public static String shortHash(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder();
            for (int i = 0; i < 12; i++) builder.append(String.format("%02x", digest[i]));
            return builder.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
