package com.minibank.backend.saving.repository;

import java.util.List;
import java.util.Optional;
import java.math.BigDecimal;
import java.time.Instant;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.minibank.backend.saving.entity.Saving;

@Repository
public interface SavingRepository extends JpaRepository<Saving, Long> {
	List<Saving> findByUserId(long userId);

	@EntityGraph(attributePaths = {"user", "savingProduct", "sourceAccount", "settlementAccount"})
	List<Saving> findByUserIdOrderByCreatedAtDesc(long userId);

	@EntityGraph(attributePaths = {"user", "savingProduct", "sourceAccount", "settlementAccount"})
	List<Saving> findByStatusOrderByCreatedAtDesc(String status);

	Optional<Saving> findByCode(String code);
	Optional<Saving> findByIdAndUserId(long id, long userId);

	@EntityGraph(attributePaths = {"user", "savingProduct", "sourceAccount", "settlementAccount"})
	Optional<Saving> findWithDetailsByIdAndUserId(Long id, Long userId);

	@EntityGraph(attributePaths = {"user", "savingProduct", "sourceAccount", "settlementAccount"})
	List<Saving> findAllByOrderByCreatedAtDesc();

	@EntityGraph(attributePaths = {"user", "savingProduct", "sourceAccount", "settlementAccount"})
	Optional<Saving> findWithDetailsById(Long id);

	@Query("select count(s) from Saving s where lower(s.status) = 'active'")
	long countByStatusActive();

	@Query("select coalesce(sum(s.principalAmount), 0) from Saving s where lower(s.status) = 'active'")
	BigDecimal sumPrincipalByStatusActive();

	@Query("select count(s) from Saving s where lower(s.status) = 'active' and s.maturityDate is not null and s.maturityDate <= :threshold")
	long countDueSoon(@Param("threshold") Instant threshold);
}
