package br.com.orderevents.payment.service;

import br.com.orderevents.payment.domain.Payment;
import br.com.orderevents.payment.repository.PaymentRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

@Service
public class PaymentService {
    private final PaymentRepository paymentRepository;
    private final PaymentCreationService paymentCreationService;

    public PaymentService(
            PaymentRepository paymentRepository,
            PaymentCreationService paymentCreationService) {
        this.paymentRepository = paymentRepository;
        this.paymentCreationService = paymentCreationService;
    }

    public CreatePaymentResult createPayment(
            Long amount,
            String currency,
            String idempotencyKey) {

        return paymentRepository.findByIdempotencyKey(idempotencyKey)
                .map(existingPayment -> resolveExistingPayment(
                        existingPayment, amount, currency
                ))
                .orElseGet(() -> createOrRecover(
                        amount, currency, idempotencyKey
                ));
    }

    private CreatePaymentResult createOrRecover(
            Long amount,
            String currency,
            String idempotencyKey) {

        try {
            Payment payment = paymentCreationService.create(
                    amount, currency, idempotencyKey
            );

            return new CreatePaymentResult(payment, true);

        } catch (DataIntegrityViolationException exception) {
            Payment existingPayment = paymentRepository
                    .findByIdempotencyKey(idempotencyKey)
                    .orElseThrow(() -> exception);

            return resolveExistingPayment(existingPayment, amount, currency);
        }
    }

    private CreatePaymentResult resolveExistingPayment(
            Payment existingPayment,
            Long amount,
            String currency) {

        if (!existingPayment.getAmount().equals(amount)
                || !existingPayment.getCurrency().equals(currency)) {
            throw new IdempotencyConflictException(
                    "Idempotency key already used with different payment data"
            );
        }

        return new CreatePaymentResult(existingPayment, false);
    }
}
