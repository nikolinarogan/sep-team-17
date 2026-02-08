package com.ws.backend.controller;

import com.ws.backend.dto.PaymentStatusDTO;
import com.ws.backend.model.OrderStatus;
import com.ws.backend.model.PaymentTransaction;
import com.ws.backend.service.OrderService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/payment/callback")
public class PaymentCallbackController {

    private final OrderService orderService;

    public PaymentCallbackController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping("/success")
    public ResponseEntity<Void> handleSuccess(@RequestBody PaymentStatusDTO status) {
        orderService.processCallback(status, OrderStatus.CONFIRMED);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/failed")
    public ResponseEntity<Void> handleFailed(@RequestBody PaymentStatusDTO status) {
        orderService.processCallback(status, OrderStatus.CANCELLED);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/error")
    public ResponseEntity<Void> handleError(@RequestBody PaymentStatusDTO status) {
        orderService.processCallback(status, OrderStatus.ERROR);
        return ResponseEntity.ok().build();
    }
}
