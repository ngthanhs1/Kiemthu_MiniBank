"use client";

// admin/pages/contracts/page.tsx
// Trang quản lý hợp đồng & thỏa thuận — phiên bản đầy đủ.
//
// Tính năng:
//  - Hiển thị 3 nhóm template: tiết kiệm / vay tín dụng / vay thế chấp
//  - Mỗi nhóm chỉ 1 bản ACTIVE — nút "Activate" archive bản cũ tự động
//  - Tạo / chỉnh sửa / upload .docx / xóa template
//  - Xem danh sách khách hàng đã ký + tải file hợp đồng

import { useEffect, useMemo, useRef, useState } from "react";
import AdminShell from "../components/admin-shell";

import {
  activateContractTemplate,
  archiveContractTemplate,
  createContractTemplate,
  deleteContractTemplate,
  getContractTemplate,
  listContractAcceptances,
  listContractTemplates,
  listGeneratedContracts,
  updateContractTemplate,
  uploadContractTemplate,
  type ContractAcceptanceSummary,
  type GeneratedContract,
  type TemplateDetail,
  type TemplatePlaceholder,
  type TemplateSummary,
} from "../../lib/api/admin-contracts";

// ─── Constants ────────────────────────────────────────────────────────────────

const CONTRACT_GROUPS = [
  {
    key: "saving",
    label: "Thỏa thuận tiết kiệm",
    code: "SAVING_AGREEMENT",
    aliases: ["SAVING_AGREEMENT"],
    services: "saving",
    color: "emerald",
    description: "Thỏa thuận mở sổ tiết kiệm. Có thể dùng placeholder để điền dữ liệu tự động.",
  },
  {
    key: "loan_credit",
    label: "Hợp đồng vay tín dụng",
    code: "LOAN_CREDIT",
    aliases: ["LOAN_CREDIT", "UNSECURED_LOAN_CONTRACT"],
    services: "loan",
    color: "blue",
    description: "Có placeholder, điền dữ liệu hồ sơ vay tự động.",
  },
  {
    key: "loan_mortgage",
    label: "Hợp đồng vay thế chấp",
    code: "LOAN_MORTGAGE",
    aliases: ["LOAN_MORTGAGE", "SECURED_LOAN_CONTRACT"],
    services: "loan",
    color: "amber",
    description: "Có placeholder bao gồm thông tin tài sản thế chấp.",
  },
] satisfies ReadonlyArray<{
  key: string;
  label: string;
  code: string;
  aliases: readonly string[];
  services: string;
  color: string;
  description: string;
}>;

type GroupKey = (typeof CONTRACT_GROUPS)[number]["key"];

