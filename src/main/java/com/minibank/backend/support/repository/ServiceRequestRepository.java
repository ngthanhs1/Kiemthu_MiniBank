package com.minibank.backend.support.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.minibank.backend.support.entity.ServiceRequest;

@Repository
public interface ServiceRequestRepository extends JpaRepository<ServiceRequest, Long> {
	List<ServiceRequest> findByUserId(long userId);
	Optional<ServiceRequest> findByIdAndUserId(long id, long userId);
	List<ServiceRequest> findByUserIdOrderBySubmittedAtDesc(long userId);

	@EntityGraph(attributePaths = {"user", "assignedTo"})
	List<ServiceRequest> findByRequestTypeOrderBySubmittedAtDesc(String requestType);

	@EntityGraph(attributePaths = {"user", "assignedTo"})
	List<ServiceRequest> findByRequestTypeAndUserIdOrderBySubmittedAtDesc(String requestType, long userId);

	@Query("select count(r) from ServiceRequest r where lower(r.status) = 'submitted'")
	long countByStatusSubmitted();
}
