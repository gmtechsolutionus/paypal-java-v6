package com.example.paypal.dto;

import com.example.paypal.model.EnvironmentMode;

public class ValidateCredentialsResponse {

    private final boolean valid;
    private final String credentialToken;
    private final EnvironmentMode environment;
    private final String message;

    public ValidateCredentialsResponse(boolean valid, String credentialToken, EnvironmentMode environment, String message) {
        this.valid = valid;
        this.credentialToken = credentialToken;
        this.environment = environment;
        this.message = message;
    }

    public boolean isValid() {
        return valid;
    }

    public String getCredentialToken() {
        return credentialToken;
    }

    public EnvironmentMode getEnvironment() {
        return environment;
    }

    public String getMessage() {
        return message;
    }
}

