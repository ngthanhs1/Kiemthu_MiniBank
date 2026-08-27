package com.minibank.backend.admin.controller;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.minibank.backend.admin.entity.AdminUser;
import com.minibank.backend.admin.repository.AdminUserRepository;
import com.minibank.backend.approval.dto.ApprovalProgressResponse;
import com.minibank.backend.approval.service.MultiStepApprovalService;
import com.minibank.backend.common.security.CurrentJwt;
import com.minibank.backend.contract.entity.Contract;
import com.minibank.backend.contract.entity.ContractTemplate;
import com.minibank.backend.contract.repository.ContractRepository;
import com.minibank.backend.contract.repository.ContractTemplateRepository;
import com.minibank.backend.loan.entity.LoanApplication;
import com.minibank.backend.loan.repository.LoanApplicationRepository;
import com.minibank.backend.saving.dto.SavingResponse;
import com.minibank.backend.saving.repository.SavingRepository;
import com.minibank.backend.system.service.NotificationService;
import com.minibank.backend.user.entity.Document;
import com.minibank.backend.user.repository.DocumentRepository;

@RestController
@RequestMapping("/api/admin/approvals")
public class AdminApprovalController {
    private final LoanApplicationRepository loanApplicationRepository;
    private final SavingRepository savingRepository;
    private final ContractTemplateRepository templateRepository;
    private final ContractRepository contractRepository;
    private final AdminUserRepository adminUserRepository;
    private final DocumentRepository documentRepository;
    private final MultiStepApprovalService multiStepApprovalService;
    private final NotificationService notificationService;

    public AdminApprovalController(
        LoanApplicationRepository loanApplicationRepository,
        SavingRepository savingRepository,
        ContractTemplateRepository templateRepository,
        ContractRepository contractRepository,
        AdminUserRepository adminUserRepository,
        DocumentRepository documentRepository,
        MultiStepApprovalService multiStepApprovalService,
        NotificationService notificationService
    ) {
        this.loanApplicationRepository = loanApplicationRepository;
        this.savingRepository = savingRepository;
        this.templateRepository = templateRepository;
        this.contractRepository = contractRepository;
        this.adminUserRepository = adminUserRepository;
        this.documentRepository = documentRepository;
        this.multiStepApprovalService = multiStepApprovalService;
        this.notificationService = notificationService;
    }

