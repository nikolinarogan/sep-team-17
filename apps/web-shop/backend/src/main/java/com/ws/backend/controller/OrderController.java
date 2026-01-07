package com.ws.backend.controller;

import com.ws.backend.dto.OrderRequestDTO;
import com.ws.backend.jwt.JwtService;
import com.ws.backend.model.Order;
import com.ws.backend.service.OrderService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/orders")
public class OrderController {

    private final OrderService orderService;
    private final JwtService jwtService;

    public OrderController(OrderService orderService, JwtService jwtService) {
        this.orderService = orderService;
        this.jwtService = jwtService;
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

            Order order = orderService.createOrder(request, userId);
            return ResponseEntity.ok(order);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body("Failed to create order: " + e.getMessage());
        }
    }
}