const PLACEHOLDER_PRESETS: Array<{
  group: string;
  items: Array<{ fieldCode: string; fieldLabel: string; dataSource: string }>;
}> = [
  {
    group: "Khách hàng",
    items: [
      { fieldCode: "customer_name", fieldLabel: "Họ và tên", dataSource: "user.fullName" },
      { fieldCode: "customer_dob", fieldLabel: "Ngày sinh", dataSource: "user.dob" },
      { fieldCode: "customer_citizen_id", fieldLabel: "CCCD", dataSource: "user.citizenId" },
      { fieldCode: "customer_phone", fieldLabel: "Số điện thoại", dataSource: "user.phone" },
      { fieldCode: "customer_address", fieldLabel: "Địa chỉ", dataSource: "user.address" },
    ],
  },
  {
    group: "Sổ tiết kiệm",
    items: [
      { fieldCode: "saving_product", fieldLabel: "Sản phẩm tiết kiệm", dataSource: "saving.productName" },
      { fieldCode: "saving_amount", fieldLabel: "Số tiền gửi", dataSource: "saving.principalAmount" },
      { fieldCode: "saving_term_months", fieldLabel: "Kỳ hạn (tháng)", dataSource: "saving.termMonths" },
      { fieldCode: "saving_interest_rate", fieldLabel: "Lãi suất", dataSource: "saving.interestRate" },
      { fieldCode: "saving_open_date", fieldLabel: "Ngày mở sổ", dataSource: "saving.openDate" },
      { fieldCode: "saving_maturity_date", fieldLabel: "Ngày đáo hạn", dataSource: "saving.maturityDate" },
      { fieldCode: "saving_maturity_amount", fieldLabel: "Số tiền đáo hạn", dataSource: "saving.maturityAmount" },
      { fieldCode: "saving_auto_renew", fieldLabel: "Tự động tái tục", dataSource: "saving.autoRenew" },
    ],
  },
  {
    group: "Khoản vay",
    items: [
      { fieldCode: "loan_amount", fieldLabel: "Số tiền vay", dataSource: "loan.amount" },
      { fieldCode: "loan_term_months", fieldLabel: "Kỳ hạn (tháng)", dataSource: "loan.termMonths" },
      { fieldCode: "loan_interest_rate", fieldLabel: "Lãi suất", dataSource: "loan.interestRate" },
      { fieldCode: "loan_purpose", fieldLabel: "Mục đích vay", dataSource: "loan.purpose" },
      { fieldCode: "loan_product", fieldLabel: "Sản phẩm vay", dataSource: "loan.productName" },
      { fieldCode: "loan_monthly_payment", fieldLabel: "Trả hàng tháng", dataSource: "loan.monthlyPayment" },
    ],
  },
  {
    group: "Tài sản thế chấp",
    items: [
      { fieldCode: "collateral_desc", fieldLabel: "Mô tả tài sản", dataSource: "loan.collateralDescription" },
      { fieldCode: "collateral_value", fieldLabel: "Giá trị tài sản", dataSource: "loan.collateralValue" },
      { fieldCode: "collateral_address", fieldLabel: "Địa chỉ tài sản", dataSource: "loan.collateralAddress" },
      { fieldCode: "collateral_cert_no", fieldLabel: "Số GCN", dataSource: "loan.collateralCertNo" },
    ],
  },
  {
    group: "Hợp đồng & ngày",
    items: [
      { fieldCode: "contract_number", fieldLabel: "Số hợp đồng", dataSource: "contract.number" },
      { fieldCode: "contract_date", fieldLabel: "Ngày hợp đồng", dataSource: "contract.date" },
      { fieldCode: "sign_date", fieldLabel: "Ngày ký", dataSource: "contract.signedAt" },
      { fieldCode: "today", fieldLabel: "Ngày hiện tại", dataSource: "system.today" },
    ],
  },
];

const defaultPlaceholdersForGroup = (group?: (typeof CONTRACT_GROUPS)[number]): TemplatePlaceholder[] => {
  if (!group) return [];
  const allowedGroups = group.key === "saving"
    ? ["Khách hàng", "Sổ tiết kiệm", "Hợp đồng & ngày"]
    : group.key === "loan_credit"
      ? ["Khách hàng", "Khoản vay", "Hợp đồng & ngày"]
      : ["Khách hàng", "Khoản vay", "Tài sản thế chấp", "Hợp đồng & ngày"];

  return PLACEHOLDER_PRESETS
    .filter((preset) => allowedGroups.includes(preset.group))
    .flatMap((preset) => preset.items)
    .map((item, index) => ({ ...item, sortOrder: index + 1 }));
};

const normalizeTemplateCode = (code: string) => code.trim().toUpperCase();

// ─── Helpers ──────────────────────────────────────────────────────────────────

const fmt = (v?: string | null) => {
  if (!v) return "—";
  const d = new Date(v);
  return isNaN(d.getTime())
    ? v
    : new Intl.DateTimeFormat("vi-VN", { dateStyle: "short", timeStyle: "short" }).format(d);
};

const statusBadge = (status?: string | null) => {
  const s = (status ?? "").toLowerCase();
  if (s === "active")
    return <span className="rounded-full bg-emerald-100 px-2 py-0.5 text-[10px] font-semibold text-emerald-700">ACTIVE</span>;
  if (s === "archived")
    return <span className="rounded-full bg-zinc-100 px-2 py-0.5 text-[10px] font-semibold text-zinc-500">ARCHIVED</span>;
  return <span className="rounded-full bg-blue-50 px-2 py-0.5 text-[10px] font-semibold text-blue-600">DRAFT</span>;
};

type TemplateForm = {
  name: string;
  code: string;
  description: string;
  services: string;
  status: string;
  templateBody: string;
  placeholders: TemplatePlaceholder[];
};

const emptyForm = (group?: (typeof CONTRACT_GROUPS)[number]): TemplateForm => ({
  name: group ? `${group.label} v1.0` : "",
  code: group?.code ?? "",
  description: "",
  services: group?.services ?? "loan",
  status: "draft",
  templateBody: "",
  placeholders: defaultPlaceholdersForGroup(group),
});

const toForm = (d: TemplateDetail): TemplateForm => ({
  name: d.name ?? "",
  code: d.code ?? "",
  description: d.description ?? "",
  services: d.services ?? "loan",
  status: (d.status ?? "draft").toLowerCase(),
  templateBody: d.templateBody ?? "",
  placeholders: (d.placeholders ?? []).map((p) => ({ ...p })),
});

