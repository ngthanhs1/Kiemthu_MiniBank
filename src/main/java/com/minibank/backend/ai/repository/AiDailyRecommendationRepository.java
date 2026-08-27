package com.minibank.backend.ai.repository;

import java.time.LocalDate;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.minibank.backend.ai.entity.AiDailyRecommendation;

public interface AiDailyRecommendationRepository extends JpaRepository<AiDailyRecommendation, Long> {
	Optional<AiDailyRecommendation> findFirstByUserIdAndDay(Long userId, LocalDate day);
}
