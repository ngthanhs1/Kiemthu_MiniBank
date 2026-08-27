package com.minibank.backend.transaction.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.minibank.backend.transaction.entity.TransactionCategory;

public interface TransactionCategoryRepository extends JpaRepository<TransactionCategory, Long> {
	List<TransactionCategory> findByTransactionIdIn(Collection<Long> transactionIds);

	List<TransactionCategory> findByTransactionId(Long transactionId);

	Optional<TransactionCategory> findFirstByTransactionIdOrderByTaggedAtDescIdDesc(Long transactionId);

	void deleteByTransactionId(Long transactionId);
}