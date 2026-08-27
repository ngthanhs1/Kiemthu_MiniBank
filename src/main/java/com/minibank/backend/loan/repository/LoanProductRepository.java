package com.minibank.backend.loan.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.minibank.backend.loan.entity.LoanProduct;

@Repository
public interface LoanProductRepository extends JpaRepository<LoanProduct, Long> {
	List<LoanProduct> findByStatusOrderByBaseInterestRateDesc(String status);

	List<LoanProduct> findByStatusIgnoreCaseOrderByBaseInterestRateDesc(String status);

	List<LoanProduct> findAllByOrderByUpdatedAtDesc();

	boolean existsByCodeIgnoreCase(String code);
}
