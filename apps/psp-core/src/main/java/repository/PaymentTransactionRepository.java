package repository;

import model.PaymentTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, Long> {
    Optional<PaymentTransaction> findByUuid(String uuid);

    boolean existsByMerchantIdAndMerchantOrderId(String merchantId, String merchantOrderId);
    Optional<PaymentTransaction> findByExecutionId(String executionId);
    Optional<PaymentTransaction> findByMerchantIdAndMerchantOrderId(String merchantId, String merchantOrderId);
}
