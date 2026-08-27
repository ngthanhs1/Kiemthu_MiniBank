package com.minibank.backend.admin.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.minibank.backend.admin.entity.AdminUser;

public interface AdminUserRepository extends JpaRepository<AdminUser, Long> {
	Optional<AdminUser> findByUsernameIgnoreCase(String username);

	Optional<AdminUser> findFirstByStatusIgnoreCaseOrderByIdAsc(String status);

	Optional<AdminUser> findFirstByOrderByIdAsc();

	boolean existsByUsernameIgnoreCase(String username);

	boolean existsByEmailIgnoreCase(String email);
}
