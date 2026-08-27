package com.minibank.backend.loan.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.minibank.backend.loan.entity.LoanProductInterestTier;

@Repository
public interface LoanProductInterestTierRepository extends JpaRepository<LoanProductInterestTier, Long> {
    @Query("select t from LoanProductInterestTier t join fetch t.loanProduct p " +
        "where p.id = :loanProductId order by t.effectiveFrom desc, t.minAmount asc")
    List<LoanProductInterestTier> findByLoanProductIdOrderByEffectiveFromDescMinAmountAsc(
        @Param("loanProductId") Long loanProductId
    );
}
