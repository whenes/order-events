package br.com.orderevents.payment.controller.dto;

import br.com.orderevents.payment.domain.Payment;
import br.com.orderevents.payment.domain.PaymentStatus;

import java.time.Instant;
import java.util.UUID;

public record PaymentResponse(
        UUID id,
        Long amount,
        String currency,
        PaymentStatus status,
        Instant createdAt
) {

    public static PaymentResponse from(Payment payment) {
        return new PaymentResponse(
                payment.getId(),
                payment.getAmount(),
                payment.getCurrency(),
                payment.getStatus(),
                payment.getCreatedAt()
        );
    }
}