// ─── Component ────────────────────────────────────────────────────────────────

export default function ContractsPage() {
  const [templates, setTemplates] = useState<TemplateSummary[]>([]);
  const [loadingTemplates, setLoadingTemplates] = useState(true);
  const [selectedId, setSelectedId] = useState<number | null>(null);
  const [detail, setDetail] = useState<TemplateDetail | null>(null);
  const [form, setForm] = useState<TemplateForm>(emptyForm());
  const [isCreating, setIsCreating] = useState(false);
  const [saving, setSaving] = useState(false);
  const [activating, setActivating] = useState(false);
  const [uploading, setUploading] = useState(false);
  const [deleting, setDeleting] = useState(false);

  const [acceptances, setAcceptances] = useState<ContractAcceptanceSummary[]>([]);
  const [contracts, setContracts] = useState<GeneratedContract[]>([]);
  const [loadingDocs, setLoadingDocs] = useState(true);
  const [customerSearch, setCustomerSearch] = useState("");
  const [selectedCustomerKey, setSelectedCustomerKey] = useState("");

  const [error, setError] = useState("");
  const [toast, setToast] = useState("");
  const templateBodyRef = useRef<HTMLTextAreaElement>(null);

  // ── Load templates ──────────────────────────────────────────────────────────

  useEffect(() => {
    let alive = true;
    setLoadingTemplates(true);
    listContractTemplates()
      .then((data) => {
        if (!alive) return;
        setTemplates(data);
        if (!selectedId && data.length > 0) setSelectedId(data[0].id);
      })
      .catch((e) => alive && setError(e.message ?? "Không tải được template"))
      .finally(() => alive && setLoadingTemplates(false));
    return () => { alive = false; };
  }, []);

  // ── Load template detail ────────────────────────────────────────────────────

  useEffect(() => {
    if (!selectedId) return;
    let alive = true;
    getContractTemplate(selectedId)
      .then((d) => {
        if (!alive) return;
        setDetail(d);
        setForm(toForm(d));
        setIsCreating(false);
      })
      .catch((e) => alive && setError(e.message ?? "Không tải được chi tiết"));
    return () => { alive = false; };
  }, [selectedId]);

  // ── Load docs ───────────────────────────────────────────────────────────────

  useEffect(() => {
    let alive = true;
    setLoadingDocs(true);
    Promise.all([listContractAcceptances("all"), listGeneratedContracts()])
      .then(([acc, docs]) => {
        if (!alive) return;
        setAcceptances(acc);
        setContracts(docs);
      })
      .catch((e) => alive && setError(e.message ?? "Không tải được danh sách hợp đồng"))
      .finally(() => alive && setLoadingDocs(false));
    return () => { alive = false; };
  }, []);

  // ── Computed ────────────────────────────────────────────────────────────────

  const groupedTemplates = useMemo(() => {
    return CONTRACT_GROUPS.map((g) => ({
      ...g,
      templates: templates.filter(
        (t) => g.aliases.includes(normalizeTemplateCode(t.code))
      ),
      activeTemplate: templates.find(
        (t) =>
          g.aliases.includes(normalizeTemplateCode(t.code)) &&
          (t.status ?? "").toLowerCase() === "active"
      ) ?? null,
    }));
  }, [templates]);

  const contractMap = useMemo(() => {
    const m = new Map<string, GeneratedContract>();
    contracts.forEach((c) => c.contractNumber && m.set(c.contractNumber, c));
    return m;
  }, [contracts]);

  const customers = useMemo(() => {
    const map = new Map<string, { key: string; name: string; phone: string; count: number; last: string }>();
    acceptances.forEach((a) => {
      const key = a.userId != null ? `user:${a.userId}` : `phone:${a.userPhone ?? a.referenceId}`;
      const ex = map.get(key) ?? { key, name: a.userFullName ?? "Khách hàng", phone: a.userPhone ?? "", count: 0, last: "" };
      ex.count++;
      if (a.acceptedAt && a.acceptedAt > ex.last) ex.last = a.acceptedAt;
      map.set(key, ex);
    });
    const q = customerSearch.trim().toLowerCase();
    return Array.from(map.values())
      .filter((c) => !q || [c.name, c.phone, c.key].some((v) => v.toLowerCase().includes(q)))
      .sort((a, b) => b.last.localeCompare(a.last));
  }, [acceptances, customerSearch]);

  useEffect(() => {
    if (!selectedCustomerKey && customers.length > 0) setSelectedCustomerKey(customers[0].key);
  }, [customers]);

  const selectedAcceptances = useMemo(() => {
    if (!selectedCustomerKey) return [];
    if (selectedCustomerKey.startsWith("user:")) {
      const id = Number(selectedCustomerKey.replace("user:", ""));
      return acceptances.filter((a) => a.userId === id);
    }
    const phone = selectedCustomerKey.replace("phone:", "");
    return acceptances.filter((a) => (a.userPhone ?? "") === phone);
  }, [acceptances, selectedCustomerKey]);

  // ── Actions ─────────────────────────────────────────────────────────────────

  function showToast(msg: string) {
    setToast(msg);
    setTimeout(() => setToast(""), 3000);
  }

  async function reloadTemplates() {
    const data = await listContractTemplates();
    setTemplates(data);
  }

  async function handleActivate() {
    if (!detail) return;
    setActivating(true);
    setError("");
    try {
      const updated = await activateContractTemplate(detail.id);
      setDetail(updated);
      setForm(toForm(updated));
      await reloadTemplates();
      showToast("✓ Đã activate — bản cũ đã được archive tự động");
    } catch (e: any) {
      setError(e.message ?? "Không thể activate template");
    } finally {
      setActivating(false);
    }
  }

  async function handleArchive() {
    if (!detail) return;
    if (!confirm("Archive template này? Người dùng sẽ không thể ký hợp đồng loại này cho đến khi có bản active khác.")) return;
    setError("");
    try {
      const updated = await archiveContractTemplate(detail.id);
      setDetail(updated);
      setForm(toForm(updated));
      await reloadTemplates();
      showToast("Đã archive template");
    } catch (e: any) {
      setError(e.message ?? "Không thể archive template");
    }
  }

  async function handleSave() {
    setSaving(true);
    setError("");
    try {
      const payload = {
        name: form.name.trim(),
        code: form.code.trim(),
        description: form.description.trim() || null,
        services: form.services,
        status: form.status,
        templateBody: form.templateBody,
        templateFileUrl: detail?.templateFileUrl ?? null,
        placeholders: form.placeholders.map((p, i) => ({
              ...p,
              fieldCode: p.fieldCode.trim(),
              fieldLabel: p.fieldLabel?.trim() ?? null,
              dataSource: p.dataSource?.trim() ?? null,
              sortOrder: typeof p.sortOrder === "number" ? p.sortOrder : i + 1,
            })),
      };

      let updated: TemplateDetail;
      if (detail && !isCreating) {
        await updateContractTemplate(detail.id, payload);
        updated = await getContractTemplate(detail.id);
      } else {
        updated = await createContractTemplate(payload);
        setSelectedId(updated.id);
        setIsCreating(false);
      }
      setDetail(updated);
      setForm(toForm(updated));
      await reloadTemplates();
      showToast("✓ Đã lưu template");
    } catch (e: any) {
      setError(e.message ?? "Không thể lưu template");
    } finally {
      setSaving(false);
    }
  }

  async function handleDelete() {
    if (!detail) return;
    if ((detail.status ?? "").toLowerCase() === "active") {
      setError("Không thể xóa template đang active. Archive trước.");
      return;
    }
    if (!confirm(`Xóa template "${detail.name}"?`)) return;
    setDeleting(true);
    try {
      await deleteContractTemplate(detail.id);
      await reloadTemplates();
      setSelectedId(null);
      setDetail(null);
      setForm(emptyForm());
      showToast("Đã xóa template");
    } catch (e: any) {
      setError(e.message ?? "Không thể xóa template");
    } finally {
      setDeleting(false);
    }
  }

  async function handleUpload(e: React.FormEvent<HTMLFormElement>) {
    e.preventDefault();
    const f = e.currentTarget;
    const input = f.querySelector<HTMLInputElement>("input[type=file]");
    if (!input?.files?.[0]) return;
    setUploading(true);
    setError("");
    try {
      const fd = new FormData(f);
      const uploaded = await uploadContractTemplate({
        file: input.files[0],
        name: (fd.get("name") as string) || input.files[0].name,
        code: (fd.get("code") as string) || "TPL-" + Math.random().toString(36).slice(2, 8).toUpperCase(),
        description: (fd.get("description") as string) || "",
        services: (fd.get("services") as string) || "loan",
      });
      setDetail(uploaded);
      setForm(toForm(uploaded));
      setSelectedId(uploaded.id);
      setIsCreating(false);
      await reloadTemplates();
      f.reset();
      showToast("✓ Upload thành công");
    } catch (e: any) {
      setError(e.message ?? "Không thể upload template");
    } finally {
      setUploading(false);
    }
  }

  function handleAddPreset(item: { fieldCode: string; fieldLabel: string; dataSource: string }) {
    setForm((prev) => {
      const exists = prev.placeholders.some((p) => p.fieldCode === item.fieldCode);
      const nextPlaceholders = exists
        ? prev.placeholders
        : [...prev.placeholders, { fieldCode: item.fieldCode, fieldLabel: item.fieldLabel, dataSource: item.dataSource, sortOrder: prev.placeholders.length + 1 }];

      const token = `{{${item.fieldCode}}}`;
      const body = prev.templateBody ?? "";
      const textarea = templateBodyRef.current;
      if (!textarea) return { ...prev, templateBody: body ? `${body}\n${token}` : token, placeholders: nextPlaceholders };

      const start = textarea.selectionStart ?? body.length;
      const end = textarea.selectionEnd ?? start;
      const nextBody = `${body.slice(0, start)}${token}${body.slice(end)}`;
      queueMicrotask(() => { const pos = start + token.length; textarea.focus(); textarea.setSelectionRange(pos, pos); });

      return { ...prev, templateBody: nextBody, placeholders: nextPlaceholders };
    });
  }

  const isActive = (detail?.status ?? "").toLowerCase() === "active";
  const isDraft = (detail?.status ?? "").toLowerCase() === "draft";

  // ─── Render ────────────────────────────────────────────────────────────────

  return (
    <AdminShell title="Quản lý hợp đồng & thỏa thuận" subtitle="Template · Khách hàng đã ký · Tài liệu đã sinh">

      {toast && (
        <div className="fixed bottom-6 left-1/2 -translate-x-1/2 z-50 rounded-full bg-emerald-700 px-5 py-2 text-sm font-medium text-white shadow-lg">
          {toast}
        </div>
      )}

      {error && (
        <div className="mb-4 rounded-lg border border-rose-100 bg-rose-50 px-4 py-3 text-sm text-rose-700">
          {error}
          <button className="ml-3 text-rose-500 underline" onClick={() => setError("")}>Đóng</button>
        </div>
      )}

      {/* ── Tổng quan 3 nhóm ────────────────────────────────────────────────── */}
      <div className="grid grid-cols-1 gap-3 md:grid-cols-3">
        {CONTRACT_GROUPS.map((g) => {
          const group = groupedTemplates.find((x) => x.key === g.key)!;
          return (
            <div key={g.key} className="rounded-lg border border-zinc-100 bg-white p-4">
              <div className="flex items-center justify-between">
                <span className="text-sm font-semibold">{g.label}</span>
                {group.activeTemplate
                  ? <span className="rounded-full bg-emerald-100 px-2 py-0.5 text-[10px] font-semibold text-emerald-700">ACTIVE</span>
                  : <span className="rounded-full bg-zinc-100 px-2 py-0.5 text-[10px] text-zinc-500">Chưa active</span>
                }
              </div>
              <div className="mt-1 text-xs text-zinc-500">{g.description}</div>
              <div className="mt-2 text-xs text-zinc-500">{group.templates.length} phiên bản</div>
              {group.activeTemplate && (
                <div className="mt-1 text-xs font-medium text-emerald-700">{group.activeTemplate.name}</div>
              )}
              <button
                className="mt-3 w-full rounded-md bg-zinc-50 py-1.5 text-xs font-medium text-zinc-700 hover:bg-zinc-100"
                onClick={() => {
                  const first = group.templates[0];
                  if (first) setSelectedId(first.id);
                  else {
                    setIsCreating(true);
                    setSelectedId(null);
                    setDetail(null);
                    setForm(emptyForm(g));
                  }
                }}
              >
                {group.templates.length > 0 ? "Xem template" : "Tạo template"}
              </button>
            </div>
          );
        })}
      </div>

      {/* ── Grid chính: danh sách + editor ───────────────────────────────────── */}
      <div className="mt-6 grid grid-cols-1 gap-6 lg:grid-cols-[280px,1fr]">

        {/* Danh sách template */}
        <div className="rounded-lg bg-white p-4">
          <div className="flex items-center justify-between mb-3">
            <h3 className="text-sm font-bold">Phiên bản template</h3>
            <button
              className="rounded bg-blue-50 px-2 py-1 text-xs font-semibold text-blue-700"
              onClick={() => {
                setIsCreating(true);
                setSelectedId(null);
                setDetail(null);
                setForm(emptyForm());
              }}
            >
              + Tạo mới
            </button>
          </div>

          {CONTRACT_GROUPS.map((g) => {
            const group = groupedTemplates.find((x) => x.key === g.key)!;
            return (
              <div key={g.key} className="mb-4">
                <div className="text-[11px] font-semibold uppercase tracking-wide text-zinc-400 mb-1">{g.label}</div>
                {loadingTemplates ? (
                  <div className="text-xs text-zinc-400">Đang tải...</div>
                ) : group.templates.length === 0 ? (
                  <div className="text-xs text-zinc-400">Chưa có phiên bản nào</div>
                ) : (
                  group.templates.map((t) => (
                    <button
                      key={t.id}
                      onClick={() => setSelectedId(t.id)}
                      className={`mb-1 w-full rounded-md border px-3 py-2 text-left text-xs transition ${
                        selectedId === t.id ? "border-blue-200 bg-blue-50" : "border-transparent hover:bg-zinc-50"
                      }`}
                    >
                      <div className="flex items-center justify-between gap-2">
                        <span className="font-medium text-zinc-900 truncate">{t.name}</span>
                        {statusBadge(t.status)}
                      </div>
                      <div className="text-zinc-400 mt-0.5">Sửa {fmt(t.updatedAt)}</div>
                    </button>
                  ))
                )}
              </div>
            );
          })}
        </div>

        {/* Editor */}
        <div className="rounded-lg bg-white p-4">

          {/* Header editor */}
          <div className="flex flex-wrap items-center justify-between gap-2 mb-4">
            <h3 className="text-sm font-bold">
              {isCreating ? "Tạo template mới" : detail ? `Chỉnh sửa: ${detail.name}` : "Chọn template"}
            </h3>
            <div className="flex flex-wrap gap-2">
              {/* Activate */}
              {detail && !isCreating && !isActive && (
                <button
                  onClick={handleActivate}
                  disabled={activating}
                  className="rounded bg-emerald-600 px-3 py-1.5 text-xs font-semibold text-white disabled:opacity-50"
                >
                  {activating ? "Đang activate..." : "🚀 Activate"}
                </button>
              )}
              {/* Archive */}
              {detail && !isCreating && isActive && (
                <button
                  onClick={handleArchive}
                  className="rounded border border-zinc-200 px-3 py-1.5 text-xs font-semibold text-zinc-500 hover:bg-zinc-50"
                >
                  Archive
                </button>
              )}
              {/* Delete */}
              {detail && !isCreating && !isActive && (
                <button
                  onClick={handleDelete}
                  disabled={deleting}
                  className="rounded border border-rose-200 px-3 py-1.5 text-xs font-semibold text-rose-600 hover:bg-rose-50 disabled:opacity-50"
                >
                  {deleting ? "Đang xóa..." : "Xóa"}
                </button>
              )}
              {/* Save */}
              <button
                onClick={handleSave}
                disabled={saving}
                className="rounded bg-blue-600 px-3 py-1.5 text-xs font-semibold text-white disabled:opacity-50"
              >
                {saving ? "Đang lưu..." : detail && !isCreating ? "Lưu thay đổi" : "Tạo template"}
              </button>
            </div>
          </div>

          {isActive && (
            <div className="mb-4 rounded-lg border border-emerald-100 bg-emerald-50 px-3 py-2 text-xs text-emerald-700">
              ✓ Template này đang ACTIVE và được dùng trên mobile. Để chỉnh sửa, hãy tạo phiên bản mới rồi activate.
            </div>
          )}

          {/* Fields */}
          <div className="grid grid-cols-1 gap-3 md:grid-cols-2">
            <input value={form.name} onChange={(e) => setForm((p) => ({ ...p, name: e.target.value }))}
              placeholder="Tên template (VD: Thỏa thuận tiết kiệm v2.0)"
              className="rounded border p-2 text-sm" />
            <input value={form.code} onChange={(e) => setForm((p) => ({ ...p, code: e.target.value }))}
              placeholder="Mã code (VD: SAVING_AGREEMENT)"
              className="rounded border p-2 text-sm font-mono" />
            <input value={form.description} onChange={(e) => setForm((p) => ({ ...p, description: e.target.value }))}
              placeholder="Mô tả ngắn"
              className="rounded border p-2 text-sm" />
            <div className="flex gap-2">
              <select value={form.services} onChange={(e) => setForm((p) => ({ ...p, services: e.target.value }))}
                className="w-full rounded border p-2 text-sm">
                <option value="saving">Tiết kiệm</option>
                <option value="loan">Vay vốn</option>
                <option value="general">Khác</option>
              </select>
              <select value={form.status} onChange={(e) => setForm((p) => ({ ...p, status: e.target.value }))}
                className="w-full rounded border p-2 text-sm">
                <option value="draft">DRAFT</option>
                <option value="active">ACTIVE</option>
                <option value="archived">ARCHIVED</option>
              </select>
            </div>
          </div>

          {/* Template body */}
          <div className="mt-4">
            <div className="text-xs font-semibold text-zinc-500 mb-1">Nội dung template</div>
            <textarea
              ref={templateBodyRef}
              value={form.templateBody}
              onChange={(e) => setForm((p) => ({ ...p, templateBody: e.target.value }))}
              rows={12}
              className="w-full rounded border p-3 text-sm font-mono"
              placeholder="Nhập nội dung hợp đồng/thỏa thuận. Dùng {{fieldCode}} cho các trường dữ liệu động..."
              readOnly={isActive}
            />
          </div>

          {/* Placeholders */}
          <div className="mt-4">
              <div className="flex items-center justify-between mb-2">
                <div className="text-xs font-semibold text-zinc-500">Placeholder</div>
                <button onClick={() => setForm((p) => ({
                  ...p,
                  placeholders: [...p.placeholders, { fieldCode: "", fieldLabel: "", dataSource: "", sortOrder: p.placeholders.length + 1 }],
                }))} className="rounded bg-zinc-100 px-2 py-1 text-xs font-semibold text-zinc-700">
                  + Thêm
                </button>
              </div>

              {/* Presets */}
              <div className="mb-3 rounded border border-zinc-100 bg-zinc-50 p-3">
                <div className="text-xs font-semibold text-zinc-500 mb-2">Chèn nhanh</div>
                <div className="grid grid-cols-1 gap-2 md:grid-cols-2">
                  {PLACEHOLDER_PRESETS.map((g) => (
                    <div key={g.group} className="rounded border border-zinc-100 bg-white p-2">
                      <div className="text-[11px] font-semibold text-zinc-500 mb-1">{g.group}</div>
                      <div className="flex flex-wrap gap-1">
                        {g.items.map((item) => (
                          <button key={item.fieldCode}
                            onClick={() => handleAddPreset(item)}
                            className="rounded-full border border-zinc-200 px-2 py-0.5 text-[11px] text-zinc-700 hover:bg-zinc-100">
                            {item.fieldLabel}
                          </button>
                        ))}
                      </div>
                    </div>
                  ))}
                </div>
              </div>

              {/* Danh sách placeholder */}
              {form.placeholders.length === 0
                ? <div className="text-xs text-zinc-400">Chưa có placeholder.</div>
                : form.placeholders.map((p, i) => (
                  <div key={i} className="mb-2 grid grid-cols-1 gap-2 rounded border border-zinc-100 p-2 md:grid-cols-5">
                    <input value={p.fieldCode} onChange={(e) => setForm((prev) => {
                      const pl = [...prev.placeholders]; pl[i] = { ...pl[i], fieldCode: e.target.value }; return { ...prev, placeholders: pl };
                    })} placeholder="field_code" className="rounded border p-1.5 text-xs font-mono" />
                    <input value={p.fieldLabel ?? ""} onChange={(e) => setForm((prev) => {
                      const pl = [...prev.placeholders]; pl[i] = { ...pl[i], fieldLabel: e.target.value }; return { ...prev, placeholders: pl };
                    })} placeholder="Nhãn hiển thị" className="rounded border p-1.5 text-xs" />
                    <input value={p.dataSource ?? ""} onChange={(e) => setForm((prev) => {
                      const pl = [...prev.placeholders]; pl[i] = { ...pl[i], dataSource: e.target.value }; return { ...prev, placeholders: pl };
                    })} placeholder="Nguồn dữ liệu" className="rounded border p-1.5 text-xs" />
                    <input type="number" value={p.sortOrder ?? i + 1} onChange={(e) => setForm((prev) => {
                      const pl = [...prev.placeholders]; pl[i] = { ...pl[i], sortOrder: Number(e.target.value) }; return { ...prev, placeholders: pl };
                    })} placeholder="Thứ tự" className="rounded border p-1.5 text-xs" />
                    <button onClick={() => setForm((prev) => ({ ...prev, placeholders: prev.placeholders.filter((_, idx) => idx !== i) }))}
                      className="rounded bg-rose-50 px-2 py-1 text-xs font-semibold text-rose-600">Xóa</button>
                  </div>
                ))
              }
            </div>

          {/* Upload DOCX */}
          <div className="mt-6 rounded border border-dashed border-zinc-200 p-4">
              <div className="text-xs font-semibold text-zinc-600 mb-3">Upload template .docx</div>
              <form onSubmit={handleUpload} className="grid grid-cols-1 gap-2 md:grid-cols-4">
                <input name="name" placeholder="Tên" className="rounded border p-2 text-sm" />
                <input name="code" placeholder="Mã code" className="rounded border p-2 text-sm font-mono" />
                <input name="description" placeholder="Mô tả" className="rounded border p-2 text-sm" />
                <select name="services" className="rounded border p-2 text-sm">
                  <option value="loan">Vay vốn</option>
                  <option value="saving">Tiết kiệm</option>
                </select>
                <input type="file" accept=".docx" className="col-span-1 md:col-span-3 text-sm" />
                <button className="rounded bg-blue-600 px-3 py-2 text-sm font-semibold text-white" disabled={uploading}>
                  {uploading ? "Đang tải..." : "Upload"}
                </button>
              </form>
          </div>
        </div>
      </div>

      {/* ── Danh sách khách hàng đã ký ──────────────────────────────────────── */}
      <div className="mt-6 grid grid-cols-1 gap-6 lg:grid-cols-[280px,1fr]">
        <div className="rounded-lg bg-white p-4">
          <div className="flex items-center justify-between mb-2">
            <h3 className="text-sm font-bold">Khách hàng đã ký</h3>
            <span className="text-xs text-zinc-400">{customers.length} KH</span>
          </div>
          <input value={customerSearch} onChange={(e) => setCustomerSearch(e.target.value)}
            placeholder="Tìm theo tên, SĐT" className="mb-3 w-full rounded border p-2 text-sm" />
          <div className="max-h-[400px] overflow-y-auto flex flex-col gap-1">
            {loadingDocs ? <div className="text-xs text-zinc-400">Đang tải...</div>
              : customers.length === 0 ? <div className="text-xs text-zinc-400">Chưa có dữ liệu.</div>
              : customers.map((c) => (
                <button key={c.key} onClick={() => setSelectedCustomerKey(c.key)}
                  className={`rounded-lg border px-3 py-2 text-left text-sm transition ${
                    selectedCustomerKey === c.key ? "border-blue-200 bg-blue-50" : "border-transparent hover:bg-zinc-50"
                  }`}>
                  <div className="font-semibold text-zinc-900">{c.name}</div>
                  <div className="flex items-center justify-between">
                    <span className="text-xs text-zinc-500">{c.phone}</span>
                    <span className="text-xs text-zinc-400">{c.count} hợp đồng</span>
                  </div>
                </button>
              ))}
          </div>
        </div>

        <div className="rounded-lg bg-white p-4">
          <h3 className="text-sm font-bold mb-1">Hợp đồng của khách hàng</h3>
          <div className="text-xs text-zinc-400 mb-4">Chọn khách hàng bên trái để xem và tải file hợp đồng.</div>
          <div className="flex flex-col gap-3">
            {loadingDocs ? <div className="text-xs text-zinc-400">Đang tải...</div>
              : selectedAcceptances.length === 0 ? <div className="text-xs text-zinc-400">Chưa có hợp đồng.</div>
              : selectedAcceptances.map((a) => {
                const contract = a.contractNumber ? contractMap.get(a.contractNumber) : undefined;
                return (
                  <div key={`${a.referenceType}-${a.referenceId}-${a.contractNumber}`}
                    className="rounded-lg border border-zinc-100 p-3">
                    <div className="flex flex-wrap items-center justify-between gap-2">
                      <div>
                        <div className="text-sm font-semibold">{a.contractNumber ?? "Chưa có số"}</div>
                        <div className="text-xs text-zinc-500">{a.templateName ?? a.templateCode ?? "—"}</div>
                      </div>
                      <span className="text-xs text-zinc-500">{a.acceptanceStatus ?? "—"}</span>
                    </div>
                    <div className="mt-1 text-xs text-zinc-400">Ký: {fmt(a.acceptedAt)}</div>
                    <div className="mt-2">
                      {contract?.fileUrl
                        ? <a href={contract.fileUrl} target="_blank" rel="noreferrer"
                            className="inline-flex rounded bg-blue-600 px-3 py-1.5 text-xs font-medium text-white">
                            Tải file
                          </a>
                        : <span className="text-xs text-zinc-400">Chưa có file</span>
                      }
                    </div>
                  </div>
                );
              })}
          </div>
        </div>
      </div>
    </AdminShell>
  );
}
