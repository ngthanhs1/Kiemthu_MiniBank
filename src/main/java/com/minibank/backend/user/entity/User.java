package com.minibank.backend.user.entity;

import java.time.Instant;
import java.time.LocalDate;

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
@Table(name = "users")
public class User {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, unique = true, length = 20)
	private String phone;

	@Column(nullable = false, unique = true)
	private String email;

	@Column(name = "password_hash", nullable = false)
	private String passwordHash;

	@Column(name = "full_name")
	private String fullName;

	@Column(name = "dob")
	private LocalDate dob;

	@Column(name = "citizen_id", length = 50)
	private String citizenId;

	@Column(columnDefinition = "text")
	private String address;

	@Column(nullable = false, length = 32)
	private String status;

	@Column(name = "customer_rank", nullable = false, length = 32)
	private String customerRank;

	@Column(name = "credit_score_level", length = 16)
	private String creditScoreLevel;

	@Column(name = "transaction_pin_hash")
	private String transactionPinHash;

	@Column(name = "public_key", columnDefinition = "text")
	private String publicKey;

	@Column(name = "device_id")
	private String deviceId;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@UpdateTimestamp
	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;
}
