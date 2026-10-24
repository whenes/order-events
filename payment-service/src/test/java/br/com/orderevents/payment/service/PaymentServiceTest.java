package br.com.orderevents.payment.service;

import br.com.orderevents.payment.domain.Payment;
import br.com.orderevents.payment.domain.PaymentStatus;
import br.com.orderevents.payment.repository.PaymentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

import org.springframework.dao.DataIntegrityViolationException;
import static org.mockito.Mockito.doThrow;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {
    @Mock
    private PaymentRepository paymentRepository;

    @InjectMocks
    private PaymentService paymentService;

    @Mock
    private PaymentCreationService paymentCreationService;

    @Test
    void shouldCreatePaymentWhenIdempotencyKeyDoesNotExist() {
        when(paymentRepository.findByIdempotencyKey("key-123"))
                .thenReturn(Optional.empty());

        Payment payment = new Payment(15000L, "BRL", "key-123");

        when(paymentCreationService.create(15000L, "BRL", "key-123"))
                .thenReturn(payment);

        CreatePaymentResult result = paymentService.createPayment(
                15000L, "BRL", "key-123"
        );

        assertTrue(result.created());
        assertEquals(payment, result.payment());
        assertEquals(PaymentStatus.PENDING, result.payment().getStatus());

        verify(paymentCreationService)
                .create(15000L, "BRL", "key-123");
    }

    @Test
    void shouldReturnExistingPaymentWhenRequestHasSameData() {
        Payment existingPayment = new Payment(15000L, "BRL", "key-123");

        when(paymentRepository.findByIdempotencyKey("key-123"))
                .thenReturn(Optional.of(existingPayment));

        CreatePaymentResult result = paymentService.createPayment(
                15000L, "BRL", "key-123"
        );

        assertFalse(result.created());
        assertEquals(existingPayment, result.payment());

        verify(paymentRepository, never())
                .save(org.mockito.ArgumentMatchers.any(Payment.class));
    }

    @Test
    void shouldThrowConflictWhenRequestHasDifferentAmount() {
        Payment existingPayment = new Payment(15000L, "BRL", "key-123");

        when(paymentRepository.findByIdempotencyKey("key-123"))
                .thenReturn(Optional.of(existingPayment));

        assertThrows(
                IdempotencyConflictException.class,
                () -> paymentService.createPayment(25000L, "BRL", "key-123")
        );

        verify(paymentRepository, never())
                .save(org.mockito.ArgumentMatchers.any(Payment.class));
    }

    @Test
    void shouldRecoverPaymentWhenConcurrentRequestViolatesUniqueConstraint() {
        when(paymentRepository.findByIdempotencyKey("key-123"))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(
                        new Payment(15000L, "BRL", "key-123")
                ));

        when(paymentCreationService.create(15000L, "BRL", "key-123"))
                .thenThrow(new DataIntegrityViolationException(
                        "Duplicate idempotency key"
                ));

        CreatePaymentResult result = paymentService.createPayment(
                15000L, "BRL", "key-123"
        );

        assertFalse(result.created());
        assertEquals(15000L, result.payment().getAmount());
        assertEquals("BRL", result.payment().getCurrency());
    }
}
