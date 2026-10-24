package br.com.orderevents.payment.service;

import br.com.orderevents.payment.repository.PaymentRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class PaymentConcurrencyIntegrationTest {
    @Autowired
    private PaymentService paymentService;

    @Autowired
    private PaymentRepository paymentRepository;

    @Test
    void shouldHandleConcurrentRequestsWithSameIdempotencyKey() throws Exception {
        String idempotencyKey = "concurrent-" + UUID.randomUUID();

        int numberOfRequests = 2;

        CountDownLatch ready = new CountDownLatch(numberOfRequests);
        CountDownLatch start = new CountDownLatch(1);

        ExecutorService executor = Executors.newFixedThreadPool(numberOfRequests);

        try {
            Future<CreatePaymentResult> firstRequest = executor.submit(() -> {
                ready.countDown();
                start.await();

                return paymentService.createPayment(
                        15000L, "BRL", idempotencyKey
                );
            });

            Future<CreatePaymentResult> secondRequest = executor.submit(() -> {
                ready.countDown();
                start.await();

                return paymentService.createPayment(
                        15000L, "BRL", idempotencyKey
                );
            });

            assertTrue(
                    ready.await(5, TimeUnit.SECONDS),
                    "Both requests should be ready to start"
            );

            start.countDown();

            CreatePaymentResult firstResult =
                    firstRequest.get(15, TimeUnit.SECONDS);

            CreatePaymentResult secondResult =
                    secondRequest.get(15, TimeUnit.SECONDS);

            assertNotNull(firstResult.payment().getId());
            assertNotNull(secondResult.payment().getId());

            assertEquals(
                    firstResult.payment().getId(),
                    secondResult.payment().getId(),
                    "Both requests should return the same payment"
            );

            assertTrue(
                    firstResult.created() ^ secondResult.created(),
                    "Exactly one request should report that it created the payment"
            );

            assertTrue(
                    paymentRepository.findByIdempotencyKey(idempotencyKey).isPresent(),
                    "The payment should exist in PostgreSQL"
            );

            assertEquals(
                    1L,
                    paymentRepository.countByIdempotencyKey(idempotencyKey),
                    "There should be exactly one payment for the idempotency key"
            );

        } finally {
            executor.shutdownNow();
        }
    }
}
