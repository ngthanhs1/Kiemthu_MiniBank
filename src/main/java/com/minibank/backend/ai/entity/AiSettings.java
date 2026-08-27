package com.minibank.backend.ai.entity;

import java.time.Instant;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
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
@Table(name = "ai_settings")
public class AiSettings {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "classification_enabled", nullable = false)
	private boolean classificationEnabled;

	@Column(name = "classification_frequency_minutes", nullable = false)
	private int classificationFrequencyMinutes;

	@Column(name = "classification_start_time", length = 8)
	private String classificationStartTime;

	@Column(name = "recommendation_enabled", nullable = false)
	private boolean recommendationEnabled;

	@Column(name = "recommendation_frequency_minutes", nullable = false)
	private int recommendationFrequencyMinutes;

	@Column(name = "recommendation_start_time", length = 8)
	private String recommendationStartTime;

	@Column(name = "last_classification_run")
	private Instant lastClassificationRun;

	@Column(name = "last_recommendation_run")
	private Instant lastRecommendationRun;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@UpdateTimestamp
	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;
}
