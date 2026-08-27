package com.minibank.backend.user.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.minibank.backend.user.entity.KycRequest;

public interface KycRequestRepository extends JpaRepository<KycRequest, Long> {
	List<KycRequest> findByStatusOrderBySubmittedAtAsc(String status);

	List<KycRequest> findByStatusOrderBySubmittedAtDesc(String status);

	List<KycRequest> findAllByOrderBySubmittedAtDesc();

	Optional<KycRequest> findByIdAndUserId(Long id, Long userId);

	boolean existsByUserIdAndStatus(Long userId, String status);

	boolean existsByUserIdAndStatusIn(Long userId, Collection<String> statuses);
}
