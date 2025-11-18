package com.example.paypal.model;

import java.time.Instant;

public class PayPalCredential {

    private final String clientId;
    private final String clientSecret;
    private final EnvironmentMode mode;
    private final Instant createdAt;

    public PayPalCredential(String clientId, String clientSecret, EnvironmentMode mode) {
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.mode = mode;
        this.createdAt = Instant.now();
    }

    public String getClientId() {
        return clientId;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public EnvironmentMode getMode() {
        return mode;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}

