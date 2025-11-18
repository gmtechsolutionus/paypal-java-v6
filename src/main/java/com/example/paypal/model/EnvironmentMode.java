package com.example.paypal.model;

public enum EnvironmentMode {
    SANDBOX,
    LIVE;

    public static EnvironmentMode from(String value) {
        if (value == null) {
            return SANDBOX;
        }
        String normalized = value.trim().toUpperCase();
        return "LIVE".equals(normalized) ? LIVE : SANDBOX;
    }
}

