"use client";

/* eslint-disable react-hooks/set-state-in-effect */

import { useCallback, useEffect, useMemo, useState } from "react";
import { AlertCircle, Bell, CheckCircle2, ChevronDown, Clock, Search } from "lucide-react";
import AdminShell from "../../../components/admin-shell";

const API_BASE = (
  process.env.NEXT_PUBLIC_API_BASE_URL ??
  process.env.NEXT_PUBLIC_API_URL ??
  "http://localhost:8080"
).replace(/\/+$/, "");

type LoanItem = {
  id: number;
  code: string;
  customerName: string | null;
  customerPhone: string | null;
  disbursementAccountNumber: string | null;
  repaymentAccountId: number | null;
  repaymentAccountNumber: string | null;
  approvedAmount: number;
  disbursedAmount: number;
  outstandingPrincipal: number;
  outstandingInterest: number;
  actualInterestRate: number;
  termMonths: number;
  nextDueDate: string | null;
  status: string;
  createdAt: string | null;
  closedAt: string | null;
};

const statusOptions = [
  { value: "ALL", label: "Tat ca" },
  { value: "active", label: "Dang vay" },
  { value: "closed", label: "Da tat toan" },
  { value: "overdue", label: "Qua han" },
];

function formatVND(value: number | null | undefined) {
  return new Intl.NumberFormat("vi-VN", {
    style: "currency",
    currency: "VND",
    maximumFractionDigits: 0,
  }).format(value ?? 0);
}

function formatDate(value: string | null | undefined) {
  if (!value) return "-";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return value;
  return date.toLocaleDateString("vi-VN");
}

function isDueSoon(value: string | null | undefined, days = 7) {
  if (!value) return false;
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return false;
  const diff = date.getTime() - Date.now();
  return diff >= 0 && diff <= days * 24 * 60 * 60 * 1000;
}

function isOverdue(item: LoanItem) {
  if (item.status?.toLowerCase() !== "active" || !item.nextDueDate) return false;
  const date = new Date(item.nextDueDate);
  return !Number.isNaN(date.getTime()) && date.getTime() < Date.now();
}

function statusLabel(item: LoanItem) {
  if (item.status?.toLowerCase() === "closed") return "Da tat toan";
  if (isOverdue(item)) return "Qua han";
  if (isDueSoon(item.nextDueDate)) return "Sap den han";
  if (item.status?.toLowerCase() === "active") return "Dang vay";
  return item.status ?? "-";
}

function StatusBadge({ item }: { item: LoanItem }) {
  if (item.status?.toLowerCase() === "closed") {
    return <span className="rounded border border-emerald-200 bg-emerald-50 px-2.5 py-1 text-xs font-medium text-emerald-600"><CheckCircle2 className="mr-1 inline" size={12} />Da tat toan</span>;
  }
  if (isOverdue(item)) {
    return <span className="rounded bg-red-600 px-2.5 py-1 text-xs font-medium text-white"><AlertCircle className="mr-1 inline" size={12} />Qua han</span>;
  }
  if (isDueSoon(item.nextDueDate)) {
    return <span className="rounded border border-amber-200 bg-amber-50 px-2.5 py-1 text-xs font-medium text-amber-600"><Clock className="mr-1 inline" size={12} />Sap den han</span>;
  }
  return <span className="rounded border border-blue-200 bg-blue-50 px-2.5 py-1 text-xs font-medium text-blue-600">Dang vay</span>;
}

