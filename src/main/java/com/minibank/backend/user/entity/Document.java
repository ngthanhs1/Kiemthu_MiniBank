package com.minibank.backend.user.entity;

import java.time.Instant;

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
@Table(name = "documents")
public class Document {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "owner_type", nullable = false, length = 50)
	private String ownerType;

	@Column(name = "owner_id", nullable = false)
	private Long ownerId;

	@Column(name = "document_type", nullable = false, length = 100)
	private String documentType;

	@Column(name = "file_name")
	private String fileName;

	@Column(name = "file_url", nullable = false, columnDefinition = "text")
	private String fileUrl;

	@Column(name = "mime_type", length = 100)
	private String mimeType;

	@Column(name = "verified_status", nullable = false, length = 32)
	private String verifiedStatus;

	@Column(name = "uploaded_by_type", nullable = false, length = 32)
	private String uploadedByType;

	@Column(name = "uploaded_by_id")
	private Long uploadedById;

	@CreationTimestamp
	@Column(name = "uploaded_at", nullable = false, updatable = false)
	private Instant uploadedAt;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "verified_by_id")
	private AdminUser verifiedBy;

	@Column(name = "verified_at")
	private Instant verifiedAt;

	@Column(columnDefinition = "text")
	private String note;

	@Column(name = "signed_status", length = 32)
	private String signedStatus;

	@Column(name = "signed_by_user_id")
	private Long signedByUserId;

	@Column(name = "signed_at")
	private Instant signedAt;
}
