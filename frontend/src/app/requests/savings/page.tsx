"use client";

import { useEffect, useMemo, useState } from "react";
import { Check, ChevronLeft, FileText, Search, X } from "lucide-react";

import AdminShell from "../../components/admin-shell";
import {
  approveSaving,
  getSavingApproval,
  listSavingsApprovals,
  rejectSaving,
  type ApprovalProgress,
  type SavingApprovalDetail,
  type SavingApprovalListItem,
} from "../../../lib/api/admin-approvals";

const STATUS_FILTERS = [
  { label: "Tất cả", value: "pending_otp,pending_approval,pending_contract,active,rejected" },
  { label: "Chờ xử lý", value: "pending_otp,pending_approval,pending_contract" },
  { label: "Đã duyệt", value: "active" },
  { label: "Từ chối", value: "rejected" },
];

const CHECKLIST = [
  "Xác nhận thông tin khách hàng khớp hồ sơ",
  "Kiểm tra CCCD và chữ ký điện tử",
  "Kiểm tra tài khoản nguồn / tài khoản thanh toán",
  "Đối chiếu sản phẩm, kỳ hạn và lãi suất",
  "Xác nhận số tiền gửi và trạng thái hợp lệ",
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
  return new Intl.DateTimeFormat("vi-VN", {
    dateStyle: "short",
    timeStyle: "short",
  }).format(date);
}

function statusLabel(status: string) {
  const normalized = status.toLowerCase();
  if (normalized.includes("pending")) return "Chờ duyệt";
  if (normalized === "active") return "Đã duyệt";
  if (normalized === "rejected") return "Từ chối";
  return status;
}

function statusClass(status: string) {
  const normalized = status.toLowerCase();
  if (normalized.includes("pending")) return "bg-yellow-100 text-yellow-700";
  if (normalized === "active") return "bg-green-100 text-green-700";
  if (normalized === "rejected") return "bg-red-100 text-red-700";
  return "bg-slate-100 text-slate-700";
}

