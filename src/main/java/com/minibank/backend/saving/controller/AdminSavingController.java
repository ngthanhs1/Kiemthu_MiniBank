package com.minibank.backend.saving.controller;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.minibank.backend.approval.dto.ApprovalProgressResponse;
import com.minibank.backend.approval.service.MultiStepApprovalService;
import com.minibank.backend.contract.entity.Contract;
import com.minibank.backend.contract.repository.ContractRepository;
import com.minibank.backend.admin.service.AdminServiceRequestService;
import com.minibank.backend.common.security.CurrentJwt;
import com.minibank.backend.saving.dto.SavingResponse;
import com.minibank.backend.saving.entity.Saving;
import com.minibank.backend.saving.repository.SavingRepository;
import com.minibank.backend.saving.service.SavingService;
import com.minibank.backend.saving.service.SavingSettlementRequestService;
import com.minibank.backend.saving.service.SavingSettlementRequestService.DecisionRequest;
import com.minibank.backend.saving.service.SavingSettlementRequestService.SettlementRequestItem;
import com.minibank.backend.user.entity.Document;
import com.minibank.backend.user.repository.DocumentRepository;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/admin/savings")
@RequiredArgsConstructor
public class AdminSavingController {

    private final SavingRepository savingRepository;
    private final SavingService savingService;
    private final AdminServiceRequestService adminServiceRequestService;
    private final DocumentRepository documentRepository;
    private final ContractRepository contractRepository;
    private final MultiStepApprovalService multiStepApprovalService;
    private final SavingSettlementRequestService savingSettlementRequestService;

    @GetMapping
    @Transactional(readOnly = true)
    public List<SavingResponse> list(@RequestParam(required = false) String status) {
        List<Saving> items = loadSavings(status);
        return items.stream()
            .map(saving -> SavingResponse.from(saving, multiStepApprovalService.findProgress("saving", saving.getId())))
            .toList();
    }

    @GetMapping("/closure-requests")
    @Transactional(readOnly = true)
    public List<SettlementRequestItem> listClosureRequests(
        @RequestParam(value = "q", required = false) String q,
        @RequestParam(value = "type", required = false) String type,
        @RequestParam(value = "status", required = false) String status
    ) {
        return savingSettlementRequestService.listForAdmin(q, type, status);
    }

    @GetMapping("/closure-requests/{requestId}")
    @Transactional(readOnly = true)
    public SettlementRequestItem getClosureRequest(@PathVariable long requestId) {
        return savingSettlementRequestService.getForAdmin(requestId);
    }

    @PostMapping("/closure-requests/{requestId}/approve")
    public SettlementRequestItem approveClosureRequest(
        @PathVariable long requestId,
        @RequestBody(required = false) DecisionRequest request
    ) {
        return savingSettlementRequestService.approve(requestId, CurrentJwt.requireUserId(), request);
    }

    @PostMapping("/closure-requests/{requestId}/reject")
    public SettlementRequestItem rejectClosureRequest(
        @PathVariable long requestId,
        @RequestBody(required = false) DecisionRequest request
    ) {
        return savingSettlementRequestService.reject(requestId, CurrentJwt.requireUserId(), request);
    }

    @GetMapping("/{id}")
    @Transactional(readOnly = true)
    public SavingApprovalDetail getDetail(@PathVariable Long id) {
        Saving saving = savingRepository.findWithDetailsById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Saving not found"));

        List<SavingDocumentItem> documents = documentRepository
            .findTop20ByOwnerTypeIgnoreCaseAndOwnerIdAndDocumentTypeStartingWithIgnoreCaseOrderByUploadedAtDesc(
                "USER",
                saving.getUser().getId(),
                "saving_"
            )
            .stream()
            // keep only newest file per type to avoid duplicated rows in admin UI
            .collect(
                java.util.stream.Collectors.toMap(
                    d -> normalizeDocType(d.getDocumentType()),
                    d -> d,
                    (left, right) -> left,
                    java.util.LinkedHashMap::new
                )
            )
            .values()
            .stream()
            .map(this::toDocumentItem)
            .toList();

        Contract latestContract = contractRepository
            .findByOwnerTypeAndOwnerIdOrderByCreatedAtDesc("saving", saving.getId())
            .stream()
            .findFirst()
            .orElse(null);

        return new SavingApprovalDetail(
            saving.getId(),
            saving.getCode(),
            saving.getStatus(),
            saving.getPrincipalAmount(),
            saving.getActualInterestRate(),
            saving.getTermUnit(),
            saving.getTermValue(),
            saving.isAutoRenew(),
            saving.getOpenDate(),
            saving.getMaturityDate(),
            saving.getAgreementAcceptedAt(),
            saving.getAgreementVersion(),
            saving.getSourceAccount() != null ? saving.getSourceAccount().getId() : null,
            saving.getSourceAccount() != null ? saving.getSourceAccount().getAccountNumber() : null,
            saving.getSourceAccount() != null ? saving.getSourceAccount().getAccountName() : null,
            saving.getSettlementAccount() != null ? saving.getSettlementAccount().getId() : null,
            saving.getSettlementAccount() != null ? saving.getSettlementAccount().getAccountNumber() : null,
            saving.getSettlementAccount() != null ? saving.getSettlementAccount().getAccountName() : null,
            saving.getSavingProduct() != null ? saving.getSavingProduct().getId() : null,
            saving.getSavingProduct() != null ? saving.getSavingProduct().getCode() : null,
            saving.getSavingProduct() != null ? saving.getSavingProduct().getName() : null,
            saving.getUser() != null ? saving.getUser().getId() : null,
            saving.getUser() != null ? saving.getUser().getFullName() : null,
            saving.getUser() != null ? saving.getUser().getPhone() : null,
            saving.getUser() != null ? saving.getUser().getEmail() : null,
            saving.getUser() != null ? saving.getUser().getDob() : null,
            saving.getUser() != null ? saving.getUser().getAddress() : null,
            saving.getUser() != null ? saving.getUser().getCitizenId() : null,
            saving.getUser() != null ? saving.getUser().getCustomerRank() : null,
            saving.getUser() != null ? saving.getUser().getCreditScoreLevel() : null,
            saving.getRejectionReason(),
            documents,
            latestContract != null ? latestContract.getId() : null,
            latestContract != null ? latestContract.getContractNumber() : null,
            latestContract != null ? latestContract.getStatus() : null,
            multiStepApprovalService.findProgress("saving", saving.getId())
        );
    }

