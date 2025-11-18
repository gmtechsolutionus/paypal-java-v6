package com.example.paypal.config;

import com.example.paypal.model.EnvironmentMode;
import com.example.paypal.model.PayPalCredential;
import com.paypal.core.PayPalEnvironment;
import com.paypal.core.PayPalHttpClient;
import org.springframework.stereotype.Component;

@Component
public class PayPalClientFactory {

    public PayPalHttpClient fromCredential(PayPalCredential credential) {
        PayPalEnvironment environment = toEnvironment(credential);
        return new PayPalHttpClient(environment);
    }

    private PayPalEnvironment toEnvironment(PayPalCredential credential) {
        EnvironmentMode mode = credential.getMode();
        if (mode == EnvironmentMode.LIVE) {
            return new PayPalEnvironment.Live(credential.getClientId(), credential.getClientSecret());
        }
        return new PayPalEnvironment.Sandbox(credential.getClientId(), credential.getClientSecret());
    }
}

