package com.example.employee.common;

public final class TextUtils {

    private TextUtils() {
    }

    public static String trimToEmpty(String value) {
        return value == null || value.isBlank() ? "" : value.trim();
    }

    public static String requireText(String value, String message) {
        String cleaned = trimToEmpty(value);
        if (cleaned.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return cleaned;
    }
}
