package br.com.orderevents.payment.service;

import br.com.orderevents.payment.domain.Payment;

public record CreatePaymentResult(
        Payment payment,
        boolean created
) {}