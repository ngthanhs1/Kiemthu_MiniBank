package com.minibank.backend.system.entity;

import java.time.Instant;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

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
@Table(name = "system_logs")
public class SystemLog {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "actor_type", nullable = false, length = 32)
	private String actorType;

	@Column(name = "actor_id")
	private Long actorId;

	@Column(nullable = false, length = 100)
	private String action;

	@Column(name = "target_type", length = 50)
	private String targetType;

	@Column(name = "target_id")
	private Long targetId;

	@Column(name = "metadata_json", columnDefinition = "jsonb")
	@JdbcTypeCode(SqlTypes.JSON)
	private String metadataJson;

	@Column(name = "ip_address", length = 64)
	private String ipAddress;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;
}
