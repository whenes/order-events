package br.com.orderevents.payment.controller.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CreatePaymentRequest(

        @NotNull(message = "Payment amount is required")
        @Positive(message = "Payment amount must be greater than zero")
        Long amount,

        @NotBlank(message = "Payment currency is required")
        String currency
) {}