package com.minibank.backend.admin.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.minibank.backend.admin.entity.PermissionDefinition;

public interface PermissionDefinitionRepository extends JpaRepository<PermissionDefinition, Long> {
	List<PermissionDefinition> findAllByOrderByTabGroupAscSortOrderAscLabelAsc();

	Optional<PermissionDefinition> findByCode(String code);
}