package br.com.orderevents.payment.controller;

import br.com.orderevents.payment.domain.Payment;
import br.com.orderevents.payment.domain.PaymentStatus;
import br.com.orderevents.payment.service.IdempotencyConflictException;
import br.com.orderevents.payment.service.PaymentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import java.util.UUID;
import br.com.orderevents.payment.service.CreatePaymentResult;

import org.springframework.test.util.ReflectionTestUtils;

@WebMvcTest(PaymentController.class)
class PaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PaymentService paymentService;

    @Test
    void shouldCreatePaymentWhenRequestIsValid() throws Exception {
        Payment payment = new Payment(15000L, "BRL", "key-123");

        UUID paymentId = UUID.randomUUID();
        ReflectionTestUtils.setField(payment, "id", paymentId);

        when(paymentService.createPayment(15000L, "BRL", "key-123"))
                .thenReturn(new CreatePaymentResult(payment, true));

        mockMvc.perform(post("/payments")
                        .header("Idempotency-Key", "key-123")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "amount": 15000,
                                    "currency": "BRL"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(paymentId.toString()))
                .andExpect(jsonPath("$.amount").value(15000))
                .andExpect(jsonPath("$.currency").value("BRL"))
                .andExpect(jsonPath("$.status").value(PaymentStatus.PENDING.name()))
                .andExpect(jsonPath("$.createdAt").exists());

        verify(paymentService).createPayment(15000L, "BRL", "key-123");
    }

    @Test
    void shouldReturnBadRequestWhenAmountIsZero() throws Exception {
        mockMvc.perform(post("/payments")
                        .header("Idempotency-Key", "key-123")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "amount": 0,
                                    "currency": "BRL"
                                }
                                """))
                .andExpect(status().isBadRequest());

        verify(paymentService, never()).createPayment(any(), any(), any());
    }

    @Test
    void shouldReturnBadRequestWhenIdempotencyKeyIsMissing() throws Exception {
        mockMvc.perform(post("/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "amount": 15000,
                                    "currency": "BRL"
                                }
                                """))
                .andExpect(status().isBadRequest());

        verify(paymentService, never()).createPayment(any(), any(), any());
    }

    @Test
    void shouldReturnConflictWhenIdempotencyKeyIsReusedWithDifferentData()
            throws Exception {

        when(paymentService.createPayment(25000L, "BRL", "key-123"))
                .thenThrow(new IdempotencyConflictException(
                        "Idempotency key already used with different payment data"
                ));

        mockMvc.perform(post("/payments")
                        .header("Idempotency-Key", "key-123")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "amount": 25000,
                                "currency": "BRL"
                            }
                            """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Idempotency conflict"))
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.detail").value(
                        "Idempotency key already used with different payment data"
                ));

        verify(paymentService).createPayment(25000L, "BRL", "key-123");
    }

    @Test
    void shouldReturnOkWhenPaymentAlreadyExists() throws Exception {
        Payment payment = new Payment(15000L, "BRL", "key-123");

        when(paymentService.createPayment(15000L, "BRL", "key-123"))
                .thenReturn(new CreatePaymentResult(payment, false));

        mockMvc.perform(post("/payments")
                        .header("Idempotency-Key", "key-123")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                        {
                            "amount": 15000,
                            "currency": "BRL"
                        }
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.amount").value(15000))
                .andExpect(jsonPath("$.currency").value("BRL"));

        verify(paymentService).createPayment(15000L, "BRL", "key-123");
    }
}