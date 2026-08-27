package com.minibank.backend.admin.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.minibank.backend.admin.entity.Role;

public interface RoleRepository extends JpaRepository<Role, Long> {
	Optional<Role> findByCode(String code);
}
