package com.minibank.backend.ai.entity;

import java.time.Instant;
import java.time.LocalDate;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(
	name = "ai_daily_recommendations",
	uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "day"})
)
public class AiDailyRecommendation {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "user_id", nullable = false)
	private Long userId;

	@Column(name = "day", nullable = false)
	private LocalDate day;

	@Column(name = "month", nullable = false, length = 7)
	private String month;

	@Column(name = "risk_level", nullable = false, length = 16)
	private String riskLevel;

	@Column(name = "saving_score", nullable = false)
	private int savingScore;

	@Column(name = "recommendations_json")
	private String recommendationsJson;

	@Column(name = "source", nullable = false, length = 32)
	private String source;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;
}
