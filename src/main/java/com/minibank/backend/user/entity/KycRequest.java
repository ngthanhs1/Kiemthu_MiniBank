package com.minibank.backend.user.entity;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import org.hibernate.annotations.CreationTimestamp;

import com.minibank.backend.admin.entity.AdminUser;

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
@Table(name = "kyc_requests")
public class KycRequest {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@Column(name = "full_name", nullable = false)
	private String fullName;

	@Column(name = "dob", nullable = false)
	private LocalDate dob;

	@Column(name = "citizen_id", nullable = false, length = 50)
	private String citizenId;

	@Column(name = "address", nullable = false, columnDefinition = "text")
	private String address;

	@Column(name = "occupation", length = 255)
	private String occupation;

	@Column(name = "monthly_income", precision = 18, scale = 2)
	private BigDecimal monthlyIncome;

	@Column(name = "citizen_front_image_url", columnDefinition = "text")
	private String citizenFrontImageUrl;

	@Column(name = "citizen_back_image_url", columnDefinition = "text")
	private String citizenBackImageUrl;

	@Column(name = "portrait_image_url", columnDefinition = "text")
	private String portraitImageUrl;

	@Column(nullable = false, length = 32)
	private String status;

	@CreationTimestamp
	@Column(name = "submitted_at", nullable = false, updatable = false)
	private Instant submittedAt;

	@Column(name = "reviewed_at")
	private Instant reviewedAt;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "reviewed_by_id")
	private AdminUser reviewedBy;

	@Column(name = "review_note", columnDefinition = "text")
	private String reviewNote;
}
