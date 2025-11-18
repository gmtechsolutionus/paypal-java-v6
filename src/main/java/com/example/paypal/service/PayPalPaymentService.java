package com.example.paypal.service;

import com.example.paypal.config.CredentialStore;
import com.example.paypal.config.PayPalClientFactory;
import com.example.paypal.dto.BillingAddress;
import com.example.paypal.dto.CardPaymentRequest;
import com.example.paypal.model.EnvironmentMode;
import com.example.paypal.model.PayPalCredential;
import com.paypal.http.HttpResponse;
import com.paypal.orders.Order;
import com.paypal.orders.OrderRequest;
import com.paypal.orders.OrdersCaptureRequest;
import com.paypal.orders.OrdersCreateRequest;
import com.paypal.core.PayPalHttpClient;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
public class PayPalPaymentService {

    private final PayPalClientFactory clientFactory;
    private final CredentialStore credentialStore;

    public PayPalPaymentService(PayPalClientFactory clientFactory, CredentialStore credentialStore) {
        this.clientFactory = clientFactory;
        this.credentialStore = credentialStore;
    }

    public PayPalCredential requireCredential(String token) {
        return credentialStore.find(token)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid or expired credential token"));
    }

    public void verifyCredential(PayPalCredential credential) {
        PayPalHttpClient client = clientFactory.fromCredential(credential);
        try {
            client.fetchAccessToken();
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Credential validation failed: " + e.getMessage());
        }
    }

    public String validateAndStoreCredential(String clientId, String clientSecret, String environment) {
        PayPalCredential credential = new PayPalCredential(clientId, clientSecret, EnvironmentMode.from(environment));
        verifyCredential(credential);
        return credentialStore.save(credential);
    }

    public Order processDirectCardPayment(CardPaymentRequest request) {
        PayPalCredential credential = requireCredential(request.getCredentialToken());
        PayPalHttpClient client = clientFactory.fromCredential(credential);
        OrdersCreateRequest createRequest = new OrdersCreateRequest();
        createRequest.header("Prefer", "return=representation");
        createRequest.requestBody(buildOrderBody(request));
        try {
            HttpResponse<Order> createResponse = client.execute(createRequest);
            Order order = createResponse.result();
            if (!"COMPLETED".equalsIgnoreCase(order.status())) {
                OrdersCaptureRequest captureRequest = new OrdersCaptureRequest(order.id());
                captureRequest.requestBody(new OrderRequest());
                order = client.execute(captureRequest).result();
            }
            return order;
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Payment failed: " + e.getMessage());
        }
    }

    private Map<String, Object> buildOrderBody(CardPaymentRequest request) {
        Map<String, Object> amount = new HashMap<>();
        amount.put("currency_code", request.getCurrencyCode().toUpperCase());
        amount.put("value", normalizeAmount(request.getAmount()));

        Map<String, Object> purchaseUnit = new HashMap<>();
        purchaseUnit.put("amount", amount);

        Map<String, Object> payload = new HashMap<>();
        payload.put("intent", "CAPTURE");
        payload.put("purchase_units", Collections.singletonList(purchaseUnit));
        payload.put("payment_source", Collections.singletonMap("card", buildCardSource(request)));
        payload.put("processing_instruction", "ORDER_COMPLETE_ON_PAYMENT_APPROVAL");
        return payload;
    }

    private Map<String, Object> buildCardSource(CardPaymentRequest request) {
        Map<String, Object> card = new HashMap<>();
        card.put("number", request.getCardNumber());
        card.put("expiry", request.getExpiry());
        card.put("security_code", request.getSecurityCode());
        if (StringUtils.hasText(request.getCardholderName())) {
            card.put("name", request.getCardholderName());
        }
        Optional.ofNullable(request.getBillingAddress()).ifPresent(address -> card.put("billing_address", toAddressMap(address)));
        return card;
    }

    private Map<String, Object> toAddressMap(BillingAddress address) {
        Map<String, Object> map = new HashMap<>();
        if (StringUtils.hasText(address.getAddressLine1())) {
            map.put("address_line_1", address.getAddressLine1());
        }
        if (StringUtils.hasText(address.getAddressLine2())) {
            map.put("address_line_2", address.getAddressLine2());
        }
        if (StringUtils.hasText(address.getAdminArea1())) {
            map.put("admin_area_1", address.getAdminArea1());
        }
        if (StringUtils.hasText(address.getAdminArea2())) {
            map.put("admin_area_2", address.getAdminArea2());
        }
        if (StringUtils.hasText(address.getPostalCode())) {
            map.put("postal_code", address.getPostalCode());
        }
        if (StringUtils.hasText(address.getCountryCode())) {
            map.put("country_code", address.getCountryCode());
        }
        return map;
    }

    private String normalizeAmount(BigDecimal amount) {
        return amount.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }
}

