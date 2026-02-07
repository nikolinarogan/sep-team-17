package com.example.psp_paypal.controller;

import com.example.psp_paypal.dto.MicroservicePaymentRequest;
import com.example.psp_paypal.dto.MicroservicePaymentResponse;
import com.example.psp_paypal.service.PayPalLogicService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/connector")
public class PayPalConnectorController {
    private final PayPalLogicService logicService;

    public PayPalConnectorController(PayPalLogicService logicService) {
        this.logicService = logicService;
    }

    @PostMapping("/init")
    public ResponseEntity<MicroservicePaymentResponse> init(@RequestBody MicroservicePaymentRequest req) {
        return ResponseEntity.ok(logicService.createOrder(req));
    }

    @PostMapping("/capture/{orderId}")
    public ResponseEntity<Boolean> capture(@PathVariable String orderId) {
        return ResponseEntity.ok(logicService.captureOrder(orderId));
    }
}
