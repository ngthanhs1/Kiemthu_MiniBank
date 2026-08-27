package com.minibank.backend.admin.controller;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import com.minibank.backend.admin.dto.DocumentCreateRequest;
import com.minibank.backend.admin.dto.DocumentPageResponse;
import com.minibank.backend.admin.dto.DocumentSummary;
import com.minibank.backend.admin.dto.DocumentVerifyRequest;
import com.minibank.backend.admin.entity.AdminUser;
import com.minibank.backend.admin.repository.AdminUserRepository;
import com.minibank.backend.common.security.CurrentJwt;
import com.minibank.backend.user.entity.Document;
import com.minibank.backend.user.repository.DocumentRepository;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/admin/documents")
@PreAuthorize("hasRole('ADMIN')")
public class AdminDocumentController {
	private static final int MAX_PAGE_SIZE = 200;

	private final DocumentRepository documentRepository;
	private final AdminUserRepository adminUserRepository;

	public AdminDocumentController(DocumentRepository documentRepository, AdminUserRepository adminUserRepository) {
		this.documentRepository = documentRepository;
		this.adminUserRepository = adminUserRepository;
	}

	@GetMapping
	@Transactional(readOnly = true)
	public DocumentPageResponse list(
		@RequestParam(value = "ownerType", required = false) String ownerType,
		@RequestParam(value = "ownerId", required = false) Long ownerId,
		@RequestParam(value = "status", required = false) String status,
		@RequestParam(value = "documentType", required = false) String documentType,
		@RequestParam(value = "page", defaultValue = "0") int page,
		@RequestParam(value = "size", defaultValue = "50") int size
	) {
		int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
		Page<Document> result = documentRepository.search(
			normalize(ownerType),
			ownerId,
			normalize(status),
			normalize(documentType),
			PageRequest.of(Math.max(page, 0), safeSize)
		);
		List<DocumentSummary> items = result.getContent().stream().map(this::toSummary).toList();
		return new DocumentPageResponse(items, result.getTotalElements(), Math.max(page, 0), safeSize);
	}

	@GetMapping("/{documentId}")
	@Transactional(readOnly = true)
	public DocumentSummary get(@PathVariable Long documentId) {
		Document doc = documentRepository.findById(documentId)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Document not found"));
		return toSummary(doc);
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@Transactional
	public DocumentSummary create(@Valid @RequestBody DocumentCreateRequest request) {
		long adminUserId = CurrentJwt.requireUserId();
		Document doc = Document.builder()
			.ownerType(request.ownerType().trim())
			.ownerId(request.ownerId())
			.documentType(request.documentType().trim())
			.fileName(blankToNull(request.fileName()))
			.fileUrl(request.fileUrl().trim())
			.mimeType(blankToNull(request.mimeType()))
			.verifiedStatus("pending")
			.uploadedByType("ADMIN")
			.uploadedById(adminUserId)
			.note(blankToNull(request.note()))
			.build();
		documentRepository.save(doc);
		return toSummary(doc);
	}

	@PostMapping("/upload")
	@ResponseStatus(HttpStatus.CREATED)
	public java.util.Map<String, String> uploadFile(@RequestPart("file") MultipartFile file) throws Exception {
		if (file == null || file.isEmpty()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing file");
		}

		Path uploadDir = Path.of("data", "uploads");
		Files.createDirectories(uploadDir);
		String ext = "";
		String original = file.getOriginalFilename();
		if (original != null && original.contains(".")) {
			ext = original.substring(original.lastIndexOf('.'));
		}
		String name = UUID.randomUUID().toString() + ext;
		Path target = uploadDir.resolve(name);
		try (var in = file.getInputStream()) {
			Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
		}
		String fileUrl = "/uploads/" + name; // serve static files via server config (TODO)
		return java.util.Map.of("fileUrl", fileUrl);
	}

	@PostMapping("/{documentId}/verify")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@Transactional
	public void verify(@PathVariable Long documentId, @Valid @RequestBody DocumentVerifyRequest request) {
		String status = normalize(request.status());
		if (status == null || (!status.equals("approved") && !status.equals("rejected"))) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid status");
		}

		long adminUserId = CurrentJwt.requireUserId();
		AdminUser adminUser = adminUserRepository.findById(adminUserId)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Admin not found"));

		Document doc = documentRepository.findById(documentId)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Document not found"));

		doc.setVerifiedStatus(status);
		doc.setVerifiedBy(adminUser);
		doc.setVerifiedAt(Instant.now());
		doc.setNote(blankToNull(request.note()));
		documentRepository.save(doc);
	}

	private DocumentSummary toSummary(Document doc) {
		Long verifiedById = doc.getVerifiedBy() == null ? null : doc.getVerifiedBy().getId();
		return new DocumentSummary(
			doc.getId(),
			doc.getOwnerType(),
			doc.getOwnerId(),
			doc.getDocumentType(),
			doc.getFileName(),
			doc.getFileUrl(),
			doc.getMimeType(),
			doc.getVerifiedStatus(),
			doc.getUploadedByType(),
			doc.getUploadedById(),
			doc.getUploadedAt(),
			verifiedById,
			doc.getVerifiedAt(),
			doc.getNote()
		);
	}

	private static String normalize(String value) {
		if (value == null) return null;
		String trimmed = value.trim();
		return trimmed.isEmpty() ? null : trimmed.toLowerCase();
	}

	private static String blankToNull(String value) {
		if (value == null) return null;
		String trimmed = value.trim();
		return trimmed.isEmpty() ? null : trimmed;
	}
}
