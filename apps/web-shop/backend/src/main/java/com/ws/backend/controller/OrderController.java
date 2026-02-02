package com.ws.backend.controller;

import com.ws.backend.dto.OrderHistoryDTO;
import com.ws.backend.dto.OrderRequestDTO;
import com.ws.backend.dto.PaymentResponseDTO;
import com.ws.backend.jwt.JwtService;
import com.ws.backend.model.Order;
import com.ws.backend.model.PaymentTransaction;
import com.ws.backend.repository.PaymentTransactionRepository;
import com.ws.backend.service.OrderService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/orders")
public class OrderController {

    private final OrderService orderService;
    private final JwtService jwtService;
    private final PaymentTransactionRepository transactionRepository;

    public OrderController(OrderService orderService, JwtService jwtService, PaymentTransactionRepository transactionRepository) {
        this.orderService = orderService;
        this.jwtService = jwtService;
        this.transactionRepository = transactionRepository;
    }

    @PostMapping
    public ResponseEntity<?> createOrder(
            @RequestBody OrderRequestDTO request,
            HttpServletRequest httpRequest
    ) {
        try {
            String token = JwtService.extractTokenFromRequest(httpRequest);
            if (token == null || !jwtService.validateToken(token)) {
                return ResponseEntity.badRequest().body("Invalid or missing authentication token");
            }

            Long userId = jwtService.getUserIdFromToken(token);
            if (userId == null) {
                return ResponseEntity.badRequest().body("User ID not found in token");
            }

            PaymentResponseDTO responseDTO = orderService.createOrder(request, userId);
            return ResponseEntity.ok(responseDTO);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body("Failed to create order: " + e.getMessage());
        }
    }

    @GetMapping
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> getUserOrders(HttpServletRequest httpRequest) {
        try {
            String token = JwtService.extractTokenFromRequest(httpRequest);
            if (token == null || !jwtService.validateToken(token)) {
                return ResponseEntity.badRequest().body("Invalid or missing authentication token");
            }

            Long userId = jwtService.getUserIdFromToken(token);
            if (userId == null) {
                return ResponseEntity.badRequest().body("User ID not found in token");
            }

            List<OrderHistoryDTO> orders = orderService.getUserOrders(userId);
            return ResponseEntity.ok(orders);

        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed to get orders: " + e.getMessage());
        }
    }

    @GetMapping("/status")
    public ResponseEntity<?> getOrderStatus(@RequestParam String merchantOrderId) {
        try {
            PaymentTransaction tx = transactionRepository
                    .findByMerchantOrderId(merchantOrderId)
                    .orElseThrow(() -> new RuntimeException("Narudžbina nije pronađena"));

            Order order = tx.getOrder();

            return ResponseEntity.ok(Map.of(
                    "orderStatus", order.getOrderStatus().toString(),
                    "merchantOrderId", merchantOrderId
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        }
    }
}
