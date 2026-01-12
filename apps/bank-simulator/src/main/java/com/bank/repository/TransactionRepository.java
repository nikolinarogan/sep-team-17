package com.bank.repository;

import com.bank.model.Merchant;
import com.bank.model.Transaction;
import com.bank.model.TransactionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    // Pronalazi transakciju po internom ID-ju plaćanja (koji generiše Banka)
    Optional<Transaction> findByPaymentId(String paymentId);

    // Opciono: Pronalazi transakciju po onom ID-ju koji je PSP poslao (STAN)
    Optional<Transaction> findByPspTransactionId(String pspTransactionId);
    Optional<Transaction> findTopByMerchantAndAmountAndStatusOrderByTimestampDesc(
            Merchant merchant,
            BigDecimal amount,
            TransactionStatus status
    );
}
