package com.minibank.backend.support.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.minibank.backend.support.entity.LimitChangeRequest;

@Repository
public interface LimitChangeRequestRepository extends JpaRepository<LimitChangeRequest, Long> {
	Optional<LimitChangeRequest> findByServiceRequestId(Long serviceRequestId);
}
