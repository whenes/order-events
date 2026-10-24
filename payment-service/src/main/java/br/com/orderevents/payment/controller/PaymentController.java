package br.com.orderevents.payment.controller;

import br.com.orderevents.payment.controller.dto.CreatePaymentRequest;
import br.com.orderevents.payment.controller.dto.PaymentResponse;
import br.com.orderevents.payment.service.CreatePaymentResult;
import br.com.orderevents.payment.service.PaymentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/payments")
public class PaymentController {
    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping
    public ResponseEntity<PaymentResponse> createPayment(
            @Valid @RequestBody CreatePaymentRequest request,
            @RequestHeader("Idempotency-Key") String idempotencyKey) {

        CreatePaymentResult result = paymentService.createPayment(
                request.amount(),
                request.currency(),
                idempotencyKey
        );

        PaymentResponse response = PaymentResponse.from(result.payment());

        HttpStatus status = result.created()
                ? HttpStatus.CREATED
                : HttpStatus.OK;

        return ResponseEntity
                .status(status)
                .body(response);
    }
}
