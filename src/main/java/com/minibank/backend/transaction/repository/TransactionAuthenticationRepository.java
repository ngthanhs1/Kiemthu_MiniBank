package com.minibank.backend.transaction.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.minibank.backend.transaction.entity.TransactionAuthentication;

public interface TransactionAuthenticationRepository extends JpaRepository<TransactionAuthentication, Long> {
	Optional<TransactionAuthentication> findByTransactionId(Long transactionId);
}
