package com.ws.backend.repository;

import com.ws.backend.model.Order;
import com.ws.backend.model.PaymentTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, Long> {
    /**
     * Pronalazi transakciju na osnovu merchantOrderId (npr. TX-1-1704...)
     * * @param merchantOrderId unikatni ID koji je Web Shop generisao
     * @return Optional koji sadrži transakciju ako postoji
     */
    Optional<PaymentTransaction> findByMerchantOrderId(String merchantOrderId);
    
    /**
     * Pronalazi transakciju na osnovu Order ID-a
     */
    Optional<PaymentTransaction> findByOrderId(Long orderId);
}
