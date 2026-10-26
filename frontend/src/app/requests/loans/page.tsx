"use client";

import { useEffect, useMemo, useState } from "react";
import { Check, ChevronLeft, FileText, Search, X } from "lucide-react";

import AdminShell from "../../components/admin-shell";
import {
  approveLoanApplication,
  getLoanApproval,
  listLoanApprovals,
  rejectLoanApplication,
  type ApprovalProgress,
  type LoanApprovalSummary,
} from "../../../lib/api/admin-approvals";

const STATUS_FILTERS = [
  { label: "Tất cả", value: "pending,approved,rejected" },
  { label: "Chờ duyệt", value: "pending" },
  { label: "Đã duyệt", value: "approved" },
  { label: "Từ chối", value: "rejected" },
];

const LOAN_CHECKLIST = [
  "Xác nhận thông tin khách hàng khớp hồ sơ",
  "Kiểm tra chứng từ thu nhập và CCCD",
  "Kiểm tra tài sản thế chấp nếu có",
  "Đối chiếu hạn mức, kỳ hạn và mục đích vay",
  "Xác nhận bước duyệt này đúng cấp theo policy",
];

function formatCurrency(value: string | number | null | undefined) {
  if (value == null || value === "") return "—";
  const n = typeof value === "number" ? value : Number(value);
  if (Number.isNaN(n)) return String(value);
  return new Intl.NumberFormat("vi-VN", { style: "currency", currency: "VND" }).format(n);
}

function formatDateTime(value: string | null | undefined) {
  if (!value) return "—";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return value;
  return new Intl.DateTimeFormat("vi-VN", { dateStyle: "short", timeStyle: "short" }).format(date);
}

function statusLabel(status: string) {
  const normalized = status.toLowerCase();
  if (normalized === "pending") return "Chờ duyệt";
  if (normalized === "approved") return "Đã duyệt";
  if (normalized === "rejected") return "Từ chối";
  return status;
}

function statusClass(status: string) {
  const normalized = status.toLowerCase();
  if (normalized === "pending") return "bg-yellow-100 text-yellow-700";
  if (normalized === "approved") return "bg-green-100 text-green-700";
  if (normalized === "rejected") return "bg-red-100 text-red-700";
  return "bg-slate-100 text-slate-700";
}

function isImageUrl(url: string | null | undefined) {
  if (!url) return false;
  return /\.(png|jpe?g|webp|gif|bmp|avif)(\?.*)?$/i.test(url);
}

function DocumentPreview({ label, url }: { label: string; url: string | null | undefined }) {
  if (!url) {
    return <Field label={label} value="—" />;
  }

  if (isImageUrl(url)) {
    return (
      <div>
        <div className="text-xs text-neutral-500">{label}</div>
        <a href={url} target="_blank" rel="noreferrer" className="mt-2 block overflow-hidden rounded-2xl border border-neutral-200 bg-neutral-50">
          <img src={url} alt={label} className="h-44 w-full object-cover" />
        </a>
        <a className="mt-2 inline-flex text-xs font-medium text-blue-600 hover:underline" href={url} target="_blank" rel="noreferrer">
          Mở ảnh gốc
        </a>
      </div>
    );
  }

  return <Field label={label} value={<a className="text-blue-600 hover:underline" href={url} target="_blank" rel="noreferrer">Mở file</a>} />;
}

function ToastBanner({ text, tone }: { text: string; tone: "success" | "error" }) {
  return (
    <div className="fixed right-6 top-6 z-50 rounded-xl border bg-white px-4 py-3 text-sm shadow-lg">
      <div className={tone === "success" ? "text-green-700" : "text-red-700"}>{text}</div>
    </div>
  );
}

function Section({ title, children }: { title: string; children: React.ReactNode }) {
  return (
    <section className="rounded-2xl border border-neutral-200 bg-white p-5">
      <h3 className="mb-4 border-b border-neutral-100 pb-2 text-sm font-semibold text-neutral-800">{title}</h3>
      {children}
    </section>
  );
}

function Field({ label, value }: { label: string; value: React.ReactNode }) {
  return (
    <div>
      <div className="text-xs text-neutral-500">{label}</div>
      <div className="text-sm text-neutral-900">{value}</div>
    </div>
  );
}

