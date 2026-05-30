package com.agrolynch.api.service;

import com.agrolynch.api.dto.PaymentRequest;
import com.agrolynch.api.model.Buyer;
import com.agrolynch.api.model.BuyerPayment;
import com.agrolynch.api.model.OcrScan;
import com.agrolynch.api.repository.BuyerPaymentRepository;
import com.agrolynch.api.repository.BuyerRepository;
import com.agrolynch.api.repository.OcrScanRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class PaymentService {

    @Autowired
    private BuyerRepository buyerRepository;

    @Autowired
    private BuyerPaymentRepository paymentRepository;

    @Autowired
    private OcrScanRepository ocrScanRepository;

    @Transactional
    public BuyerPayment processOcrPayment(PaymentRequest request) {
        Buyer buyer = buyerRepository.findById(request.getBuyerId())
            .orElseThrow(() -> new RuntimeException("Buyer not found"));

        // 1. Create Payment
        BuyerPayment payment = new BuyerPayment();
        payment.setId(UUID.randomUUID().toString());
        payment.setBuyer(buyer);
        payment.setAmount(request.getAmount());
        payment.setPaymentDate(request.getDate());
        payment.setReferenceNo(request.getReferenceNo());
        payment.setNotes(request.getNotes());
        
        // 2. Update Buyer Balance (Auto-calculation)
        buyer.setTotalPaid(buyer.getTotalPaid() + request.getAmount());
        buyer.setPendingAmount(buyer.getTotalPurchase() - buyer.getTotalPaid());
        
        // 3. Save OCR History
        OcrScan scan = new OcrScan();
        scan.setId(UUID.randomUUID().toString());
        scan.setBuyerId(buyer.getId());
        scan.setExtractedAmount(request.getAmount());
        scan.setScanStatus("CONFIRMED");
        scan.setCreatedAt(LocalDateTime.now());
        
        ocrScanRepository.save(scan);
        buyerRepository.save(buyer);
        return paymentRepository.save(payment);
    }

    public List<BuyerPayment> getBuyerPayments(String buyerId) {
        return paymentRepository.findByBuyerId(buyerId);
    }
}
