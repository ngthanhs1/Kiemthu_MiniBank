package com.minibank.backend.loan.repository;

import java.util.List;
import java.util.Optional;
import java.math.BigDecimal;
import java.time.Instant;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.minibank.backend.loan.entity.Loan;

@Repository
public interface LoanRepository extends JpaRepository<Loan, Long> {
	List<Loan> findByUserId(long userId);
	Optional<Loan> findByCode(String code);
	Optional<Loan> findByIdAndUserId(long id, long userId);
	boolean existsByLoanApplicationId(Long loanApplicationId);

	// find loan by loan application id (used by ContractDataResolver)
	Optional<Loan> findByLoanApplicationId(Long loanApplicationId);

	@Query("select count(l) from Loan l where lower(l.status) = 'active'")
	long countByStatusActive();

	@Query("select coalesce(sum(l.outstandingPrincipal), 0) + coalesce(sum(l.outstandingInterest), 0) from Loan l where lower(l.status) = 'active'")
	BigDecimal sumOutstandingByStatusActive();

	@Query("select count(l) from Loan l where lower(l.status) = 'active' and l.nextDueDate is not null and l.nextDueDate <= :threshold")
	long countDueSoon(@Param("threshold") Instant threshold);
}
