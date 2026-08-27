package com.minibank.backend.support.entity;

import java.time.Instant;

import com.minibank.backend.admin.entity.AdminUser;
import com.minibank.backend.user.entity.User;

import org.hibernate.annotations.ColumnTransformer;
import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
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
@Table(name = "service_requests")
public class ServiceRequest {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@Column(name = "request_type", nullable = false, length = 50)
	private String requestType;

	@Column(name = "priority_tag", length = 32)
	private String priorityTag;

	@Column(nullable = false, length = 32)
	private String status;

	@Column
	private String title;

	@Column(columnDefinition = "text")
	private String description;

	// Fix: cast String → jsonb on write so Hibernate doesn't send varchar to a jsonb column
	@Column(name = "payload_json", columnDefinition = "jsonb")
	@ColumnTransformer(write = "?::jsonb")
	private String payloadJson;

	@CreationTimestamp
	@Column(name = "submitted_at", nullable = false, updatable = false)
	private Instant submittedAt;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "assigned_to_id")
	private AdminUser assignedTo;

	@Column(name = "processed_at")
	private Instant processedAt;

	@Column(name = "process_note", columnDefinition = "text")
	private String processNote;
}