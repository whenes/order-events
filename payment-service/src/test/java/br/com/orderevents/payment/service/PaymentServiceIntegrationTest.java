package br.com.orderevents.payment.service;

import br.com.orderevents.payment.domain.Payment;
import br.com.orderevents.payment.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Transactional
class PaymentServiceIntegrationTest {
    @Autowired
    private PaymentService paymentService;

    @Autowired
    private PaymentRepository paymentRepository;

    @BeforeEach
    void setUp() {
        paymentRepository.deleteAll();
    }

    @Test
    void shouldPersistPaymentInDatabase() {
        String idempotencyKey = "integration-" + UUID.randomUUID();

        CreatePaymentResult result = paymentService.createPayment(
                15000L, "BRL", idempotencyKey
        );

        assertTrue(result.created());
        assertNotNull(result.payment().getId());

        Payment persistedPayment = paymentRepository
                .findByIdempotencyKey(idempotencyKey)
                .orElseThrow();

        assertEquals(result.payment().getId(), persistedPayment.getId());
        assertEquals(15000L, persistedPayment.getAmount());
        assertEquals("BRL", persistedPayment.getCurrency());
    }

    @Test
    void shouldReuseExistingPaymentForSameIdempotencyKeyAndData() {
        String idempotencyKey = "integration-" + UUID.randomUUID();

        CreatePaymentResult firstResult = paymentService.createPayment(
                15000L, "BRL", idempotencyKey
        );

        CreatePaymentResult secondResult = paymentService.createPayment(
                15000L, "BRL", idempotencyKey
        );

        assertTrue(firstResult.created());
        assertFalse(secondResult.created());
        assertEquals(firstResult.payment().getId(), secondResult.payment().getId());

        assertEquals(1, paymentRepository.count());
    }

    @Test
    void shouldRejectDifferentDataForSameIdempotencyKey() {
        String idempotencyKey = "integration-" + UUID.randomUUID();

        paymentService.createPayment(15000L, "BRL", idempotencyKey);

        assertThrows(
                IdempotencyConflictException.class,
                () -> paymentService.createPayment(
                        25000L, "BRL", idempotencyKey
                )
        );

        assertEquals(1, paymentRepository.count());
    }
}