function ApprovalProgressPanel({ progress }: { progress?: ApprovalProgress | null }) {
  if (!progress) {
    return (
      <Section title="Tiến độ duyệt">
        <div className="text-sm text-neutral-500">
          Hệ thống sẽ tự áp policy theo mức tiền. Tiến độ duyệt sẽ xuất hiện sau khi hồ sơ được khởi tạo.
        </div>
      </Section>
    );
  }

  return (
    <Section title="Tiến độ duyệt">
      <div className="space-y-4 text-sm">
        <div className="grid grid-cols-2 gap-3">
          <div className="rounded-xl bg-blue-50 p-3">
            <div className="text-xs text-blue-600">Nhân viên nghiệp vụ</div>
            <div className="text-lg font-semibold text-blue-700">
              {progress.staffApprovedCount}/{progress.staffApprovalsRequired}
            </div>
          </div>
          <div className="rounded-xl bg-purple-50 p-3">
            <div className="text-xs text-purple-600">Quản lý / Super Admin</div>
            <div className="text-lg font-semibold text-purple-700">
              {progress.managerApprovedCount}/{progress.managerApprovalsRequired}
            </div>
          </div>
        </div>
        <div className="text-xs text-neutral-500">
          Bước hiện tại: <span className="font-medium text-neutral-800">{progress.currentStage === "MANAGER" ? "Quản lý / Super Admin" : "Nhân viên nghiệp vụ"}</span>
        </div>
        <div className="rounded-xl border border-amber-200 bg-amber-50 p-3 text-xs text-amber-800">
          Cùng cấp duyệt: bất kỳ nhân viên đủ role nào cũng có thể xử lý trước. Mỗi bước phải là một tài khoản khác; Super Admin chỉ tính ở tầng quản lý.
        </div>
        <div className="space-y-2">
          {progress.actions.length ? progress.actions.map((action) => (
            <div key={action.id} className="rounded-xl border border-neutral-100 bg-neutral-50 p-3">
              <div className="font-medium text-neutral-900">{action.adminFullName ?? `Admin #${action.adminUserId}`}</div>
              <div className="text-xs text-neutral-500">{action.approverRole} · {action.action} · {formatDateTime(action.actedAt)}</div>
              {action.note ? <div className="mt-1 text-xs text-neutral-600">{action.note}</div> : null}
            </div>
          )) : <div className="text-xs text-neutral-500">Chưa có bước duyệt nào.</div>}
        </div>
      </div>
    </Section>
  );
}

