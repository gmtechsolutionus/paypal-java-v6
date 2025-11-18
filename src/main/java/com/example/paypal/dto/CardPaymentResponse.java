package com.example.paypal.dto;

public class CardPaymentResponse {

    private final String status;
    private final String orderId;
    private final Object rawResponse;

    public CardPaymentResponse(String status, String orderId, Object rawResponse) {
        this.status = status;
        this.orderId = orderId;
        this.rawResponse = rawResponse;
    }

    public String getStatus() {
        return status;
    }

    public String getOrderId() {
        return orderId;
    }

    public Object getRawResponse() {
        return rawResponse;
    }
}

