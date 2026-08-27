package com.minibank.backend.account.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.minibank.backend.account.entity.TransferQrIntent;

public interface TransferQrIntentRepository extends JpaRepository<TransferQrIntent, Long> {
	Optional<TransferQrIntent> findFirstByAccountIdOrderByCreatedAtDesc(Long accountId);
	Optional<TransferQrIntent> findByIdAndCreatedByUserId(Long id, Long createdByUserId);
	Optional<TransferQrIntent> findByIdAndClaimedByUserId(Long id, Long claimedByUserId);
	Optional<TransferQrIntent> findByIntentToken(String intentToken);
	Optional<TransferQrIntent> findByIdAndStatus(Long id, String status);
}