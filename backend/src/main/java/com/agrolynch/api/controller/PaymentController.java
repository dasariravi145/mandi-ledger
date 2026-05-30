package com.agrolynch.api.controller;

import com.agrolynch.api.dto.PaymentRequest;
import com.agrolynch.api.model.BuyerPayment;
import com.agrolynch.api.service.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    @Autowired
    private PaymentService paymentService;

    @PostMapping("/save-with-ocr")
    public ResponseEntity<BuyerPayment> saveOcrPayment(@Valid @RequestBody PaymentRequest request) {
        BuyerPayment payment = paymentService.processOcrPayment(request);
        return ResponseEntity.ok(payment);
    }

    @GetMapping("/history/{buyerId}")
    public ResponseEntity<List<BuyerPayment>> getPaymentHistory(@PathVariable String buyerId) {
        return ResponseEntity.ok(paymentService.getBuyerPayments(buyerId));
    }
}
