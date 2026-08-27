package com.minibank.backend.system.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.minibank.backend.system.entity.SystemLog;

public interface SystemLogRepository extends JpaRepository<SystemLog, Long> {

	@Query(
		"select s from SystemLog s " +
		"where (:actorType is null or s.actorType = :actorType) " +
		"and (:actorId is null or s.actorId = :actorId) " +
		"and (:action is null or s.action = :action) " +
		"and (:targetType is null or s.targetType = :targetType) " +
		"and (:targetId is null or s.targetId = :targetId) " +
		"and (:fromTime is null or s.createdAt >= :fromTime) " +
		"and (:toTime is null or s.createdAt <= :toTime) " +
		"order by s.createdAt desc"
	)
	Page<SystemLog> search(
		@Param("actorType") String actorType,
		@Param("actorId") Long actorId,
		@Param("action") String action,
		@Param("targetType") String targetType,
		@Param("targetId") Long targetId,
		@Param("fromTime") java.time.Instant fromTime,
		@Param("toTime") java.time.Instant toTime,
		Pageable pageable
	);
}
