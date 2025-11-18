package com.example.paypal.config;

import com.example.paypal.model.PayPalCredential;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class CredentialStore {

    private static final Duration TTL = Duration.ofHours(12);
    private final Map<String, PayPalCredential> credentials = new ConcurrentHashMap<>();

    public String save(PayPalCredential credential) {
        String token = UUID.randomUUID().toString();
        credentials.put(token, credential);
        return token;
    }

    public Optional<PayPalCredential> find(String token) {
        if (token == null) {
            return Optional.empty();
        }
        PayPalCredential credential = credentials.get(token);
        if (credential == null) {
            return Optional.empty();
        }
        if (credential.getCreatedAt().isBefore(Instant.now().minus(TTL))) {
            credentials.remove(token);
            return Optional.empty();
        }
        return Optional.of(credential);
    }
}

