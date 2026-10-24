package br.com.orderevents.payment.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PaymentTest {

    @Test
    void shouldThrowExceptionWhenAmountIsNull() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new Payment(null, "BRL", "key-123")
        );

        assertEquals(
                "Payment amount must be greater than zero",
                exception.getMessage()
        );
    }

    @Test
    void shouldThrowExceptionWhenAmountIsZero() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new Payment(0L, "BRL", "key-123")
        );
    }

    @Test
    void shouldThrowExceptionWhenAmountIsNegative() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new Payment(-100L, "BRL", "key-123")
        );
    }

    @Test
    void shouldCreatePaymentWhenAmountIsPositive() {
        Payment payment = new Payment(15000L, "BRL", "key-123");

        assertNotNull(payment);
        assertEquals(15000L, payment.getAmount());
        assertEquals("BRL", payment.getCurrency());
        assertEquals("key-123", payment.getIdempotencyKey());
        assertEquals(PaymentStatus.PENDING, payment.getStatus());
    }
}