package com.example.paypal.controller;

import com.example.paypal.dto.ValidateCredentialsRequest;
import com.example.paypal.dto.ValidateCredentialsResponse;
import com.example.paypal.model.EnvironmentMode;
import com.example.paypal.service.PayPalPaymentService;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

@RestController
@RequestMapping("/api/credentials")
@Validated
public class CredentialController {

    private final PayPalPaymentService paymentService;

    public CredentialController(PayPalPaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/validate")
    public ResponseEntity<ValidateCredentialsResponse> validate(@Valid @RequestBody ValidateCredentialsRequest request) {
        EnvironmentMode mode = request.resolveMode();
        String token = paymentService.validateAndStoreCredential(request.getClientId(), request.getClientSecret(), request.getEnvironment());
        ValidateCredentialsResponse response = new ValidateCredentialsResponse(true, token, mode, "Credential validated");
        return ResponseEntity.ok(response);
    }
}

