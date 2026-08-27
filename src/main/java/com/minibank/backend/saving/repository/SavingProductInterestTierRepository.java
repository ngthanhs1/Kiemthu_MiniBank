package com.minibank.backend.saving.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.minibank.backend.saving.entity.SavingProductInterestTier;

@Repository
public interface SavingProductInterestTierRepository extends JpaRepository<SavingProductInterestTier, Long> {
    @Query("select t from SavingProductInterestTier t join fetch t.savingProduct p " +
        "where p.id = :savingProductId order by t.effectiveFrom desc, t.minAmount asc")
    List<SavingProductInterestTier> findBySavingProductIdOrderByEffectiveFromDescMinAmountAsc(
        @Param("savingProductId") Long savingProductId
    );
}
