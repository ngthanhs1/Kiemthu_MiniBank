package com.minibank.backend.approval.repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.minibank.backend.approval.entity.ApprovalPolicy;

public interface ApprovalPolicyRepository extends JpaRepository<ApprovalPolicy, Long> {
	List<ApprovalPolicy> findAllByOrderByServiceTypeAscMinAmountAsc();

	@Query("""
		select p
		from ApprovalPolicy p
		where p.active = true
		  and lower(p.serviceType) = lower(:serviceType)
		  and p.minAmount <= :amount
		  and (p.maxAmount is null or p.maxAmount >= :amount)
		order by p.minAmount desc
	""")
	List<ApprovalPolicy> findMatchingPolicies(
		@Param("serviceType") String serviceType,
		@Param("amount") BigDecimal amount
	);

	default Optional<ApprovalPolicy> findBestMatch(String serviceType, BigDecimal amount) {
		return findMatchingPolicies(serviceType, amount).stream().findFirst();
	}
}
