package com.bank.repository;

import com.bank.model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    // Pronalazi transakciju po internom ID-ju plaćanja (koji generiše Banka)
    Optional<Transaction> findByPaymentId(String paymentId);

    // Opciono: Pronalazi transakciju po onom ID-ju koji je PSP poslao (STAN)
    Optional<Transaction> findByPspTransactionId(String pspTransactionId);
}
