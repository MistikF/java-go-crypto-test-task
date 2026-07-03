package com.example.cryptojava.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CryptoOperationRepository extends JpaRepository<CryptoOperation, Long> {
}
