package com.minibank.backend.account.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.minibank.backend.account.entity.Account;

import jakarta.persistence.LockModeType;

public interface AccountRepository extends JpaRepository<Account, Long> {
	Optional<Account> findByAccountNumber(String accountNumber);

	boolean existsByAccountNumber(String accountNumber);

	List<Account> findByUserIdOrderByIdAsc(Long userId);

	// Convenience: first account for a user (used by ContractDataResolver)
	Optional<Account> findFirstByUserId(Long userId);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select a from Account a where a.accountNumber = :accountNumber")
	Optional<Account> findByAccountNumberForUpdate(@Param("accountNumber") String accountNumber);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select a from Account a where a.id = :id")
	Optional<Account> findByIdForUpdate(@Param("id") Long id);

	@Query("select count(a) from Account a where lower(a.status) = 'active'")
	long countByStatusActive();
}
