package com.bank.repository;

import com.bank.model.Card;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CardRepository extends JpaRepository<Card, Long> {
    // Spring Data JPA će automatski napraviti upit:
    // "SELECT * FROM cards WHERE pan = ?"
    Optional<Card> findByPan(String pan);
}
