package com.minibank.backend.contract.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.minibank.backend.common.security.CurrentJwt;
import com.minibank.backend.contract.dto.TemplateDetail;
import com.minibank.backend.contract.dto.TemplateSummary;
import com.minibank.backend.contract.dto.TemplateUpsertRequest;
import com.minibank.backend.contract.service.ContractTemplateService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * Admin API — quản lý contract template.
 *
 * Endpoints:
 *   GET    /api/admin/contract-templates              — Danh sách tất cả template
 *   GET    /api/admin/contract-templates/{id}         — Chi tiết một template
 *   POST   /api/admin/contract-templates              — Tạo template mới (JSON)
 *   PUT    /api/admin/contract-templates/{id}         — Cập nhật template (JSON)
 *   PATCH  /api/admin/contract-templates/{id}/activate — Activate template, auto-archive cùng loại
 *   PATCH  /api/admin/contract-templates/{id}/archive  — Archive thủ công
 *   POST   /api/admin/contract-templates/upload       — Upload file .docx
 *   DELETE /api/admin/contract-templates/{id}         — Xóa (chỉ draft/archived)
 */
@RestController
@RequestMapping("/api/admin/contract-templates")
@RequiredArgsConstructor
public class AdminContractTemplateController {

    private final ContractTemplateService templateService;

    // ── List ──────────────────────────────────────────────────────────────────

    @GetMapping
    public List<TemplateSummary> listAll() {
        CurrentJwt.requireUserId();
        return templateService.listAll();
    }

    @GetMapping("/{id}")
    public TemplateDetail getDetail(@PathVariable Long id) {
        CurrentJwt.requireUserId();
        return templateService.getDetail(id);
    }

    // ── Create / Update ───────────────────────────────────────────────────────

    @PostMapping
    public TemplateDetail create(@Valid @RequestBody TemplateUpsertRequest req) {
        Long adminId = CurrentJwt.requireUserId();
        return templateService.create(req, adminId);
    }

    @PutMapping("/{id}")
    public TemplateDetail update(
            @PathVariable Long id,
            @Valid @RequestBody TemplateUpsertRequest req) {
        Long adminId = CurrentJwt.requireUserId();
        return templateService.update(id, req, adminId);
    }

    // ── Activate / Archive ────────────────────────────────────────────────────

    /**
     * Activate một template.
     * Logic:
     *  1. Tìm template cần activate (phải ở trạng thái draft).
     *  2. Tìm tất cả template cùng `code` (hoặc cùng `services`) đang active → archive.
     *  3. Set template này thành active.
     *
     * Chỉ 1 bản active tại một thời điểm cho mỗi loại contract (code).
     */
    @PatchMapping("/{id}/activate")
    public TemplateDetail activate(@PathVariable Long id) {
        Long adminId = CurrentJwt.requireUserId();
        return templateService.activate(id, adminId);
    }

    /**
     * Archive thủ công — không thể archive bản đang active nếu không có bản khác thay thế.
     */
    @PatchMapping("/{id}/archive")
    public TemplateDetail archive(@PathVariable Long id) {
        Long adminId = CurrentJwt.requireUserId();
        templateService.archive(id, adminId);
        return templateService.getDetail(id);
    }

    // ── Upload DOCX ───────────────────────────────────────────────────────────

    @PostMapping("/upload")
    public TemplateDetail upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam("name") String name,
            @RequestParam("code") String code,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "services", defaultValue = "loan") String services) throws Exception {
        Long adminId = CurrentJwt.requireUserId();
        return templateService.uploadDocx(file, name, code, description, services, adminId);
    }

    // ── Delete ────────────────────────────────────────────────────────────────

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        Long adminId = CurrentJwt.requireUserId();
        templateService.delete(id, adminId);
        return ResponseEntity.noContent().build();
    }
}