function isImageUrl(url: string | null | undefined, mimeType: string | null | undefined) {
  if (!url) return false;
  if (mimeType?.toLowerCase().startsWith("image/")) return true;
  return /\.(png|jpe?g|webp|gif|bmp|avif)(\?.*)?$/i.test(url);
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
          Hệ thống sẽ tự áp policy theo mức tiền. Tiến độ duyệt sẽ xuất hiện sau khi sổ được khởi tạo.
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

export default function SavingsApprovalPage() {
  const [items, setItems] = useState<SavingApprovalListItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [search, setSearch] = useState("");
  const [statusFilter, setStatusFilter] = useState(STATUS_FILTERS[0].value);
  const [selectedId, setSelectedId] = useState<number | null>(null);
  const [detail, setDetail] = useState<SavingApprovalDetail | null>(null);
  const [detailLoading, setDetailLoading] = useState(false);
  const [rejectReason, setRejectReason] = useState("");
  const [toast, setToast] = useState<{ tone: "success" | "error"; text: string } | null>(null);
  const [checked, setChecked] = useState<boolean[]>(() => new Array(CHECKLIST.length).fill(false));
  const [approvalNote, setApprovalNote] = useState("");

  const filteredItems = useMemo(() => {
    const q = search.trim().toLowerCase();
    return items.filter((item) => {
      const matchesSearch =
        !q ||
        item.code.toLowerCase().includes(q) ||
        item.userFullName?.toLowerCase().includes(q) ||
        item.userPhone?.toLowerCase().includes(q) ||
        item.sourceAccountNumber?.toLowerCase().includes(q);
      return matchesSearch;
    });
  }, [items, search]);

  useEffect(() => {
    let alive = true;

    async function loadList() {
      setLoading(true);
      setError("");
      try {
        const data = await listSavingsApprovals(statusFilter);
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
    setChecked(new Array(CHECKLIST.length).fill(false));

    getSavingApproval(selectedId)
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
  const pendingCount = items.filter((item) => item.status.toLowerCase().includes("pending")).length;
  const approvedCount = items.filter((item) => item.status.toLowerCase() === "active").length;
  const checklistDone = checked.every(Boolean);
  const canApprove = Boolean(selectedId && detail?.status.toLowerCase().includes("pending") && checklistDone);

  async function refresh() {
    const data = await listSavingsApprovals(statusFilter);
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
        `Checklist: ${CHECKLIST.join(" | ")}`,
      ].filter(Boolean).join("\n");
      await approveSaving(selectedId, note);
      setToast({ tone: "success", text: "Đã ghi nhận bước duyệt sổ tiết kiệm" });
      setApprovalNote("");
      setChecked(new Array(CHECKLIST.length).fill(false));
      await refresh();
      setDetail(await getSavingApproval(selectedId));
    } catch (err) {
      setToast({ tone: "error", text: err instanceof Error ? err.message : "Không thể duyệt sổ" });
    }
  }

  async function onReject() {
    if (!selectedId) return;
    if (!rejectReason.trim()) {
      setToast({ tone: "error", text: "Vui lòng nhập lý do từ chối" });
      return;
    }
    try {
      await rejectSaving(selectedId, rejectReason.trim());
      setToast({ tone: "success", text: "Đã từ chối sổ tiết kiệm" });
      setRejectReason("");
      await refresh();
      setDetail(await getSavingApproval(selectedId));
    } catch (err) {
      setToast({ tone: "error", text: err instanceof Error ? err.message : "Không thể từ chối sổ" });
    }
  }

  return (
    <AdminShell title="Duyệt sổ tiết kiệm" subtitle="Lấy dữ liệu trực tiếp từ database">
      {toast ? <ToastBanner tone={toast.tone} text={toast.text} /> : null}
      <div className="flex h-full gap-4">
        <aside className="flex w-[390px] min-w-[320px] flex-col rounded-2xl border border-neutral-200 bg-neutral-50">
          <div className="border-b border-neutral-200 bg-white p-5">
            <div className="mb-4 flex items-center justify-between">
              <div>
                <h1 className="text-xl font-semibold text-neutral-900">Duyệt sổ tiết kiệm</h1>
                <p className="text-sm text-neutral-500">Danh sách sổ tiết kiệm đang chờ thẩm định</p>
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
                  placeholder="Tìm mã sổ, tên KH, số tài khoản"
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
                    <div className="text-xs text-neutral-500">{item.code}</div>
                  </div>
                  <span className={`rounded-full px-2 py-1 text-xs font-medium ${statusClass(item.status)}`}>{statusLabel(item.status)}</span>
                </div>
                <div className="flex items-end justify-between gap-2">
                  <div>
                    <div className="text-sm font-semibold text-blue-700">{formatCurrency(item.principalAmount)}</div>
                    <div className="text-xs text-neutral-500">{item.productName ?? "Sản phẩm tiết kiệm"}</div>
                  </div>
                  <div className="text-right text-xs text-neutral-500">
                    <div>{item.termValue} {item.termUnit}</div>
                    <div>{formatDateTime(item.createdAt)}</div>
                  </div>
                </div>
              </button>
            ))}
          </div>
        </aside>

        <main className="flex-1 overflow-hidden rounded-2xl border border-neutral-200 bg-white">
          {!selectedItem ? (
            <div className="flex h-full items-center justify-center text-sm text-neutral-400">Chọn một sổ tiết kiệm để xem chi tiết</div>
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
                      <h2 className="text-lg font-semibold text-neutral-900">{detail.userFullName ?? "Sổ tiết kiệm"}</h2>
                      <span className={`rounded-full px-2 py-1 text-xs font-medium ${statusClass(detail.status)}`}>{statusLabel(detail.status)}</span>
                    </div>
                    <p className="text-sm text-neutral-500">{detail.code} · {formatDateTime(detail.openDate ?? selectedItem.createdAt)}</p>
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

                    <Section title="Thông tin sổ tiết kiệm">
                      <div className="grid grid-cols-2 gap-x-6 gap-y-3">
                        <Field label="Mã sổ" value={detail.code} />
                        <Field label="Trạng thái" value={statusLabel(detail.status)} />
                        <Field label="Số tiền gửi" value={formatCurrency(detail.principalAmount)} />
                        <Field label="Lãi suất thực" value={`${detail.actualInterestRate}%`} />
                        <Field label="Kỳ hạn" value={`${detail.termValue} ${detail.termUnit}`} />
                        <Field label="Tự động tái tục" value={detail.autoRenew ? "Có" : "Không"} />
                        <Field label="Ngày mở" value={formatDateTime(detail.openDate)} />
                        <Field label="Ngày đáo hạn" value={formatDateTime(detail.maturityDate)} />
                        <Field label="TK nguồn" value={detail.sourceAccountNumber ?? "—"} />
                        <Field label="TK thanh toán" value={detail.settlementAccountNumber ?? "—"} />
                        <Field label="Sản phẩm" value={detail.productName ?? "—"} />
                        <Field label="Mã sản phẩm" value={detail.productCode ?? "—"} />
                      </div>
                    </Section>

                    {detail.documents.length > 0 ? (
                      <Section title="Giấy tờ CCCD / tài liệu liên quan">
                        <div className="grid gap-3 md:grid-cols-2">
                          {detail.documents.map((doc) => (
                            isImageUrl(doc.fileUrl, doc.mimeType) ? (
                              <a key={doc.id} href={doc.fileUrl ?? "#"} target="_blank" rel="noreferrer" className="overflow-hidden rounded-xl border border-neutral-200 bg-neutral-50 hover:bg-neutral-100">
                                <img src={doc.fileUrl ?? ""} alt={doc.documentType} className="h-44 w-full object-cover" />
                                <div className="p-3">
                                  <div className="truncate text-sm font-medium text-neutral-900">{doc.documentType}</div>
                                  <div className="truncate text-xs text-neutral-500">{doc.fileName ?? doc.note ?? doc.fileUrl ?? "—"}</div>
                                </div>
                              </a>
                            ) : (
                              <a key={doc.id} href={doc.fileUrl ?? "#"} target="_blank" rel="noreferrer" className="rounded-xl border border-neutral-200 bg-neutral-50 p-3 hover:bg-neutral-100">
                                <div className="flex items-center gap-3">
                                  <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-blue-50 text-blue-600">
                                    <FileText size={18} />
                                  </div>
                                  <div className="min-w-0">
                                    <div className="truncate text-sm font-medium text-neutral-900">{doc.documentType}</div>
                                    <div className="truncate text-xs text-neutral-500">{doc.fileName ?? doc.note ?? doc.fileUrl ?? "—"}</div>
                                  </div>
                                </div>
                              </a>
                            )
                          ))}
                        </div>
                      </Section>
                    ) : null}
                  </div>

                  <div className="space-y-6">
                    <ApprovalProgressPanel progress={detail.approvalProgress} />

                    <Section title="Checklist nghiệp vụ">
                      <div className="space-y-2">
                        {CHECKLIST.map((item, index) => (
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
                          placeholder="Ghi chú duyệt / kiểm tra sổ tiết kiệm"
                          value={approvalNote}
                          onChange={(e) => setApprovalNote(e.target.value)}
                        />
                      </div>
                    </Section>

                    <Section title="Trạng thái xử lý">
                      <div className="space-y-3 text-sm">
                        <Field label="Contract" value={detail.contractNumber ?? "—"} />
                        <Field label="Trạng thái hợp đồng" value={detail.contractStatus ?? "—"} />
                        <Field label="Ngày duyệt/xử lý" value={formatDateTime(detail.agreementAcceptedAt)} />
                        <Field label="Lý do từ chối" value={detail.rejectionReason ?? "—"} />
                        <Field label="Phiên bản thỏa thuận" value={detail.agreementVersion ?? "—"} />
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
