package com.minibank.backend.user.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.minibank.backend.user.entity.User;

public interface UserRepository extends JpaRepository<User, Long> {
	Optional<User> findByPhone(String phone);

	boolean existsByPhone(String phone);

	boolean existsByEmailIgnoreCase(String email);
}