    @GetMapping("/loan-applications")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'LOAN_APPLICATION_APPROVAL')")
    @Transactional(readOnly = true)
    public List<LoanApprovalSummary> listLoanApplications(
        @RequestParam(defaultValue = "pending") String status
    ) {
        java.util.LinkedHashSet<String> statuses = new java.util.LinkedHashSet<>();
        for (String token : status.split(",")) {
            String normalized = token.trim();
            if (!normalized.isBlank()) statuses.add(normalized);
        }

        if (statuses.isEmpty()) {
            statuses.add("pending");
        }

        return statuses.stream()
            .flatMap(s -> loanApplicationRepository.findByStatusOrderBySubmittedAtDesc(s).stream())
            .sorted(java.util.Comparator.comparing(LoanApplication::getSubmittedAt).reversed())
            .map(this::toLoanApprovalSummary)
            .toList();
    }

    @GetMapping("/loan-applications/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'LOAN_APPLICATION_APPROVAL')")
    @Transactional(readOnly = true)
    public LoanApprovalSummary getLoanApplication(@PathVariable Long id) {
        LoanApplication app = loanApplicationRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Loan application not found"));
        return toLoanApprovalSummary(app);
    }

    @GetMapping("/savings")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'SAVING_APPROVAL')")
    @Transactional(readOnly = true)
    public List<SavingResponse> listSavings(
        @RequestParam(defaultValue = "pending_otp,pending_approval,pending_contract") String status
    ) {
        java.util.LinkedHashSet<String> statuses = new java.util.LinkedHashSet<>();
        for (String token : status.split(",")) {
            String normalized = token.trim();
            if (!normalized.isBlank()) statuses.add(normalized);
        }
        return statuses.stream()
            .flatMap(s -> savingRepository.findByStatusOrderByCreatedAtDesc(s).stream())
            .sorted(java.util.Comparator.comparing(com.minibank.backend.saving.entity.Saving::getCreatedAt).reversed())
            .map(SavingResponse::from)
            .toList();
    }

    @PostMapping("/loan-applications/{id}/approve")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'LOAN_APPLICATION_APPROVAL')")
    @ResponseStatus(HttpStatus.OK)
    @Transactional
    public LoanApprovalSummary approveLoanApplication(@PathVariable Long id, @RequestBody(required = false) ApproveLoanRequest req) {
        LoanApplication app = loanApplicationRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Loan application not found"));

        if (!"pending".equalsIgnoreCase(app.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Loan application is not pending");
        }

        ApprovalProgressResponse progress = multiStepApprovalService.approve(
            "loan_application",
            app.getId(),
            app.getRequestedAmount(),
            CurrentJwt.requireUserId(),
            req != null ? req.note : null
        );

        if (progress.finalApproved()) {
            AdminUser reviewer = requireReviewer();
            app.setStatus("approved");
            app.setReviewedAt(Instant.now());
            app.setReviewedBy(reviewer);
            app.setReviewNote(req != null ? req.note : null);
            loanApplicationRepository.save(app);

            Contract existing = contractRepository
                .findByOwnerTypeAndOwnerIdOrderByCreatedAtDesc("loan_application", app.getId())
                .stream()
                .findFirst()
                .orElse(null);

            if (existing == null) {
                ContractTemplate template = resolveTemplateForLoan(req);
                Contract contract = Contract.builder()
                    .ownerType("loan_application")
                    .ownerId(app.getId())
                    .template(template)
                    .contractNumber(generateLoanContractNumber())
                    .status("SENT")
                    .createdBy(reviewer)
                    .build();
                contractRepository.save(contract);
            }

            if (app.getUser() != null && app.getUser().getId() != null) {
                notificationService.createForUser(
                    app.getUser().getId(),
                    "LOAN_APPLICATION",
                    "Hồ sơ vay đã được duyệt",
                    "Hồ sơ vay #" + app.getId() + " của bạn đã được duyệt. Vui lòng vào mục Hợp đồng để xem và ký hợp đồng."
                );
            }
        }

        return toLoanApprovalSummary(app);
    }

    @PostMapping("/loan-applications/{id}/reject")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'LOAN_APPLICATION_APPROVAL')")
    @ResponseStatus(HttpStatus.OK)
    @Transactional
    public LoanApprovalSummary rejectLoanApplication(@PathVariable Long id, @RequestBody(required = false) RejectLoanRequest req) {
        LoanApplication app = loanApplicationRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Loan application not found"));

        if (!"pending".equalsIgnoreCase(app.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Loan application is not pending");
        }

        multiStepApprovalService.reject(
            "loan_application",
            app.getId(),
            app.getRequestedAmount(),
            CurrentJwt.requireUserId(),
            req != null ? req.reason : null
        );
        app.setStatus("rejected");
        app.setReviewedAt(Instant.now());
        app.setReviewedBy(requireReviewer());
        app.setReviewNote(req != null ? req.reason : null);
        loanApplicationRepository.save(app);

        if (app.getUser() != null && app.getUser().getId() != null) {
            String reason = (req != null && req.reason != null && !req.reason.isBlank())
                ? " Lý do: " + req.reason
                : "";
            notificationService.createForUser(
                app.getUser().getId(),
                "LOAN_APPLICATION",
                "Hồ sơ vay bị từ chối",
                "Hồ sơ vay #" + app.getId() + " của bạn đã bị từ chối." + reason
            );
        }

        return toLoanApprovalSummary(app);
    }

    private LoanApprovalSummary toLoanApprovalSummary(LoanApplication app) {
        Contract latestContract = contractRepository
            .findByOwnerTypeAndOwnerIdOrderByCreatedAtDesc("loan_application", app.getId())
            .stream()
            .findFirst()
            .orElse(null);
        Map<String, String> userDocUrls = resolveUserIdentityDocs(
            app.getUser() != null ? app.getUser().getId() : null
        );

        return new LoanApprovalSummary(
            app.getId(),
            app.getUser() != null ? app.getUser().getId() : null,
            app.getUser() != null ? app.getUser().getFullName() : null,
            app.getUser() != null ? app.getUser().getPhone() : null,
            app.getUser() != null ? app.getUser().getEmail() : null,
            app.getUser() != null ? app.getUser().getDob() : null,
            app.getUser() != null ? app.getUser().getAddress() : null,
            app.getUser() != null ? app.getUser().getCitizenId() : null,
            app.getUser() != null ? app.getUser().getCustomerRank() : null,
            app.getUser() != null ? app.getUser().getCreditScoreLevel() : null,
            app.getLoanProduct() != null ? app.getLoanProduct().getId() : null,
            app.getLoanProduct() != null ? app.getLoanProduct().getCode() : null,
            app.getLoanProduct() != null ? app.getLoanProduct().getName() : null,
            app.getLoanProduct() != null ? app.getLoanProduct().getLoanType() : null,
            app.getRequestedAmount(),
            app.getRequestedTermMonths(),
            app.getMonthlyIncome(),
            app.getPurpose(),
            app.getCollateralDescription(),
            app.getIncomeProofUrl(),
            app.getCollateralProofUrl(),
            userDocUrls.get("cccd_front"),
            userDocUrls.get("cccd_back"),
            userDocUrls.get("selfie"),
            app.getPriorityTag(),
            app.getStatus(),
            app.getSubmittedAt(),
            app.getReviewedAt(),
            app.getReviewNote(),
            app.getCollateralDescription() != null && !app.getCollateralDescription().isBlank(),
            latestContract != null ? latestContract.getId() : null,
            latestContract != null ? latestContract.getContractNumber() : null,
            latestContract != null ? latestContract.getStatus() : null,
            multiStepApprovalService.findProgress("loan_application", app.getId())
        );
    }

    private Map<String, String> resolveUserIdentityDocs(Long userId) {
        if (userId == null) return Map.of();
        List<Document> docs = documentRepository
            .findTop20ByOwnerTypeIgnoreCaseAndOwnerIdAndDocumentTypeStartingWithIgnoreCaseOrderByUploadedAtDesc(
                "USER",
                userId,
                ""
            );
        java.util.LinkedHashMap<String, String> result = new java.util.LinkedHashMap<>();
        for (Document doc : docs) {
            if (doc == null || doc.getDocumentType() == null || doc.getFileUrl() == null || doc.getFileUrl().isBlank()) {
                continue;
            }
            String normalized = doc.getDocumentType().trim().toLowerCase(Locale.ROOT);
            if (!result.containsKey("cccd_front") &&
                (normalized.equals("cccd_front") || normalized.equals("saving_cccd_front"))) {
                result.put("cccd_front", doc.getFileUrl());
            }
            if (!result.containsKey("cccd_back") &&
                (normalized.equals("cccd_back") || normalized.equals("saving_cccd_back"))) {
                result.put("cccd_back", doc.getFileUrl());
            }
            if (!result.containsKey("selfie") &&
                (normalized.equals("selfie") || normalized.equals("saving_selfie"))) {
                result.put("selfie", doc.getFileUrl());
            }
            if (result.size() >= 3) break;
        }
        return result;
    }

    private AdminUser requireReviewer() {
        long adminId = CurrentJwt.requireUserId();
        return adminUserRepository.findById(adminId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Admin not found"));
    }

    private ContractTemplate resolveTemplateForLoan(ApproveLoanRequest req) {
        if (req != null && req.templateId != null) {
            return templateRepository.findById(req.templateId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Template not found"));
        }

        List<ContractTemplate> activeTemplates = templateRepository.findActiveByService("loan");
        if (!activeTemplates.isEmpty()) {
            return activeTemplates.get(0);
        }

        return templateRepository.findAllByOrderByCreatedAtDesc()
            .stream()
            .filter(template -> supportsService(template, "loan"))
            .findFirst()
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "No loan contract template configured"
            ));
    }

    private boolean supportsService(ContractTemplate template, String service) {
        if (template == null || template.getServices() == null || service == null) {
            return false;
        }

        String normalizedService = service.trim().toLowerCase(Locale.ROOT);
        for (String token : template.getServices().split(",")) {
            String normalizedToken = token.trim().toLowerCase(Locale.ROOT);
            if (normalizedToken.equals(normalizedService) || normalizedToken.equals("general")) {
                return true;
            }
        }

        return false;
    }

    private String generateLoanContractNumber() {
        String year = String.valueOf(LocalDate.now().getYear());
        String suffix = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        return "HD-LOAN-" + year + "-" + suffix;
    }

    public static class ApproveLoanRequest {
        public Long templateId;
        public String note;
    }

    public static class RejectLoanRequest {
        public String reason;
    }

    public record LoanApprovalSummary(
        Long id,
        Long userId,
        String userFullName,
        String userPhone,
        String userEmail,
        java.time.LocalDate userDob,
        String userAddress,
        String userCitizenId,
        String customerRank,
        String creditScoreLevel,
        Long loanProductId,
        String productCode,
        String productName,
        String loanType,
        java.math.BigDecimal requestedAmount,
        int termMonths,
        java.math.BigDecimal monthlyIncome,
        String purpose,
        String collateralDescription,
        String incomeProofUrl,
        String collateralProofUrl,
        String cccdFrontUrl,
        String cccdBackUrl,
        String selfieUrl,
        String priorityTag,
        String status,
        Instant submittedAt,
        Instant reviewedAt,
        String reviewNote,
        boolean hasCollateral,
        Long contractId,
        String contractNumber,
        String contractStatus,
        ApprovalProgressResponse approvalProgress
    ) {}
}