export default function LoanApprovalPage() {
  const [items, setItems] = useState<LoanApprovalSummary[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [search, setSearch] = useState("");
  const [statusFilter, setStatusFilter] = useState(STATUS_FILTERS[0].value);
  const [selectedId, setSelectedId] = useState<number | null>(null);
  const [detail, setDetail] = useState<LoanApprovalSummary | null>(null);
  const [detailLoading, setDetailLoading] = useState(false);
  const [rejectReason, setRejectReason] = useState("");
  const [approvalNote, setApprovalNote] = useState("");
  const [checked, setChecked] = useState<boolean[]>(() => new Array(LOAN_CHECKLIST.length).fill(false));
  const [toast, setToast] = useState<{ tone: "success" | "error"; text: string } | null>(null);

  const filteredItems = useMemo(() => {
    const q = search.trim().toLowerCase();
    return items.filter((item) => {
      const matchesSearch =
        !q ||
        item.userFullName?.toLowerCase().includes(q) ||
        item.userPhone?.toLowerCase().includes(q) ||
        item.userEmail?.toLowerCase().includes(q) ||
        String(item.id).includes(q);
      return matchesSearch;
    });
  }, [items, search]);

  useEffect(() => {
    let alive = true;

    async function loadList() {
      setLoading(true);
      setError("");
      try {
        const data = await listLoanApprovals(statusFilter);
        if (!alive) return;
        setItems(data);
        setSelectedId((current) => current ?? data[0]?.id ?? null);
      } catch (err) {
        if (!alive) return;
        setError(err instanceof Error ? err.message : "Không thể tải dữ liệu");
      } finally {
        if (alive) setLoading(false);
      }
    }

    loadList();
    return () => {
      alive = false;
    };
  }, [statusFilter]);

  useEffect(() => {
    if (!selectedId) {
      setDetail(null);
      return;
    }

    let alive = true;
    setDetailLoading(true);
    setDetail(null);
    setApprovalNote("");
    setChecked(new Array(LOAN_CHECKLIST.length).fill(false));

    getLoanApproval(selectedId)
      .then((data) => {
        if (alive) setDetail(data);
      })
      .catch((err) => {
        if (alive) setError(err instanceof Error ? err.message : "Không thể tải chi tiết");
      })
      .finally(() => {
        if (alive) setDetailLoading(false);
      });

    return () => {
      alive = false;
    };
  }, [selectedId]);

  const selectedItem = filteredItems.find((item) => item.id === selectedId) ?? null;
  const pendingCount = items.filter((item) => item.status.toLowerCase() === "pending").length;
  const approvedCount = items.filter((item) => item.status.toLowerCase() === "approved").length;
  const checklistDone = checked.every(Boolean);
  const canApprove = Boolean(selectedId && detail?.status.toLowerCase() === "pending" && checklistDone);

  async function refresh() {
    const data = await listLoanApprovals(statusFilter);
    setItems(data);
    setSelectedId((current) => current ?? data[0]?.id ?? null);
  }

  async function onApprove() {
    if (!selectedId) return;
    if (!checklistDone) {
      setToast({ tone: "error", text: "Vui lòng xác nhận đủ checklist trước khi duyệt" });
      return;
    }
    if (!window.confirm("Xác nhận ghi nhận bước duyệt? Người duyệt sau phải là tài khoản khác.")) return;
    try {
      const note = [
        approvalNote.trim(),
        `Checklist: ${LOAN_CHECKLIST.join(" | ")}`,
      ].filter(Boolean).join("\n");
      await approveLoanApplication(selectedId, note);
      setToast({ tone: "success", text: "Đã ghi nhận bước duyệt khoản vay" });
      setApprovalNote("");
      setChecked(new Array(LOAN_CHECKLIST.length).fill(false));
      await refresh();
      setDetail(await getLoanApproval(selectedId));
    } catch (err) {
      setToast({ tone: "error", text: err instanceof Error ? err.message : "Không thể phê duyệt" });
    }
  }

  async function onReject() {
    if (!selectedId) return;
    if (!rejectReason.trim()) {
      setToast({ tone: "error", text: "Vui lòng nhập lý do từ chối" });
      return;
    }
    try {
      await rejectLoanApplication(selectedId, rejectReason.trim());
      setToast({ tone: "success", text: "Đã từ chối khoản vay" });
      setRejectReason("");
      await refresh();
      setDetail(await getLoanApproval(selectedId));
    } catch (err) {
      setToast({ tone: "error", text: err instanceof Error ? err.message : "Không thể từ chối" });
    }
  }

  return (
    <AdminShell title="Duyệt vay vốn" subtitle="Lấy dữ liệu trực tiếp từ database">
      {toast ? <ToastBanner tone={toast.tone} text={toast.text} /> : null}
      <div className="flex h-full gap-4">
        <aside className="flex w-[390px] min-w-[320px] flex-col rounded-2xl border border-neutral-200 bg-neutral-50">
          <div className="border-b border-neutral-200 bg-white p-5">
            <div className="mb-4 flex items-center justify-between">
              <div>
                <h1 className="text-xl font-semibold text-neutral-900">Duyệt vay vốn</h1>
                <p className="text-sm text-neutral-500">Danh sách hồ sơ vay đang được xử lý</p>
              </div>
              <div className="flex gap-2">
                <div className="rounded-xl border border-yellow-200 bg-yellow-50 px-3 py-2 text-center">
                  <div className="text-lg font-bold text-yellow-700">{pendingCount}</div>
                  <div className="text-xs text-yellow-600">Chờ duyệt</div>
                </div>
                <div className="rounded-xl border border-green-200 bg-green-50 px-3 py-2 text-center">
                  <div className="text-lg font-bold text-green-700">{approvedCount}</div>
                  <div className="text-xs text-green-600">Đã duyệt</div>
                </div>
              </div>
            </div>

            <div className="mb-3 flex gap-2">
              <div className="relative flex-1">
                <Search size={16} className="absolute left-3 top-1/2 -translate-y-1/2 text-neutral-400" />
                <input
                  className="w-full rounded-xl border border-neutral-200 bg-white py-2 pl-9 pr-3 text-sm outline-none focus:ring-2 focus:ring-blue-500"
                  value={search}
                  onChange={(e) => setSearch(e.target.value)}
                  placeholder="Tìm tên KH, số ĐT, email, mã đơn"
                />
              </div>
              <select
                className="rounded-xl border border-neutral-200 bg-white px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-blue-500"
                value={statusFilter}
                onChange={(e) => setStatusFilter(e.target.value)}
              >
                {STATUS_FILTERS.map((option) => (
                  <option key={option.value} value={option.value}>{option.label}</option>
                ))}
              </select>
            </div>
          </div>

          <div className="flex-1 space-y-3 overflow-y-auto p-4">
            {loading ? <div className="py-10 text-center text-sm text-neutral-400">Đang tải dữ liệu...</div> : null}
            {error ? <div className="rounded-xl border border-red-200 bg-red-50 p-4 text-sm text-red-700">{error}</div> : null}
            {!loading && !error && filteredItems.map((item) => (
              <button
                key={item.id}
                onClick={() => setSelectedId(item.id)}
                className={`w-full rounded-2xl border p-4 text-left transition ${selectedId === item.id ? "border-blue-400 bg-white shadow-md ring-1 ring-blue-300" : "border-neutral-200 bg-white hover:shadow-sm"}`}
              >
                <div className="mb-2 flex items-start justify-between gap-2">
                  <div>
                    <div className="text-sm font-semibold text-neutral-900">{item.userFullName ?? "Chưa có tên"}</div>
                    <div className="text-xs text-neutral-500">#{item.id}</div>
                  </div>
                  <span className={`rounded-full px-2 py-1 text-xs font-medium ${statusClass(item.status)}`}>{statusLabel(item.status)}</span>
                </div>
                <div className="flex items-end justify-between gap-2">
                  <div>
                    <div className="text-sm font-semibold text-blue-700">{formatCurrency(item.requestedAmount)}</div>
                    <div className="text-xs text-neutral-500">{item.productName ?? "Khoản vay"}</div>
                  </div>
                  <div className="text-right text-xs text-neutral-500">
                    <div>{item.termMonths} tháng</div>
                    <div>{formatDateTime(item.submittedAt)}</div>
                  </div>
                </div>
              </button>
            ))}
          </div>
        </aside>

        <main className="flex-1 overflow-hidden rounded-2xl border border-neutral-200 bg-white">
          {!selectedItem ? (
            <div className="flex h-full items-center justify-center text-sm text-neutral-400">Chọn một hồ sơ vay để xem chi tiết</div>
          ) : detailLoading || !detail ? (
            <div className="flex h-full items-center justify-center text-sm text-neutral-400">Đang tải chi tiết...</div>
          ) : (
            <div className="flex h-full flex-col">
              <div className="flex items-center justify-between border-b border-neutral-200 px-6 py-4">
                <div className="flex items-center gap-3">
                  <button onClick={() => setSelectedId(null)} className="rounded-lg p-1.5 hover:bg-neutral-100">
                    <ChevronLeft size={18} className="text-neutral-600" />
                  </button>
                  <div>
                    <div className="flex items-center gap-2">
                      <h2 className="text-lg font-semibold text-neutral-900">{detail.userFullName ?? "Hồ sơ vay"}</h2>
                      <span className={`rounded-full px-2 py-1 text-xs font-medium ${statusClass(detail.status)}`}>{statusLabel(detail.status)}</span>
                    </div>
                    <p className="text-sm text-neutral-500">{detail.productName ?? "Khoản vay"} · {formatDateTime(detail.submittedAt)}</p>
                  </div>
                </div>

                <div className="flex gap-2">
                  <button onClick={onReject} className="inline-flex items-center gap-2 rounded-xl border border-red-300 px-4 py-2 text-sm font-medium text-red-600 hover:bg-red-50">
                    <X size={16} /> Từ chối
                  </button>
                  <button
                    onClick={onApprove}
                    disabled={!canApprove}
                    className="inline-flex items-center gap-2 rounded-xl bg-green-600 px-4 py-2 text-sm font-medium text-white hover:bg-green-700 disabled:cursor-not-allowed disabled:bg-neutral-300 disabled:text-neutral-500"
                  >
                    <Check size={16} /> Phê duyệt
                  </button>
                </div>
              </div>

              <div className="flex-1 overflow-y-auto p-6">
                <div className="grid gap-6 xl:grid-cols-[minmax(0,1fr)_320px]">
                  <div className="space-y-6">
                    <Section title="Thông tin cá nhân từ DB">
                      <div className="grid grid-cols-2 gap-x-6 gap-y-3">
                        <Field label="Họ và tên" value={detail.userFullName ?? "—"} />
                        <Field label="Mã khách hàng" value={detail.userId ?? "—"} />
                        <Field label="Số điện thoại" value={detail.userPhone ?? "—"} />
                        <Field label="Email" value={detail.userEmail ?? "—"} />
                        <Field label="Ngày sinh" value={detail.userDob ?? "—"} />
                        <Field label="Địa chỉ" value={detail.userAddress ?? "—"} />
                        <Field label="CCCD/CMND" value={detail.userCitizenId ?? "—"} />
                        <Field label="Hạng KH / tín dụng" value={`${detail.customerRank ?? "—"}${detail.creditScoreLevel ? ` · ${detail.creditScoreLevel}` : ""}`} />
                      </div>
                    </Section>

                    <Section title="Thông tin khoản vay">
                      <div className="grid grid-cols-2 gap-x-6 gap-y-3">
                        <Field label="Mã hồ sơ" value={`#${detail.id}`} />
                        <Field label="Loại vay" value={detail.loanType ?? "—"} />
                        <Field label="Sản phẩm" value={detail.productName ?? "—"} />
                        <Field label="Mã sản phẩm" value={detail.productCode ?? "—"} />
                        <Field label="Số tiền vay" value={formatCurrency(detail.requestedAmount)} />
                        <Field label="Kỳ hạn" value={`${detail.termMonths} tháng`} />
                        <Field label="Thu nhập hàng tháng" value={formatCurrency(detail.monthlyIncome)} />
                        <Field label="Mục đích vay" value={detail.purpose ?? "—"} />
                        <Field label="Phân loại ưu tiên" value={detail.priorityTag ?? "—"} />
                        <Field label="Trạng thái xử lý" value={statusLabel(detail.status)} />
                        <Field label="Ngày gửi" value={formatDateTime(detail.submittedAt)} />
                        <Field label="Ngày xử lý" value={formatDateTime(detail.reviewedAt)} />
                      </div>
                    </Section>

                    <Section title="Thông tin thế chấp / chứng từ">
                      <div className="grid grid-cols-2 gap-x-6 gap-y-3">
                        <Field label="Có thế chấp" value={detail.hasCollateral ? "Có" : "Không"} />
                        <Field label="Mô tả tài sản" value={detail.collateralDescription ?? "—"} />
                        <div className="col-span-2 grid gap-4 md:grid-cols-2">
                          <DocumentPreview label="Ảnh chứng từ thu nhập" url={detail.incomeProofUrl} />
                          <DocumentPreview label="Ảnh chứng từ thế chấp" url={detail.collateralProofUrl} />
                          <DocumentPreview label="CCCD mặt trước" url={detail.cccdFrontUrl} />
                          <DocumentPreview label="CCCD mặt sau" url={detail.cccdBackUrl} />
                          <DocumentPreview label="Selfie / xác thực" url={detail.selfieUrl} />
                        </div>
                        <Field label="Ghi chú xử lý" value={detail.reviewNote ?? "—"} />
                      </div>
                    </Section>
                  </div>

                  <div className="space-y-6">
                    <ApprovalProgressPanel progress={detail.approvalProgress} />

                    <Section title="Phân loại tín dụng từ DB">
                      <div className="space-y-3 text-sm">
                        <Field label="Hạng khách hàng" value={detail.customerRank ?? "—"} />
                        <Field label="Mức tín dụng" value={detail.creditScoreLevel ?? "—"} />
                        <Field label="Trạng thái hợp đồng" value={detail.contractStatus ?? "—"} />
                        <Field label="Số hợp đồng" value={detail.contractNumber ?? "—"} />
                      </div>
                    </Section>

                    <Section title="Checklist nghiệp vụ">
                      <div className="space-y-2">
                        {LOAN_CHECKLIST.map((item, index) => (
                          <button
                            key={item}
                            onClick={() => setChecked((prev) => prev.map((value, i) => (i === index ? !value : value)))}
                            className="flex w-full items-start gap-2 rounded-xl p-2 text-left hover:bg-neutral-50"
                          >
                            <Check size={16} className={checked[index] ? "mt-0.5 text-green-600" : "mt-0.5 text-neutral-400"} />
                            <span className={`text-xs ${checked[index] ? "text-green-700 line-through" : "text-neutral-700"}`}>{item}</span>
                          </button>
                        ))}
                      </div>
                      <div className="mt-3 rounded-xl border border-blue-100 bg-blue-50 p-3 text-xs text-blue-700">
                        Cùng cấp duyệt: bất kỳ nhân viên đủ role nào cũng có thể xử lý trước. Người đã duyệt ở bước trước không được duyệt lại bước sau.
                      </div>
                      <div className="mt-4">
                        <textarea
                          className="min-h-28 w-full rounded-xl border border-neutral-200 px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-blue-500"
                          placeholder="Ghi chú duyệt / thẩm định"
                          value={approvalNote}
                          onChange={(e) => setApprovalNote(e.target.value)}
                        />
                      </div>
                      <div className="mt-4">
                        <textarea
                          className="min-h-24 w-full rounded-xl border border-neutral-200 px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-red-500"
                          placeholder="Nhập lý do từ chối nếu cần"
                          value={rejectReason}
                          onChange={(e) => setRejectReason(e.target.value)}
                        />
                      </div>
                    </Section>
                  </div>
                </div>
              </div>
            </div>
          )}
        </main>
      </div>
    </AdminShell>
  );
}
