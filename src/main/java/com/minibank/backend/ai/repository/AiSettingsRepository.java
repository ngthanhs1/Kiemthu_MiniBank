package com.minibank.backend.ai.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.minibank.backend.ai.entity.AiSettings;

public interface AiSettingsRepository extends JpaRepository<AiSettings, Long> {
	Optional<AiSettings> findFirstByOrderByIdAsc();
}
