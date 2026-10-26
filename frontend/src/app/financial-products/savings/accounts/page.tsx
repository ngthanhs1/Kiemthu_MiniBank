"use client";

/* eslint-disable react-hooks/set-state-in-effect */

import { useCallback, useEffect, useMemo, useState } from "react";
import AdminShell from "../../../components/admin-shell";

const API_BASE = (
  process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8080"
).replace(/\/+$/, "");

type SavingListItem = {
  id: number;
  code: string;
  status: string;
  userFullName: string | null;
  userPhone: string | null;
  principalAmount: number;
  actualInterestRate: number;
  productName: string | null;
  termUnit: string;
  termValue: number;
  sourceAccountNumber: string | null;
  sourceAccountName: string | null;
  autoRenew: boolean;
  openDate: string | null;
  maturityDate: string | null;
};

type SavingDetail = {
  id: number;
  code: string;
  status: string;
  principalAmount: number;
  actualInterestRate: number;
  termUnit: string;
  termValue: number;
  autoRenew: boolean;
  openDate: string | null;
  maturityDate: string | null;
  agreementAcceptedAt: string | null;
  agreementVersion: string | null;
  sourceAccountNumber: string | null;
  sourceAccountName: string | null;
  settlementAccountId: number | null;
  settlementAccountNumber: string | null;
  settlementAccountName: string | null;
  productCode: string | null;
  productName: string | null;
  userFullName: string | null;
  userPhone: string | null;
  userEmail: string | null;
  rejectionReason: string | null;
  documents: Array<{
    id: number;
    documentType: string;
    fileName: string;
    fileUrl: string;
    mimeType: string;
    verifiedStatus: string | null;
    uploadedAt: string | null;
    note: string | null;
  }>;
  contractId: number | null;
  contractNumber: string | null;
  contractStatus: string | null;
};

function formatCurrency(value: number | null | undefined) {
  if (value === null || value === undefined) return "-";
  return value
    .toFixed(0)
    .replace(/\B(?=(\d{3})+(?!\d))/g, ".");
}

function formatPercent(value: number | null | undefined) {
  if (value === null || value === undefined) return "-";
  return `${(value * 100).toFixed(2).replace(/\.00$/, "")}%`;
}

function formatDate(value: string | null | undefined) {
  if (!value) return "-";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return value;
  return date.toLocaleDateString("vi-VN");
}

function formatStatus(value: string | null | undefined) {
  if (!value) return "Khac";
  const normalized = value.toLowerCase();
  switch (normalized) {
    case "active":
      return "Hoat dong";
    case "pending_approval":
      return "Cho duyet";
    case "pending_contract":
      return "Cho hop dong";
    case "rejected":
      return "Tu choi";
    case "closed":
      return "Da dong";
    case "locked":
      return "Tam khoa";
    default:
      return value;
  }
}

function statusTone(value: string | null | undefined) {
  const normalized = value?.toLowerCase();
  switch (normalized) {
    case "active":
      return "bg-emerald-50 text-emerald-600";
    case "pending_approval":
    case "pending_contract":
      return "bg-amber-50 text-amber-600";
    case "rejected":
      return "bg-rose-50 text-rose-600";
    case "closed":
      return "bg-zinc-100 text-zinc-600";
    default:
      return "bg-zinc-100 text-zinc-600";
  }
}

function isDueSoon(dateValue: string | null | undefined, days = 7) {
  if (!dateValue) return false;
  const date = new Date(dateValue);
  if (Number.isNaN(date.getTime())) return false;
  const now = new Date();
  const diff = date.getTime() - now.getTime();
  return diff >= 0 && diff <= days * 24 * 60 * 60 * 1000;
}

