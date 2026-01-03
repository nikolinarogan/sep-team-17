package repository;

import model.MerchantSubscription;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MerchantSubscriptionRepository extends JpaRepository<MerchantSubscription, Long> {
    List<MerchantSubscription> findByMerchantMerchantId(String merchantId);

    Optional<MerchantSubscription> findByMerchantMerchantIdAndPaymentMethodName(String merchantId, String methodName);
}