export default function LoanManagementPage() {
  const [token, setToken] = useState<string | null>(null);
  const [items, setItems] = useState<LoanItem[]>([]);
  const [searchQuery, setSearchQuery] = useState("");
  const [statusFilter, setStatusFilter] = useState("active");
  const [isDropdownOpen, setIsDropdownOpen] = useState(false);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [actionLoadingId, setActionLoadingId] = useState<number | null>(null);

  useEffect(() => {
    setToken(localStorage.getItem("adminToken"));
  }, []);

  const fetchLoans = useCallback(async () => {
    if (!token) return;
    setLoading(true);
    setError(null);
    try {
      const url = new URL(`${API_BASE}/api/admin/loans`);
      if (statusFilter !== "ALL" && statusFilter !== "overdue") {
        url.searchParams.set("status", statusFilter);
      }
      const res = await fetch(url.toString(), {
        headers: {
          "Content-Type": "application/json",
          Authorization: `Bearer ${token}`,
        },
      });
      if (!res.ok) throw new Error((await res.text()) || "Load failed");
      const data = (await res.json()) as LoanItem[];
      setItems(data.filter((item) => item.status?.toLowerCase() !== "closed"));
    } catch (err) {
      setError(err instanceof Error ? err.message : "Load failed");
    } finally {
      setLoading(false);
    }
  }, [statusFilter, token]);

  useEffect(() => {
    fetchLoans();
  }, [fetchLoans]);

  const filtered = useMemo(() => {
    const q = searchQuery.trim().toLowerCase();
    return items.filter((item) => {
      const matchesQuery = !q || [
        item.code,
        item.customerName,
        item.customerPhone,
        item.disbursementAccountNumber,
        item.repaymentAccountNumber,
      ].filter(Boolean).some((value) => value!.toLowerCase().includes(q));
      const matchesStatus =
        item.status?.toLowerCase() !== "closed" &&
        (statusFilter === "ALL" ||
          (statusFilter === "overdue" ? isOverdue(item) : item.status?.toLowerCase() === statusFilter));
      return matchesQuery && matchesStatus;
    });
  }, [items, searchQuery, statusFilter]);

  const totalOutstanding = items
    .filter((item) => item.status?.toLowerCase() !== "closed")
    .reduce((sum, item) => sum + (item.outstandingPrincipal ?? 0) + (item.outstandingInterest ?? 0), 0);
  const upcomingCount = items.filter((item) => item.status?.toLowerCase() === "active" && isDueSoon(item.nextDueDate)).length;
  const overdueCount = items.filter(isOverdue).length;

  async function settleEarly(item: LoanItem) {
    if (!window.confirm(`Xac nhan tat toan som khoan vay ${item.code}?`)) return;
    setActionLoadingId(item.id);
    setError(null);
    try {
      const res = await fetch(`${API_BASE}/api/admin/loans/${item.id}/early-settle`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          Authorization: `Bearer ${token}`,
        },
        body: JSON.stringify({
          repaymentAccountId: item.repaymentAccountId,
          note: "Admin manual early loan settlement",
        }),
      });
      if (!res.ok) throw new Error((await res.text()) || "Settlement failed");
      await fetchLoans();
    } catch (err) {
      setError(err instanceof Error ? err.message : "Settlement failed");
    } finally {
      setActionLoadingId(null);
    }
  }

  if (!token) {
    return (
      <div className="flex min-h-screen items-center justify-center bg-[#f6f7fb] px-6">
        <div className="rounded-xl bg-white p-6 text-sm text-gray-600">Vui long dang nhap de tiep tuc.</div>
      </div>
    );
  }

  return (
    <AdminShell title="Quan ly Khoan vay & No" subtitle="Kiem soat du no va tat toan khoan vay">
      <div className="space-y-6 font-sans">
        <div className="grid grid-cols-1 gap-4 md:grid-cols-3">
          <div className="rounded-xl border border-gray-100 bg-white p-5 shadow-sm">
            <span className="text-xs font-medium text-gray-400">Tong du no hien tai</span>
            <div className="mt-2 text-xl font-bold text-gray-900">{formatVND(totalOutstanding)}</div>
          </div>
          <div className="rounded-xl border border-amber-100 bg-amber-50/60 p-5 shadow-sm">
            <span className="text-xs font-medium text-amber-700">Sap den han (7 ngay)</span>
            <div className="mt-2 text-xl font-bold text-amber-900">{upcomingCount} khoan</div>
          </div>
          <div className="rounded-xl border border-red-100 bg-red-50/50 p-5 shadow-sm">
            <span className="text-xs font-medium text-red-700">No qua han</span>
            <div className="mt-2 text-xl font-bold text-red-900">{overdueCount} khoan</div>
          </div>
        </div>

        <div className="space-y-4 rounded-xl border border-gray-100 bg-white p-4 shadow-sm">
          <div className="flex flex-col items-end justify-between gap-4 md:flex-row md:items-center">
            <div className="w-full space-y-1.5 md:w-1/2">
              <label className="text-xs font-semibold text-gray-700">Tim kiem</label>
              <div className="relative">
                <Search className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400" size={16} />
                <input
                  type="text"
                  placeholder="Tim theo ten, SDT, STK, ma khoan vay..."
                  value={searchQuery}
                  onChange={(e) => setSearchQuery(e.target.value)}
                  className="w-full rounded-lg border border-gray-200 bg-gray-50/50 py-2 pl-9 pr-4 text-sm outline-none focus:border-blue-500"
                />
              </div>
            </div>
            <div className="relative w-full space-y-1.5 md:w-64">
              <label className="text-xs font-semibold text-gray-700">Trang thai</label>
              <button
                onClick={() => setIsDropdownOpen(!isDropdownOpen)}
                className="flex w-full items-center justify-between rounded-lg border border-gray-200 bg-white px-3 py-2 text-left text-sm text-gray-700"
                type="button"
              >
                <span>{statusOptions.find((item) => item.value === statusFilter)?.label}</span>
                <ChevronDown size={16} className={`text-gray-400 ${isDropdownOpen ? "rotate-180" : ""}`} />
              </button>
              {isDropdownOpen ? (
                <div className="absolute right-0 top-full z-50 mt-1 w-full overflow-hidden rounded-lg border border-gray-100 bg-white py-1 shadow-xl">
                  {statusOptions.map((option) => (
                    <button
                      key={option.value}
                      onClick={() => { setStatusFilter(option.value); setIsDropdownOpen(false); }}
                      className="w-full px-4 py-2 text-left text-sm text-gray-700 hover:bg-gray-50"
                      type="button"
                    >
                      {option.label}
                    </button>
                  ))}
                </div>
              ) : null}
            </div>
          </div>

          {error ? <div className="rounded-lg border border-red-100 bg-red-50 p-3 text-sm text-red-700">{error}</div> : null}

          <div className="overflow-x-auto rounded-lg border border-gray-100">
            <table className="w-full border-collapse text-left">
              <thead>
                <tr className="border-b border-gray-100 bg-gray-50 text-xs font-semibold uppercase tracking-wider text-gray-500">
                  <th className="px-4 py-3.5">Ma khoan vay</th>
                  <th className="px-4 py-3.5">Khach hang</th>
                  <th className="px-4 py-3.5 text-right">So tien vay</th>
                  <th className="px-4 py-3.5 text-center">Lai suat</th>
                  <th className="px-4 py-3.5 text-center">Ky han</th>
                  <th className="px-4 py-3.5 text-center">Ngay den han</th>
                  <th className="px-4 py-3.5 text-right">Du no</th>
                  <th className="px-4 py-3.5 text-center">Trang thai</th>
                  <th className="px-4 py-3.5 text-center">Hanh dong</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-100 text-sm text-gray-700">
                {loading ? (
                  <tr><td colSpan={9} className="py-8 text-center text-gray-400">Dang tai du lieu...</td></tr>
                ) : filtered.length === 0 ? (
                  <tr><td colSpan={9} className="py-8 text-center text-gray-400">Khong co khoan vay phu hop</td></tr>
                ) : filtered.map((row) => (
                  <tr key={row.id} className="transition-colors hover:bg-gray-50/80">
                    <td className="px-4 py-4 font-medium text-gray-900">{row.code}</td>
                    <td className="px-4 py-4">
                      <div className="font-semibold text-gray-900">{row.customerName ?? "-"}</div>
                      <div className="mt-0.5 text-xs text-gray-400">{row.repaymentAccountNumber ?? row.customerPhone ?? "-"}</div>
                    </td>
                    <td className="px-4 py-4 text-right font-medium">{formatVND(row.approvedAmount)}</td>
                    <td className="px-4 py-4 text-center text-gray-600">{((row.actualInterestRate ?? 0) * 100).toFixed(2)}%/nam</td>
                    <td className="px-4 py-4 text-center text-gray-600">{row.termMonths} thang</td>
                    <td className="px-4 py-4 text-center text-gray-600">{formatDate(row.nextDueDate)}</td>
                    <td className="px-4 py-4 text-right font-semibold text-gray-900">{formatVND((row.outstandingPrincipal ?? 0) + (row.outstandingInterest ?? 0))}</td>
                    <td className="px-4 py-4 text-center"><StatusBadge item={row} /></td>
                    <td className="px-4 py-4 text-center">
                      {row.status?.toLowerCase() === "active" ? (
                        <button
                          className="rounded-lg bg-blue-600 px-3 py-1.5 text-xs font-semibold text-white disabled:opacity-60"
                          onClick={() => settleEarly(row)}
                          disabled={actionLoadingId === row.id}
                          type="button"
                          title={statusLabel(row)}
                        >
                          {actionLoadingId === row.id ? "Dang xu ly" : "Tat toan som"}
                        </button>
                      ) : (
                        <Bell className="mx-auto text-gray-300" size={16} />
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      </div>
    </AdminShell>
  );
}
