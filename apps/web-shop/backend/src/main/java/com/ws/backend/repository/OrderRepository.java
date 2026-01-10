package com.ws.backend.repository;

import com.ws.backend.model.Order;
import com.ws.backend.model.OrderStatus;
import com.ws.backend.model.OrderType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    
    /**
     * Proverava da li postoji konfliktna porudžbina za vozilo u datom periodu.
     * Konflikt postoji ako postoji CONFIRMED porudžbina koja se preklapa sa datim periodom.
     * Dva perioda se preklapaju ako: (start1 < end2) AND (start2 < end1)
     */
    @Query("SELECT COUNT(o) > 0 FROM Order o WHERE " +
           "o.type = :orderType AND " +
           "o.vehicle.id = :vehicleId AND " +
           "o.orderStatus = :status AND " +
           "o.startDate < :endDate AND o.endDate > :startDate")
    boolean existsConflictingVehicleOrder(
            @Param("orderType") OrderType orderType,
            @Param("vehicleId") Long vehicleId,
            @Param("status") OrderStatus status,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );
    
    /**
     * Pronalazi sve narudžbine za određenog korisnika, sortirane po datumu kreiranja (najnovije prvo)
     */
    @Query("SELECT o FROM Order o WHERE o.user.id = :userId ORDER BY o.createdAt DESC")
    java.util.List<Order> findByUserIdOrderByCreatedAtDesc(@Param("userId") Long userId);
}
