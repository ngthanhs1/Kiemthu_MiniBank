package com.minibank.backend.saving.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.minibank.backend.saving.entity.SavingProduct;

@Repository
public interface SavingProductRepository extends JpaRepository<SavingProduct, Long> {
	List<SavingProduct> findByStatusOrderByBaseInterestRateDesc(String status);

	List<SavingProduct> findByStatusIgnoreCaseOrderByBaseInterestRateDesc(String status);

	List<SavingProduct> findAllByOrderByUpdatedAtDesc();
}
