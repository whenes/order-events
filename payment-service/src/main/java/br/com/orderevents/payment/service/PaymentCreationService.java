package br.com.orderevents.payment.service;

import br.com.orderevents.payment.domain.Payment;
import br.com.orderevents.payment.repository.PaymentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;

@Service
public class PaymentCreationService {
    private final PaymentRepository paymentRepository;

    public PaymentCreationService(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Payment create(Long amount, String currency, String idempotencyKey) {
        Payment payment = new Payment(amount, currency, idempotencyKey);
        return paymentRepository.saveAndFlush(payment);
    }
}
