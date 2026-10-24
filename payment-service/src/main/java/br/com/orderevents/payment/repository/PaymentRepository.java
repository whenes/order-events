package br.com.orderevents.payment.repository;

import br.com.orderevents.payment.domain.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    Optional<Payment> findByIdempotencyKey(String idempotencyKey);
    long countByIdempotencyKey(String idempotencyKey);
}