    public static record ApproveRequest(String note) {}

    @PostMapping("/{id}/approve")
    public SavingResponse approve(@PathVariable Long id, @RequestBody(required = false) ApproveRequest body) {
        Saving saving = savingRepository.findWithDetailsById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Saving not found"));
        ApprovalProgressResponse progress = multiStepApprovalService.approve(
            "saving",
            saving.getId(),
            saving.getPrincipalAmount(),
            com.minibank.backend.common.security.CurrentJwt.requireUserId(),
            body != null ? body.note() : null
        );
        if (progress.finalApproved()) {
            return savingService.approveSaving(id);
        }
        return SavingResponse.from(saving, progress);
    }

    public static record RejectRequest(String reason) {}

    @PostMapping("/{id}/reject")
    public void reject(
        @PathVariable Long id,
        @RequestBody(required = false) RejectRequest body
    ) {
        Saving saving = savingRepository.findWithDetailsById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Saving not found"));
        multiStepApprovalService.reject(
            "saving",
            saving.getId(),
            saving.getPrincipalAmount(),
            com.minibank.backend.common.security.CurrentJwt.requireUserId(),
            body != null ? body.reason() : null
        );
        savingService.rejectSaving(id, body != null ? body.reason() : null);
    }

    public static record CloseSavingRequest(Long settlementAccountId, String note) {}

    @PostMapping("/{id}/close")
    public void closeManually(
        @PathVariable Long id,
        @RequestBody(required = false) CloseSavingRequest body
    ) {
        long adminUserId = CurrentJwt.requireUserId();
        adminServiceRequestService.closeSavingManually(
            id,
            body != null ? body.settlementAccountId() : null,
            adminUserId,
            body != null ? body.note() : null
        );
    }

    private List<Saving> loadSavings(String status) {
        if (status == null || status.isBlank()) {
            return savingRepository.findAllByOrderByCreatedAtDesc();
        }

        Set<String> statuses = Arrays.stream(status.split(","))
            .map(String::trim)
            .filter(s -> !s.isBlank())
            .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));

        if (statuses.isEmpty()) {
            return savingRepository.findAllByOrderByCreatedAtDesc();
        }
        if (statuses.size() == 1) {
            return savingRepository.findByStatusOrderByCreatedAtDesc(statuses.iterator().next());
        }

        return statuses.stream()
            .flatMap(s -> savingRepository.findByStatusOrderByCreatedAtDesc(s).stream())
            .sorted(Comparator.comparing(Saving::getCreatedAt).reversed())
            .toList();
    }

    private SavingDocumentItem toDocumentItem(Document doc) {
        return new SavingDocumentItem(
            doc.getId(),
            doc.getDocumentType(),
            doc.getFileName(),
            doc.getFileUrl(),
            doc.getMimeType(),
            doc.getVerifiedStatus(),
            doc.getUploadedAt(),
            doc.getNote()
        );
    }

    private String normalizeDocType(String docType) {
        if (docType == null) return "";
        return docType.trim().toLowerCase(Locale.ROOT);
    }

    public record SavingDocumentItem(
        Long id,
        String documentType,
        String fileName,
        String fileUrl,
        String mimeType,
        String verifiedStatus,
        Instant uploadedAt,
        String note
    ) {}

    public record SavingApprovalDetail(
        Long id,
        String code,
        String status,
        BigDecimal principalAmount,
        BigDecimal actualInterestRate,
        String termUnit,
        int termValue,
        boolean autoRenew,
        Instant openDate,
        Instant maturityDate,
        Instant agreementAcceptedAt,
        String agreementVersion,
        Long sourceAccountId,
        String sourceAccountNumber,
        String sourceAccountName,
        Long settlementAccountId,
        String settlementAccountNumber,
        String settlementAccountName,
        Long productId,
        String productCode,
        String productName,
        Long userId,
        String userFullName,
        String userPhone,
        String userEmail,
        java.time.LocalDate userDob,
        String userAddress,
        String userCitizenId,
        String customerRank,
        String creditScoreLevel,
        String rejectionReason,
        List<SavingDocumentItem> documents,
        Long contractId,
        String contractNumber,
        String contractStatus,
        ApprovalProgressResponse approvalProgress
    ) {}
}