export default function SavingAccountsPage() {
  const [token, setToken] = useState<string | null>(null);
  const [items, setItems] = useState<SavingListItem[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [query, setQuery] = useState("");
  const [statusFilter, setStatusFilter] = useState("active");

  const [detailOpen, setDetailOpen] = useState(false);
  const [detailLoading, setDetailLoading] = useState(false);
  const [detailError, setDetailError] = useState<string | null>(null);
  const [detail, setDetail] = useState<SavingDetail | null>(null);

  const [rejectReason, setRejectReason] = useState("");
  const [actionLoading, setActionLoading] = useState(false);

  useEffect(() => {
    const t = localStorage.getItem("adminToken");
    setToken(t);
  }, []);

  const authHeader = useMemo<HeadersInit>(() => {
    if (!token) return {} as HeadersInit;
    return { Authorization: `Bearer ${token}` } as HeadersInit;
  }, [token]);

  const fetchSavings = useCallback(async () => {
    if (!token) return;
    setLoading(true);
    setError(null);
    try {
      const url = new URL(`${API_BASE}/api/admin/savings`);
      if (statusFilter !== "all") {
        url.searchParams.set("status", statusFilter);
      }
      const res = await fetch(url.toString(), {
        headers: {
          "Content-Type": "application/json",
          ...authHeader,
        },
      });
      if (!res.ok) {
        const text = await res.text();
        throw new Error(text || "Load failed");
      }
      const data = (await res.json()) as SavingListItem[];
      setItems(data.filter((item) => item.status?.toLowerCase() === "active"));
    } catch (err) {
      setError(err instanceof Error ? err.message : "Load failed");
    } finally {
      setLoading(false);
    }
  }, [authHeader, statusFilter, token]);

  const fetchDetail = useCallback(async (id: number) => {
    if (!token) return;
    setDetailLoading(true);
    setDetailError(null);
    try {
      const res = await fetch(`${API_BASE}/api/admin/savings/${id}`, {
        headers: {
          "Content-Type": "application/json",
          ...authHeader,
        },
      });
      if (!res.ok) {
        const text = await res.text();
        throw new Error(text || "Load failed");
      }
      const data = (await res.json()) as SavingDetail;
      setDetail(data);
    } catch (err) {
      setDetailError(err instanceof Error ? err.message : "Load failed");
    } finally {
      setDetailLoading(false);
    }
  }, [authHeader, token]);

  useEffect(() => {
    if (token) {
      fetchSavings();
    }
  }, [fetchSavings, token]);

  const filtered = useMemo(() => {
    if (!query.trim()) return items;
    const q = query.trim().toLowerCase();
    return items.filter((item) => {
      if (item.status?.toLowerCase() !== "active") return false;
      return [
        item.code,
        item.userFullName,
        item.userPhone,
        item.sourceAccountNumber,
        item.sourceAccountName,
        item.productName,
      ]
        .filter(Boolean)
        .some((value) => value?.toLowerCase().includes(q));
    });
  }, [items, query]);

  const totalPrincipal = useMemo(() => {
    return items.reduce((sum, item) => sum + (item.principalAmount ?? 0), 0);
  }, [items]);

  const activeCount = useMemo(() => {
    return items.filter((item) => item.status?.toLowerCase() === "active").length;
  }, [items]);

  const dueSoonCount = useMemo(() => {
    return items.filter((item) => isDueSoon(item.maturityDate)).length;
  }, [items]);

  const statusOptions = useMemo(() => {
    const unique = new Set(items.map((item) => item.status).filter(Boolean));
    return Array.from(unique).sort();
  }, [items]);

  function openDetail(item: SavingListItem) {
    setDetail(null);
    setRejectReason("");
    setDetailOpen(true);
    fetchDetail(item.id);
  }

  async function approveSaving() {
    if (!detail) return;
    setActionLoading(true);
    setDetailError(null);
    try {
      const res = await fetch(`${API_BASE}/api/admin/savings/${detail.id}/approve`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          ...authHeader,
        },
      });
      if (!res.ok) {
        const text = await res.text();
        throw new Error(text || "Approve failed");
      }
      await fetchSavings();
      await fetchDetail(detail.id);
    } catch (err) {
      setDetailError(err instanceof Error ? err.message : "Approve failed");
    } finally {
      setActionLoading(false);
    }
  }

  async function rejectSaving() {
    if (!detail) return;
    setActionLoading(true);
    setDetailError(null);
    try {
      const res = await fetch(`${API_BASE}/api/admin/savings/${detail.id}/reject`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          ...authHeader,
        },
        body: JSON.stringify({ reason: rejectReason || null }),
      });
      if (!res.ok) {
        const text = await res.text();
        throw new Error(text || "Reject failed");
      }
      await fetchSavings();
      await fetchDetail(detail.id);
    } catch (err) {
      setDetailError(err instanceof Error ? err.message : "Reject failed");
    } finally {
      setActionLoading(false);
    }
  }

  async function closeSaving() {
    if (!detail) return;
    const confirmed = window.confirm("Xac nhan tat toan so tiet kiem nay?");
    if (!confirmed) return;
    setActionLoading(true);
    setDetailError(null);
    try {
      const res = await fetch(`${API_BASE}/api/admin/savings/${detail.id}/close`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          ...authHeader,
        },
        body: JSON.stringify({
          settlementAccountId: detail.settlementAccountId,
          note: "Admin manual saving closure",
        }),
      });
      if (!res.ok) {
        const text = await res.text();
        throw new Error(text || "Close failed");
      }
      await fetchSavings();
      await fetchDetail(detail.id);
    } catch (err) {
      setDetailError(err instanceof Error ? err.message : "Close failed");
    } finally {
      setActionLoading(false);
    }
  }

  if (!token) {
    return (
      <div className="flex min-h-screen items-center justify-center bg-[#f6f7fb] px-6 text-[#111827]">
        <div className="w-full max-w-md rounded-2xl border border-black/5 bg-white p-6">
          <h1 className="text-xl font-semibold">So tiet kiem</h1>
          <p className="mt-2 text-sm text-zinc-500">Vui long dang nhap de tiep tuc.</p>
          <a
            className="mt-4 inline-flex h-10 items-center justify-center rounded-lg bg-blue-600 px-4 text-sm font-semibold text-white"
            href="/login"
          >
            Di toi dang nhap
          </a>
        </div>
      </div>
    );
  }

  return (
    <AdminShell
      title="Quan ly So tiet kiem"
      subtitle="Theo doi dong tien gui va quan ly so tiet kiem"
    >
      <section className="grid gap-4 md:grid-cols-3">
        <div className="rounded-2xl border border-black/5 bg-white p-4 shadow-sm">
          <div className="text-xs text-zinc-500">Tong tien huy dong</div>
          <div className="mt-2 text-2xl font-semibold">{formatCurrency(totalPrincipal)} d</div>
        </div>
        <div className="rounded-2xl border border-black/5 bg-white p-4 shadow-sm">
          <div className="text-xs text-zinc-500">Tong so so dang hoat dong</div>
          <div className="mt-2 text-2xl font-semibold">{activeCount} so</div>
        </div>
        <div className="rounded-2xl border border-black/5 bg-white p-4 shadow-sm">
          <div className="text-xs text-zinc-500">So sap dao han (7 ngay)</div>
          <div className="mt-2 text-2xl font-semibold">{dueSoonCount} so</div>
        </div>
      </section>

      <section className="rounded-2xl border border-black/5 bg-white p-6 shadow-sm">
        <div className="flex flex-wrap items-center justify-between gap-3">
          <div>
            <div className="text-sm font-semibold">Danh sach so tiet kiem</div>
            <div className="text-xs text-zinc-500">Theo doi trang thai va ngay dao han</div>
          </div>
          <div className="flex flex-wrap items-center gap-2">
            <div className="flex h-10 items-center gap-2 rounded-lg border border-black/10 bg-white px-3">
              <span className="text-xs text-zinc-400">🔎</span>
              <input
                className="h-8 w-60 bg-transparent text-sm outline-none"
                placeholder="Tim theo ma so, ten, SDT..."
                value={query}
                onChange={(e) => setQuery(e.target.value)}
              />
            </div>
            <select
              className="h-10 rounded-lg border border-black/10 bg-white px-3 text-sm"
              value={statusFilter}
              onChange={(e) => setStatusFilter(e.target.value)}
            >
              <option value="all">Tat ca trang thai</option>
              {statusOptions.map((status) => (
                <option key={status} value={status}>
                  {formatStatus(status)}
                </option>
              ))}
            </select>
            <button
              className="h-10 rounded-lg border border-black/10 px-3 text-sm"
              onClick={fetchSavings}
              type="button"
            >
              Lam moi
            </button>
          </div>
        </div>

        {error ? (
          <div className="mt-4 rounded-lg border border-red-100 bg-red-50 p-3 text-sm text-red-700">
            {error}
          </div>
        ) : null}

        <div className="mt-4 overflow-hidden rounded-xl border border-black/5">
          <table className="w-full text-left text-sm">
            <thead className="bg-zinc-50 text-xs text-zinc-500">
              <tr>
                <th className="px-4 py-3">Ma so</th>
                <th className="px-4 py-3">Khach hang</th>
                <th className="px-4 py-3">So tien goc</th>
                <th className="px-4 py-3">Lai suat</th>
                <th className="px-4 py-3">Ky han</th>
                <th className="px-4 py-3">Ngay mo so</th>
                <th className="px-4 py-3">Ngay dao han</th>
                <th className="px-4 py-3">Trang thai</th>
                <th className="px-4 py-3">Hanh dong</th>
              </tr>
            </thead>
            <tbody>
              {loading ? (
                <tr>
                  <td className="px-4 py-6 text-center text-zinc-400" colSpan={9}>
                    Dang tai...
                  </td>
                </tr>
              ) : filtered.length === 0 ? (
                <tr>
                  <td className="px-4 py-6 text-center text-zinc-400" colSpan={9}>
                    Chua co so tiet kiem.
                  </td>
                </tr>
              ) : (
                filtered.map((item) => (
                  <tr key={item.id} className="border-t border-black/5 hover:bg-zinc-50/70">
                    <td className="px-4 py-3 font-medium text-zinc-900">{item.code}</td>
                    <td className="px-4 py-3">
                      <div className="font-medium">{item.userFullName ?? "-"}</div>
                      <div className="text-xs text-zinc-400">{item.userPhone ?? item.sourceAccountNumber ?? "-"}</div>
                    </td>
                    <td className="px-4 py-3">{formatCurrency(item.principalAmount)} d</td>
                    <td className="px-4 py-3 text-emerald-600">
                      {formatPercent(item.actualInterestRate)} / nam
                    </td>
                    <td className="px-4 py-3">
                      {item.termValue} {item.termUnit?.toLowerCase() === "month" ? "thang" : item.termUnit}
                    </td>
                    <td className="px-4 py-3">{formatDate(item.openDate)}</td>
                    <td className="px-4 py-3">{formatDate(item.maturityDate)}</td>
                    <td className="px-4 py-3">
                      <span className={`rounded-full px-2 py-1 text-xs ${statusTone(item.status)}`}>
                        {formatStatus(item.status)}
                      </span>
                    </td>
                    <td className="px-4 py-3">
                      <button
                        className="text-sm font-medium text-blue-600 hover:text-blue-700"
                        type="button"
                        onClick={() => openDetail(item)}
                      >
                        Chi tiet
                      </button>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </section>

      {detailOpen ? (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40 px-4">
          <div className="w-full max-w-3xl rounded-2xl bg-white p-6 shadow-xl">
            <div className="flex items-start justify-between">
              <div>
                <div className="text-lg font-semibold">Chi tiet So tiet kiem</div>
                <div className="text-xs text-zinc-500">Thong tin va trang thai so</div>
              </div>
              <button
                className="text-zinc-400"
                type="button"
                onClick={() => setDetailOpen(false)}
              >
                ✕
              </button>
            </div>

            {detailLoading ? (
              <div className="mt-6 text-center text-sm text-zinc-400">Dang tai...</div>
            ) : detailError ? (
              <div className="mt-4 rounded-lg border border-red-100 bg-red-50 p-3 text-sm text-red-700">
                {detailError}
              </div>
            ) : detail ? (
              <div className="mt-5 space-y-5 text-sm text-zinc-700">
                <div className="rounded-xl border border-black/5 bg-zinc-50 p-4">
                  <div className="grid gap-3 md:grid-cols-3">
                    <div>
                      <div className="text-xs text-zinc-400">Ma so</div>
                      <div className="font-semibold text-zinc-900">{detail.code}</div>
                    </div>
                    <div>
                      <div className="text-xs text-zinc-400">Khach hang</div>
                      <div className="font-semibold text-zinc-900">{detail.userFullName ?? "-"}</div>
                      <div className="text-xs text-zinc-400">{detail.userPhone ?? "-"}</div>
                    </div>
                    <div>
                      <div className="text-xs text-zinc-400">Trang thai</div>
                      <span className={`inline-flex rounded-full px-2 py-1 text-xs ${statusTone(detail.status)}`}>
                        {formatStatus(detail.status)}
                      </span>
                    </div>
                  </div>
                </div>

                <div className="grid gap-4 md:grid-cols-2">
                  <div className="rounded-xl border border-black/5 p-4">
                    <div className="text-xs text-zinc-400">So tien goc</div>
                    <div className="mt-1 text-lg font-semibold text-zinc-900">
                      {formatCurrency(detail.principalAmount)} d
                    </div>
                    <div className="mt-2 text-xs text-zinc-400">Lai suat</div>
                    <div className="text-sm font-semibold text-emerald-600">
                      {formatPercent(detail.actualInterestRate)} / nam
                    </div>
                  </div>
                  <div className="rounded-xl border border-black/5 p-4">
                    <div className="text-xs text-zinc-400">San pham</div>
                    <div className="mt-1 text-sm font-semibold text-zinc-900">
                      {detail.productName ?? detail.productCode ?? "-"}
                    </div>
                    <div className="mt-2 text-xs text-zinc-400">Ky han</div>
                    <div className="text-sm font-semibold">
                      {detail.termValue} {detail.termUnit?.toLowerCase() === "month" ? "thang" : detail.termUnit}
                    </div>
                  </div>
                </div>

                <div className="grid gap-4 md:grid-cols-2">
                  <div className="rounded-xl border border-black/5 p-4">
                    <div className="text-xs text-zinc-400">Ngay mo so</div>
                    <div className="mt-1 font-semibold">{formatDate(detail.openDate)}</div>
                    <div className="mt-2 text-xs text-zinc-400">Ngay dao han</div>
                    <div className="font-semibold">{formatDate(detail.maturityDate)}</div>
                  </div>
                  <div className="rounded-xl border border-black/5 p-4">
                    <div className="text-xs text-zinc-400">Tai khoan nguon</div>
                    <div className="mt-1 font-semibold">
                      {detail.sourceAccountNumber ?? "-"}
                    </div>
                    <div className="text-xs text-zinc-400">{detail.sourceAccountName ?? "-"}</div>
                    <div className="mt-2 text-xs text-zinc-400">Tai khoan nhan</div>
                    <div className="font-semibold">
                      {detail.settlementAccountNumber ?? "-"}
                    </div>
                    <div className="text-xs text-zinc-400">{detail.settlementAccountName ?? "-"}</div>
                  </div>
                </div>

                {detail.rejectionReason ? (
                  <div className="rounded-xl border border-rose-100 bg-rose-50 p-4 text-rose-700">
                    <div className="text-xs font-semibold">Ly do tu choi</div>
                    <div className="mt-1 text-sm">{detail.rejectionReason}</div>
                  </div>
                ) : null}

                {detail.contractId ? (
                  <div className="rounded-xl border border-black/5 p-4">
                    <div className="text-xs text-zinc-400">Hop dong gan nhat</div>
                    <div className="mt-1 font-semibold">{detail.contractNumber ?? `#${detail.contractId}`}</div>
                    <div className="text-xs text-zinc-400">{detail.contractStatus ?? "-"}</div>
                  </div>
                ) : null}

                {detail.documents?.length ? (
                  <div className="rounded-xl border border-black/5 p-4">
                    <div className="text-xs text-zinc-400">Tai lieu dinh kem</div>
                    <div className="mt-2 grid gap-2">
                      {detail.documents.map((doc) => (
                        <a
                          key={doc.id}
                          href={doc.fileUrl}
                          className="flex items-center justify-between rounded-lg border border-black/5 px-3 py-2 text-sm hover:bg-zinc-50"
                          target="_blank"
                          rel="noreferrer"
                        >
                          <div>
                            <div className="font-medium text-zinc-900">{doc.fileName}</div>
                            <div className="text-xs text-zinc-400">{doc.documentType}</div>
                          </div>
                          <span className="text-xs text-blue-600">Mo</span>
                        </a>
                      ))}
                    </div>
                  </div>
                ) : null}

                {detail.status?.toLowerCase().includes("pending") ? (
                  <div className="rounded-xl border border-amber-100 bg-amber-50 p-4">
                    <div className="text-xs text-amber-700">Xu ly yeu cau mo so</div>
                    <div className="mt-3 grid gap-3 md:grid-cols-2">
                      <div>
                        <label className="text-xs text-amber-700">Ly do tu choi</label>
                        <input
                          className="mt-1 h-10 w-full rounded-lg border border-amber-200 bg-white px-3 text-sm outline-none"
                          placeholder="Nhap ly do (neu tu choi)"
                          value={rejectReason}
                          onChange={(e) => setRejectReason(e.target.value)}
                        />
                      </div>
                      <div className="flex items-end justify-end gap-2">
                        <button
                          className="h-10 rounded-lg border border-amber-200 px-4 text-sm text-amber-700 disabled:opacity-60"
                          type="button"
                          onClick={rejectSaving}
                          disabled={actionLoading}
                        >
                          {actionLoading ? "Dang xu ly..." : "Tu choi"}
                        </button>
                        <button
                          className="h-10 rounded-lg bg-emerald-600 px-4 text-sm font-semibold text-white disabled:opacity-60"
                          type="button"
                          onClick={approveSaving}
                          disabled={actionLoading}
                        >
                          {actionLoading ? "Dang xu ly..." : "Phe duyet"}
                        </button>
                      </div>
                    </div>
                  </div>
                ) : null}

                {detail.status?.toLowerCase() === "active" ? (
                  <div className="rounded-xl border border-blue-100 bg-blue-50 p-4">
                    <div className="text-xs text-blue-700">Tat toan so tiet kiem thu cong</div>
                    <div className="mt-2 text-xs text-blue-700">
                      Tien goc va lai ghi nhan se duoc cong ve tai khoan nhan cua khach hang.
                    </div>
                    <div className="mt-3 flex justify-end">
                      <button
                        className="h-10 rounded-lg bg-blue-600 px-4 text-sm font-semibold text-white disabled:opacity-60"
                        type="button"
                        onClick={closeSaving}
                        disabled={actionLoading}
                      >
                        {actionLoading ? "Dang xu ly..." : "Tat toan"}
                      </button>
                    </div>
                  </div>
                ) : null}
              </div>
            ) : null}

            <div className="mt-6 flex justify-end">
              <button
                className="h-10 rounded-lg border border-black/10 px-4 text-sm"
                type="button"
                onClick={() => setDetailOpen(false)}
              >
                Dong
              </button>
            </div>
          </div>
        </div>
      ) : null}
    </AdminShell>
  );
}
