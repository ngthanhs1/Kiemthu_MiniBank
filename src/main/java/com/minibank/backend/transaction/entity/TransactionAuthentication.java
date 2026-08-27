package com.minibank.backend.transaction.entity;

import java.time.Instant;

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
@Table(name = "transaction_authentications")
public class TransactionAuthentication {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "transaction_id", nullable = false)
	private Transaction transaction;

	@Column(name = "pin_verified", nullable = false)
	private boolean pinVerified;

	@Column(name = "otp_code_hash")
	private String otpCodeHash;

	@Column(name = "otp_verified", nullable = false)
	private boolean otpVerified;

	@Column(name = "digital_signature", columnDefinition = "text")
	private String digitalSignature;

	@Column(name = "auth_status", nullable = false, length = 32)
	private String authStatus;

	@Column(name = "verified_at")
	private Instant verifiedAt;
}
