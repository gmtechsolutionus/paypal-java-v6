package com.example.paypal.dto;

import com.example.paypal.model.EnvironmentMode;

import javax.validation.constraints.NotBlank;

public class ValidateCredentialsRequest {

    @NotBlank
    private String clientId;

    @NotBlank
    private String clientSecret;

    private String environment;

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
    }

    public String getEnvironment() {
        return environment;
    }

    public void setEnvironment(String environment) {
        this.environment = environment;
    }

    public EnvironmentMode resolveMode() {
        return EnvironmentMode.from(environment);
    }
}

