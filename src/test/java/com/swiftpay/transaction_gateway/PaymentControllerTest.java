package com.swiftpay.transaction_gateway;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.swiftpay.transaction_gateway.controller.PaymentController;
import com.swiftpay.transaction_gateway.service.PaymentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(PaymentController.class)
class PaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PaymentService paymentService;

    @Test
    void getTransactionById_returnsNotFoundForUnknownTransaction() throws Exception {
        when(paymentService.getTransactionById("missing-id"))
                .thenThrow(new RuntimeException("Transaction not found"));

        mockMvc.perform(get("/v1/payments/missing-id"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Transaction not found"));
    }
}
