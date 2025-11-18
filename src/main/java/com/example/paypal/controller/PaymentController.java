package com.example.paypal.controller;

import com.example.paypal.dto.CardPaymentRequest;
import com.example.paypal.dto.CardPaymentResponse;
import com.example.paypal.service.PayPalPaymentService;
import com.paypal.orders.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

@RestController
@RequestMapping("/api/payment")
@Validated
public class PaymentController {

    private final PayPalPaymentService paymentService;

    public PaymentController(PayPalPaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/process")
    public ResponseEntity<CardPaymentResponse> process(@Valid @RequestBody CardPaymentRequest request) {
        Order order = paymentService.processDirectCardPayment(request);
        CardPaymentResponse response = new CardPaymentResponse(order.status(), order.id(), order);
        return ResponseEntity.ok(response);
    }
}

