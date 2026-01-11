package com.ws.backend.service;

import com.ws.backend.dto.OrderRequestDTO;
import com.ws.backend.dto.PaymentResponseDTO;
import com.ws.backend.dto.PaymentStatusDTO;
import com.ws.backend.dto.PspRequestDTO;
import com.ws.backend.model.*;
import com.ws.backend.repository.*;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSession;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

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

    @Value("${webshop.merchant.id}")
    private String merchantId;

    @Value("${webshop.merchant.password}")
    private String merchantPassword;

    @Value("${psp.api.url}")
    private String pspApiUrl;

    @Value("${webshop.frontend.url}")
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

            // Proveri da li postoji konfliktna porudžbina (samo CONFIRMED porudžbine)
            boolean isConflict = orderRepository.existsConflictingVehicleOrder(
                    OrderType.VEHICLE,
                    vehicle.getId(),
                    OrderStatus.CONFIRMED,
                    request.getStartDate(),
                    request.getEndDate());
            if (isConflict) {
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

        System.out.println("==============================================");
        System.out.println("DEBUG: SALJEM ZAHTEV PSP-u");
        System.out.println("Success URL: " + pspRequest.getSuccessUrl());
        System.out.println("Error URL:   " + pspRequest.getErrorUrl());
        System.out.println("==============================================");
        try {
            ResponseEntity<PaymentResponseDTO> response = restTemplate.postForEntity(
                    pspApiUrl, pspRequest, PaymentResponseDTO.class);

            // 5. SAČUVAJ psp ID TRANSAKCIJE (pspPaymentId)
            PaymentResponseDTO pspData = response.getBody();
            if (pspData != null) {
                tx.setPspPaymentId(pspData.getPaymentId());
                transactionRepository.save(tx);
            }

            return pspData; // Vraćaš URL i ID tvom kontroleru

        } catch (Exception e) {
            // Ako PSP ne odgovori, poništavamo narudžbinu (ili stavljamo u ERROR)
            order.setOrderStatus(OrderStatus.ERROR);
            orderRepository.save(order);
            throw new RuntimeException("PSP is not reachable: " + e.getMessage());
        }
    }

    @Transactional
    public void processCallback(PaymentStatusDTO statusDTO, OrderStatus targetStatus) {
        // 1. Pronalaženje transakcije
        PaymentTransaction tx = transactionRepository
                .findByMerchantOrderId(statusDTO.getMerchantOrderId())
                .orElseThrow(() -> new RuntimeException("Transakcija nije pronađena: " + statusDTO.getMerchantOrderId()));

        // 2. Provera da li je transakcija već završena (Idempotency check)
        if (tx.getStatus() == OrderStatus.CONFIRMED) {
            return; // Već je obrađeno, ne radimo ništa
        }

        // 3. Ažuriranje transakcije podacima iz PSP-a
        tx.setStatus(targetStatus);
        tx.setPaymentMethod(statusDTO.getPaymentMethod());
        transactionRepository.save(tx);

        // 4. Ažuriranje narudžbine
        Order order = tx.getOrder();
        order.setOrderStatus(targetStatus);

        orderRepository.save(order);
    }

    /**
     * Dobavljanje svih narudžbina korisnika sa detaljima transakcije
     * Razdvaja aktivne i prošle usluge
     */
    public java.util.List<com.ws.backend.dto.OrderHistoryDTO> getUserOrders(Long userId) {
        java.util.List<Order> orders = orderRepository.findByUserIdOrderByCreatedAtDesc(userId);
        LocalDateTime now = LocalDateTime.now();
        
        return orders.stream().map(order -> {
            com.ws.backend.dto.OrderHistoryDTO dto = new com.ws.backend.dto.OrderHistoryDTO();
            
            // Order osnovni podaci
            dto.setOrderId(order.getId());
            dto.setType(order.getType());
            dto.setOrderStatus(order.getOrderStatus());
            dto.setTotalAmount(order.getTotalAmount());
            dto.setCurrency(order.getCurrency());
            dto.setCreatedAt(order.getCreatedAt());
            dto.setCompletedAt(order.getCompletedAt());
            dto.setStartDate(order.getStartDate());
            dto.setEndDate(order.getEndDate());
            
            // Vehicle detalji
            if (order.getVehicle() != null) {
                dto.setVehicleId(order.getVehicle().getId());
                dto.setVehicleModel(order.getVehicle().getModel());
                dto.setVehicleImageUrl(order.getVehicle().getImageUrl());
                dto.setPricePerDay(order.getPricePerDay());
            }
            
            // Equipment detalji
            if (order.getEquipment() != null) {
                dto.setEquipmentId(order.getEquipment().getId());
                dto.setEquipmentType(order.getEquipment().getEquipmentType() != null ? 
                    order.getEquipment().getEquipmentType().toString() : null);
            }
            
            // Insurance detalji
            if (order.getInsurance() != null) {
                dto.setInsuranceId(order.getInsurance().getId());
                dto.setInsuranceType(order.getInsurance().getType() != null ? 
                    order.getInsurance().getType().toString() : null);
                dto.setInsurancePrice(order.getPrice());
            }
            
            // PaymentTransaction detalji - pronađi transakciju po order ID-u
            PaymentTransaction tx = transactionRepository.findByOrderId(order.getId())
                .orElse(null);
            
            if (tx != null) {
                dto.setMerchantOrderId(tx.getMerchantOrderId());
                dto.setPspPaymentId(tx.getPspPaymentId());
                dto.setPaymentMethod(tx.getPaymentMethod());
                dto.setPaymentStatus(tx.getStatus());
                dto.setPaymentCreatedAt(tx.getCreatedAt());
            }
            
            // Određivanje da li je usluga aktivna
            boolean isActive = false;
            if (order.getOrderStatus() == OrderStatus.CONFIRMED) {
                if (order.getType() == OrderType.VEHICLE || order.getType() == OrderType.EQUIPMENT) {
                    // Za vozilo i opremu: aktivno ako je trenutni datum između startDate i endDate
                    if (order.getStartDate() != null && order.getEndDate() != null) {
                        isActive = !now.isBefore(order.getStartDate()) && !now.isAfter(order.getEndDate());
                    }
                } else if (order.getType() == OrderType.INSURANCE) {
                    // Za osiguranje: sve CONFIRMED narudžbine su aktivne (trajno važe)
                    isActive = true;
                }
            }
            dto.setActive(isActive);
            
            return dto;
        }).collect(java.util.stream.Collectors.toList());
    }
}
