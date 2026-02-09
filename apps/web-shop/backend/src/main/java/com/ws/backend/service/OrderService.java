package com.ws.backend.service;

import com.ws.backend.dto.*;
import com.ws.backend.model.*;
import com.ws.backend.repository.*;
import com.ws.backend.tools.AuditLogger;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSession;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class OrderService {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private VehicleRepository vehicleRepository;

    @Autowired
    private InsuranceRepository insuranceRepository;

    @Autowired
    private EquipmentRepository equipmentRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PaymentTransactionRepository transactionRepository;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private AuditLogger auditLogger;

    @Value("${webshop.merchant.id}")
    private String merchantId;

    @Value("${webshop.merchant.password}")
    private String merchantPassword;

    @Value("https://localhost:8000/api/payments/init")
    private String pspApiUrl;

    @Value("${psp.api.url}")
    private String apiBase;

    static {
        HttpsURLConnection.setDefaultHostnameVerifier(new HostnameVerifier() {
            public boolean verify(String hostname, SSLSession session) {
                return true; // Dozvoli sve nazive (samo za dev!)
            }
        });
    }

    @Transactional
    public PaymentResponseDTO createOrder(OrderRequestDTO request, Long userId) {
        // Audit log start
        auditLogger.logEvent("ORDER_CREATION_START", "PENDING", "User: " + userId);

        AppUser user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));

        Order order = new Order();
        order.setUser(user);
        order.setCurrency(request.getCurrency() != null ? request.getCurrency() : "EUR");
        order.setOrderStatus(OrderStatus.PENDING);
        order.setCreatedAt(LocalDateTime.now());

        if (request.getVehicleId() != null) {
            if (!request.hasDates()) {
                throw new IllegalArgumentException("Start date and end date are required for vehicle orders");
            }
            if (!request.isDateRangeValid()) {
                throw new IllegalArgumentException("End date must be after start date");
            }
            if (request.getStartDate().isBefore(LocalDateTime.now())) {
                throw new IllegalArgumentException("Start date cannot be in the past");
            }

            Vehicle vehicle = vehicleRepository.findById(request.getVehicleId())
                    .orElseThrow(() -> new RuntimeException("Vehicle not found with id: " + request.getVehicleId()));

            if (vehicle.getAvailable() == null || !vehicle.getAvailable()) {
                throw new IllegalStateException("Vehicle is not available");
            }

            boolean isConflict = orderRepository.existsConflictingVehicleOrder(
                    OrderType.VEHICLE,
                    vehicle.getId(),
                    OrderStatus.CONFIRMED,
                    request.getStartDate(),
                    request.getEndDate());
            if (isConflict) {
                auditLogger.logEvent("ORDER_REJECTED_CONFLICT", "FAILED", "Vehicle: " + vehicle.getId());
                throw new IllegalStateException("Vehicle is already booked for this period");
            }

            order.setType(OrderType.VEHICLE);
            order.setVehicle(vehicle);
            order.setStartDate(request.getStartDate());
            order.setEndDate(request.getEndDate());
            order.setPricePerDay(vehicle.getPricePerDay());
            long days = ChronoUnit.DAYS.between(request.getStartDate(), request.getEndDate());
            order.setTotalAmount(vehicle.getPricePerDay() * days);

        } else if (request.getInsuranceId() != null) {
            Insurance insurance = insuranceRepository.findById(request.getInsuranceId())
                    .orElseThrow(() -> new RuntimeException("Insurance not found with id: " + request.getInsuranceId()));

            if (insurance.getIsAvailable() == null || !insurance.getIsAvailable()) {
                throw new IllegalStateException("Insurance is not available");
            }

            order.setType(OrderType.INSURANCE);
            order.setInsurance(insurance);
            order.setPrice(insurance.getPrice());
            order.setTotalAmount(insurance.getPrice());

        } else if (request.getEquipmentId() != null) {
            if (!request.hasDates()) {
                throw new IllegalArgumentException("Start date and end date are required for equipment orders");
            }
            if (!request.isDateRangeValid()) {
                throw new IllegalArgumentException("End date must be after start date");
            }
            if (request.getStartDate().isBefore(LocalDateTime.now())) {
                throw new IllegalArgumentException("Start date cannot be in the past");
            }

            Equipment equipment = equipmentRepository.findById(request.getEquipmentId())
                    .orElseThrow(() -> new RuntimeException("Equipment not found with id: " + request.getEquipmentId()));

            if (equipment.getAvailable() == null || !equipment.getAvailable()) {
                throw new IllegalStateException("Equipment is not available");
            }

            order.setType(OrderType.EQUIPMENT);
            order.setEquipment(equipment);
            order.setStartDate(request.getStartDate());
            order.setEndDate(request.getEndDate());
            order.setPricePerDay(equipment.getPricePerDay());
            long days = ChronoUnit.DAYS.between(request.getStartDate(), request.getEndDate());
            order.setTotalAmount(equipment.getPricePerDay() * days);
        }

        order = orderRepository.save(order);

        PaymentTransaction tx = new PaymentTransaction();
        tx.setOrder(order);
        tx.setMerchantOrderId( "TX-" + order.getId() + "-" + System.currentTimeMillis());
        tx.setStatus(OrderStatus.PENDING);
        tx.setCreatedAt(LocalDateTime.now());
        transactionRepository.save(tx);

        PspRequestDTO pspRequest = new PspRequestDTO();
        pspRequest.setMerchantId(merchantId);
        pspRequest.setMerchantPassword(merchantPassword);
        pspRequest.setAmount(BigDecimal.valueOf(order.getTotalAmount()));
        pspRequest.setCurrency(order.getCurrency());
        pspRequest.setMerchantOrderId(tx.getMerchantOrderId());
        pspRequest.setMerchantTimestamp(tx.getCreatedAt());

        pspRequest.setSuccessUrl(apiBase + "/payment-success?orderId=" + tx.getMerchantOrderId());
        pspRequest.setFailedUrl(apiBase + "/payment-failed?orderId=" + tx.getMerchantOrderId());
        pspRequest.setErrorUrl(apiBase + "/payment-error?orderId=" + tx.getMerchantOrderId());

        try {
            auditLogger.logEvent("PSP_INIT_COMMUNICATION", "START", "OrderID: " + tx.getMerchantOrderId());

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<PspRequestDTO> entity = new HttpEntity<>(pspRequest, headers);

            ResponseEntity<PaymentResponseDTO> response = restTemplate.postForEntity(
                    pspApiUrl, entity, PaymentResponseDTO.class);

            PaymentResponseDTO pspData = response.getBody();
            if (pspData != null) {
                tx.setPspPaymentId(pspData.getPaymentId());
                transactionRepository.save(tx);
                auditLogger.logEvent("PSP_INIT_SUCCESS", "SUCCESS", "PSP_ID: " + pspData.getPaymentId());
            }

            return pspData;

        } catch (Exception e) {
            order.setOrderStatus(OrderStatus.ERROR);
            orderRepository.save(order);
            auditLogger.logSecurityAlert("PSP_COMMUNICATION_FAIL", "Order: " + order.getId() + " | Error: " + e.getMessage());
            throw new RuntimeException("PSP is not reachable: " + e.getMessage());
        }
    }

    @Transactional
    public void processCallback(PaymentStatusDTO statusDTO, OrderStatus targetStatus) {
        auditLogger.logEvent("PSP_CALLBACK_RECEIVED", targetStatus.toString(), "MerchantOrder: " + statusDTO.getMerchantOrderId());

        PaymentTransaction tx = transactionRepository
                .findByMerchantOrderId(statusDTO.getMerchantOrderId())
                .orElseThrow(() -> new RuntimeException("Transakcija nije pronađena: " + statusDTO.getMerchantOrderId()));

        if (tx.getStatus() != OrderStatus.PENDING) {
            return;
        }

        tx.setStatus(targetStatus);
        tx.setPaymentMethod(statusDTO.getPaymentMethod());
        transactionRepository.save(tx);

        Order order = tx.getOrder();
        order.setOrderStatus(targetStatus);
        orderRepository.save(order);

        auditLogger.logEvent("ORDER_STATUS_UPDATED", targetStatus.toString(), "Order: " + order.getId());
    }

    public List<OrderHistoryDTO> getUserOrders(Long userId) {
        List<Order> orders = orderRepository.findByUserIdOrderByCreatedAtDesc(userId);
        LocalDateTime now = LocalDateTime.now();

        return orders.stream().map(order -> {
            OrderHistoryDTO dto = new OrderHistoryDTO();

            dto.setOrderId(order.getId());
            dto.setType(order.getType());
            dto.setOrderStatus(order.getOrderStatus());
            dto.setTotalAmount(order.getTotalAmount());
            dto.setCurrency(order.getCurrency());
            dto.setCreatedAt(order.getCreatedAt());
            dto.setCompletedAt(order.getCompletedAt());
            dto.setStartDate(order.getStartDate());
            dto.setEndDate(order.getEndDate());

            if (order.getVehicle() != null) {
                dto.setVehicleId(order.getVehicle().getId());
                dto.setVehicleModel(order.getVehicle().getModel());
                dto.setVehicleImageUrl(order.getVehicle().getImageUrl());
                dto.setPricePerDay(order.getPricePerDay());
            }

            if (order.getEquipment() != null) {
                dto.setEquipmentId(order.getEquipment().getId());
                dto.setEquipmentType(order.getEquipment().getEquipmentType() != null ?
                        order.getEquipment().getEquipmentType().toString() : null);
            }

            if (order.getInsurance() != null) {
                dto.setInsuranceId(order.getInsurance().getId());
                dto.setInsuranceType(order.getInsurance().getType() != null ?
                        order.getInsurance().getType().toString() : null);
                dto.setInsurancePrice(order.getPrice());
            }

            PaymentTransaction tx = transactionRepository.findByOrderId(order.getId())
                    .orElse(null);

            if (tx != null) {
                dto.setMerchantOrderId(tx.getMerchantOrderId());
                dto.setPspPaymentId(tx.getPspPaymentId());
                dto.setPaymentMethod(tx.getPaymentMethod());
                dto.setPaymentStatus(tx.getStatus());
                dto.setPaymentCreatedAt(tx.getCreatedAt());
            }

            boolean isActive = false;
            if (order.getOrderStatus() == OrderStatus.CONFIRMED) {
                if (order.getType() == OrderType.VEHICLE || order.getType() == OrderType.EQUIPMENT) {
                    if (order.getStartDate() != null && order.getEndDate() != null) {
                        isActive = !now.isBefore(order.getStartDate()) && !now.isAfter(order.getEndDate());
                    }
                } else if (order.getType() == OrderType.INSURANCE) {
                    isActive = true;
                }
            }
            dto.setActive(isActive);

            return dto;
        }).collect(java.util.stream.Collectors.toList());
    }

    @Scheduled(fixedRate = 600000)
    public void reconcilePendingOrders() {
        auditLogger.logEvent("SCHEDULER_RECONCILE", "START", "Checking stuck orders...");

        LocalDateTime fiveMinutesAgo = LocalDateTime.now().minusMinutes(5);
        List<Order> stuckOrders = orderRepository.findByOrderStatusAndCreatedAtBefore(OrderStatus.PENDING, fiveMinutesAgo);

        for (Order order : stuckOrders) {
            try {
                PaymentTransaction tx = transactionRepository.findByOrderId(order.getId()).orElse(null);
                if (tx == null) continue;

                String cleanBaseUrl = pspApiUrl.replace("/init", "");
                String checkUrl = cleanBaseUrl + "/status/" + merchantId + "/" + tx.getMerchantOrderId();

                try {
                    ResponseEntity<PaymentStatusDTO> response = restTemplate.getForEntity(checkUrl, PaymentStatusDTO.class);
                    PaymentStatusDTO pspData = response.getBody();

                    if (pspData != null) {
                        String pspStatus = pspData.getStatus();

                        if ("SUCCESS".equals(pspStatus)) {
                            processCallback(pspData, OrderStatus.CONFIRMED);
                        }
                        else if ("FAILED".equals(pspStatus) || "ERROR".equals(pspStatus)) {
                            processCallback(pspData, OrderStatus.CANCELLED);
                        }
                        else {
                            long minutesSinceCreation = ChronoUnit.MINUTES.between(order.getCreatedAt(), LocalDateTime.now());

                            if (minutesSinceCreation > 30) {
                                auditLogger.logEvent("ORDER_TIMEOUT", "CANCELLED", "Order: " + order.getId());
                                notifyPspOfCancellation(tx.getMerchantOrderId());
                                order.setOrderStatus(OrderStatus.CANCELLED);
                                orderRepository.save(order);
                                tx.setStatus(OrderStatus.CANCELLED);
                                transactionRepository.save(tx);
                            }
                        }
                    }
                } catch (Exception e) {
                    auditLogger.logEvent("RECONCILE_COMMUNICATION_ERROR", "ERROR", "MerchantOrder: " + tx.getMerchantOrderId());
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void notifyPspOfCancellation(String merchantOrderId) {
        try {
            String cancelUrl = pspApiUrl.replace("/init", "") + "/cancel";
            Map<String, String> requestBody = new HashMap<>();
            requestBody.put("merchantId", merchantId);
            requestBody.put("merchantPassword", merchantPassword);
            requestBody.put("merchantOrderId", merchantOrderId);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, String>> entity = new HttpEntity<>(requestBody, headers);
            restTemplate.postForEntity(cancelUrl, entity, Void.class);

            auditLogger.logEvent("PSP_CANCEL_NOTIFICATION", "SUCCESS", "MerchantOrder: " + merchantOrderId);
        } catch (Exception e) {
            auditLogger.logEvent("PSP_CANCEL_NOTIFICATION_ERROR", "FAILED", e.getMessage());
        }
    }
}