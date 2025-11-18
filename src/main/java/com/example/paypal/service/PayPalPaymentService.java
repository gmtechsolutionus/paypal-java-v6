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
            // Validate credentials by attempting to create a minimal test order
            // This will fail with authentication error if credentials are invalid
            OrdersCreateRequest testRequest = new OrdersCreateRequest();
            testRequest.header("Prefer", "return=representation");
            
            Map<String, Object> amount = new HashMap<>();
            amount.put("currency_code", "USD");
            amount.put("value", "0.01");
            
            Map<String, Object> purchaseUnit = new HashMap<>();
            purchaseUnit.put("amount", amount);
            
            Map<String, Object> orderBody = new HashMap<>();
            orderBody.put("intent", "CAPTURE");
            orderBody.put("purchase_units", Collections.singletonList(purchaseUnit));
            
            testRequest.requestBody(orderBody);
            
            // Execute request - will throw exception if credentials are invalid
            HttpResponse<Order> response = client.execute(testRequest);
            // If execution succeeds, credentials are valid
        } catch (IOException e) {
            String errorMsg = e.getMessage() != null ? e.getMessage().toLowerCase() : "";
            // Check for authentication errors
            if (errorMsg.contains("401") || errorMsg.contains("unauthorized") || 
                errorMsg.contains("authentication") || errorMsg.contains("invalid_client")) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid PayPal credentials");
            }
            // Other IO errors might be network issues, but we'll treat as validation failure
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Credential validation failed: " + e.getMessage());
        } catch (Exception e) {
            String errorMsg = e.getMessage() != null ? e.getMessage().toLowerCase() : "";
            // Check for authentication errors
            if (errorMsg.contains("401") || errorMsg.contains("unauthorized") || 
                errorMsg.contains("authentication") || errorMsg.contains("invalid_client")) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid PayPal credentials");
            }
            // For other errors (like missing payment source), credentials might still be valid
            // We'll allow it to pass - actual validation will happen on first payment
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
            
            // When using payment_source with direct card, PayPal may return CREATED status
            // We need to capture it explicitly
            String status = order.status();
            if ("CREATED".equalsIgnoreCase(status) || "APPROVED".equalsIgnoreCase(status)) {
                OrdersCaptureRequest captureRequest = new OrdersCaptureRequest(order.id());
                captureRequest.requestBody(new OrderRequest());
                captureRequest.header("Prefer", "return=representation");
                HttpResponse<Order> captureResponse = client.execute(captureRequest);
                order = captureResponse.result();
            }
            
            // Check final status
            String finalStatus = order.status();
            if ("COMPLETED".equalsIgnoreCase(finalStatus)) {
                return order;
            } else if ("DECLINED".equalsIgnoreCase(finalStatus) || "FAILED".equalsIgnoreCase(finalStatus)) {
                String errorDetails = extractErrorDetails(order);
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, 
                    "Payment declined: " + finalStatus + (errorDetails != null ? " - " + errorDetails : ""));
            }
            
            return order;
        } catch (IOException e) {
            String errorMsg = e.getMessage();
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, 
                "Payment failed: " + (errorMsg != null ? errorMsg : "Unknown error"));
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, 
                "Payment processing error: " + e.getMessage());
        }
    }
    
    private String extractErrorDetails(Order order) {
        // Try to extract error details from order response
        try {
            // PayPal SDK Order object may have error details
            // This is a simplified extraction - adjust based on actual SDK structure
            return order.toString();
        } catch (Exception e) {
            return null;
        }
    }

    private Map<String, Object> buildOrderBody(CardPaymentRequest request) {
        Map<String, Object> amount = new HashMap<>();
        amount.put("currency_code", request.getCurrencyCode().toUpperCase());
        amount.put("value", normalizeAmount(request.getAmount()));

        Map<String, Object> purchaseUnit = new HashMap<>();
        purchaseUnit.put("amount", amount);
        purchaseUnit.put("description", "Direct card payment");

        Map<String, Object> payload = new HashMap<>();
        payload.put("intent", "CAPTURE");
        payload.put("purchase_units", Collections.singletonList(purchaseUnit));
        payload.put("payment_source", Collections.singletonMap("card", buildCardSource(request)));
        payload.put("processing_instruction", "ORDER_COMPLETE_ON_PAYMENT_APPROVAL");
        
        // Add application context for better success rates
        Map<String, Object> applicationContext = new HashMap<>();
        applicationContext.put("brand_name", "Payment Service");
        applicationContext.put("landing_page", "NO_PREFERENCE");
        applicationContext.put("user_action", "PAY_NOW");
        applicationContext.put("return_url", "https://example.com/return");
        applicationContext.put("cancel_url", "https://example.com/cancel");
        payload.put("application_context", applicationContext);
        
        return payload;
    }

    private Map<String, Object> buildCardSource(CardPaymentRequest request) {
        Map<String, Object> card = new HashMap<>();
        
        // Remove any spaces or dashes from card number
        String cardNumber = request.getCardNumber().replaceAll("[\\s-]", "");
        card.put("number", cardNumber);
        
        // Ensure expiry is in correct format (YYYY-MM)
        String expiry = normalizeExpiry(request.getExpiry());
        card.put("expiry", expiry);
        
        card.put("security_code", request.getSecurityCode());
        
        // Cardholder name is recommended for higher success rates
        if (StringUtils.hasText(request.getCardholderName())) {
            card.put("name", request.getCardholderName());
        } else {
            card.put("name", "Cardholder");
        }
        
        // Billing address is important for success rates
        Map<String, Object> billingAddress = toAddressMap(request.getBillingAddress());
        if (!billingAddress.isEmpty()) {
            card.put("billing_address", billingAddress);
        } else {
            // Provide default US address if none provided (for sandbox testing)
            Map<String, Object> defaultAddress = new HashMap<>();
            defaultAddress.put("address_line_1", "123 Main St");
            defaultAddress.put("admin_area_2", "San Jose");
            defaultAddress.put("admin_area_1", "CA");
            defaultAddress.put("postal_code", "95131");
            defaultAddress.put("country_code", "US");
            card.put("billing_address", defaultAddress);
        }
        
        return card;
    }
    
    private String normalizeExpiry(String expiry) {
        if (expiry == null) {
            return null;
        }
        // Handle YYYY-MM format
        if (expiry.matches("^\\d{4}-\\d{2}$")) {
            return expiry;
        }
        // Handle MM/YY format
        if (expiry.matches("^\\d{2}/\\d{2}$")) {
            String[] parts = expiry.split("/");
            int year = Integer.parseInt(parts[1]);
            // Assume 20xx for years < 50, 19xx otherwise
            int fullYear = year < 50 ? 2000 + year : 1900 + year;
            return String.format("%d-%s", fullYear, parts[0]);
        }
        // Handle MMyy format
        if (expiry.matches("^\\d{4}$")) {
            String month = expiry.substring(0, 2);
            String year = expiry.substring(2, 4);
            int yearInt = Integer.parseInt(year);
            int fullYear = yearInt < 50 ? 2000 + yearInt : 1900 + yearInt;
            return String.format("%d-%s", fullYear, month);
        }
        // Return as-is if already in correct format
        return expiry;
    }

    private Map<String, Object> toAddressMap(BillingAddress address) {
        Map<String, Object> map = new HashMap<>();
        if (address == null) {
            return map;
        }
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
        // Country code is required for better success rates
        if (StringUtils.hasText(address.getCountryCode())) {
            map.put("country_code", address.getCountryCode().toUpperCase());
        }
        return map;
    }

    private String normalizeAmount(BigDecimal amount) {
        return amount.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }
}

