package com.minibank.backend.account.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.minibank.backend.account.entity.AccountQrCode;

public interface AccountQrCodeRepository extends JpaRepository<AccountQrCode, Long> {
	Optional<AccountQrCode> findFirstByAccountIdAndActiveTrueOrderByCreatedAtDesc(Long accountId);
}
