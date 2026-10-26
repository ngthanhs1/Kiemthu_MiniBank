"use client";


import { useCallback, useEffect, useMemo, useState } from "react";
import { AlertCircle, CheckCircle2, ChevronDown, Clock, Eye, Search, X } from "lucide-react";
import AdminShell from "../../../components/admin-shell";

const API_BASE = (
  process.env.NEXT_PUBLIC_API_BASE_URL ??
  process.env.NEXT_PUBLIC_API_URL ??
  "http://localhost:8080"
).replace(/\/+$/, "");

type RepaymentScheduleItem = {
  id: number;
  loanId: number | null;
  loanCode: string | null;
  customerName: string | null;
  installmentNo: number;
  dueDate: string;
  principalAmount: number;
  interestAmount: number;
  penaltyAmount: number;
  feeAmount: number;
  totalAmount: number;
  paidAmount: number;
  status: string;
  paidAt: string | null;
};

const statusOptions = [
  { value: "ALL", label: "Tat ca trang thai" },
  { value: "upcoming", label: "Sap den han" },
  { value: "paid", label: "Da tra" },
  { value: "overdue", label: "Qua han" },
  { value: "unpaid", label: "Chua tra" },
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

function isPaid(item: RepaymentScheduleItem) {
  return item.status?.toLowerCase() === "paid";
}

function isOverdue(item: RepaymentScheduleItem) {
  if (isPaid(item)) return false;
  const date = new Date(item.dueDate);
  return !Number.isNaN(date.getTime()) && date.getTime() < Date.now();
}

function isUpcoming(item: RepaymentScheduleItem) {
  if (isPaid(item)) return false;
  const date = new Date(item.dueDate);
  if (Number.isNaN(date.getTime())) return false;
  const diff = date.getTime() - Date.now();
  return diff >= 0 && diff <= 7 * 24 * 60 * 60 * 1000;
}

function StatusBadge({ item }: { item: RepaymentScheduleItem }) {
  if (isPaid(item)) {
    return <span className="inline-flex items-center gap-1 rounded border border-emerald-100 bg-emerald-50 px-2.5 py-1 text-xs font-medium text-emerald-600"><CheckCircle2 size={12} />Da tra</span>;
  }
  if (isOverdue(item)) {
    return <span className="inline-flex items-center gap-1 rounded border border-red-100 bg-red-50 px-2.5 py-1 text-xs font-medium text-red-600"><AlertCircle size={12} />Qua han</span>;
  }
  if (isUpcoming(item)) {
    return <span className="inline-flex items-center gap-1 rounded border border-blue-100 bg-blue-50 px-2.5 py-1 text-xs font-medium text-blue-600"><Clock size={12} />Sap den han</span>;
  }
  return <span className="inline-flex items-center rounded border border-gray-100 bg-gray-50 px-2.5 py-1 text-xs font-medium text-gray-600">Chua tra</span>;
}

export default function LoanRepaymentPage() {
  const [token, setToken] = useState<string | null>(null);
  const [schedules, setSchedules] = useState<RepaymentScheduleItem[]>([]);
  const [searchQuery, setSearchQuery] = useState("");
  const [statusFilter, setStatusFilter] = useState("ALL");
  const [isDropdownOpen, setIsDropdownOpen] = useState(false);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [selectedLoanCode, setSelectedLoanCode] = useState<string | null>(null);

  useEffect(() => {
    setToken(localStorage.getItem("adminToken"));
  }, []);

  const fetchSchedules = useCallback(async () => {
    if (!token) return;
    setLoading(true);
    setError(null);
    try {
      const res = await fetch(`${API_BASE}/api/admin/loan-repayment-schedules`, {
        headers: {
          "Content-Type": "application/json",
          Authorization: `Bearer ${token}`,
        },
      });
      if (!res.ok) throw new Error((await res.text()) || "Load failed");
      setSchedules((await res.json()) as RepaymentScheduleItem[]);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Load failed");
    } finally {
      setLoading(false);
    }
  }, [token]);

  useEffect(() => {
    fetchSchedules();
  }, [fetchSchedules]);

  const filteredSchedules = useMemo(() => {
    const query = searchQuery.trim().toLowerCase();
    const now = new Date();
    return schedules.filter((item) => {
      const dueDate = new Date(item.dueDate);
      const isCurrentMonth = !Number.isNaN(dueDate.getTime()) &&
        dueDate.getFullYear() === now.getFullYear() &&
        dueDate.getMonth() === now.getMonth();
      const matchesSearch = !query || [item.customerName, item.loanCode]
        .filter(Boolean)
        .some((value) => value!.toLowerCase().includes(query));
      const matchesStatus =
        statusFilter === "ALL" ||
        (statusFilter === "paid" && isPaid(item)) ||
        (statusFilter === "overdue" && isOverdue(item)) ||
        (statusFilter === "upcoming" && isUpcoming(item)) ||
        (statusFilter === "unpaid" && !isPaid(item));
      return isCurrentMonth && matchesSearch && matchesStatus;
    });
  }, [schedules, searchQuery, statusFilter]);

  const selectedLoanSchedules = useMemo(() => {
    if (!selectedLoanCode) return [];
    return schedules
      .filter((item) => item.loanCode === selectedLoanCode)
      .sort((a, b) => a.installmentNo - b.installmentNo);
  }, [schedules, selectedLoanCode]);

  const selectedLoanSummary = useMemo(() => {
    const items = selectedLoanSchedules;
    return {
      totalDue: items.reduce((sum, item) => sum + (item.totalAmount || 0), 0),
      paidDue: items.reduce((sum, item) => sum + (item.paidAmount || 0), 0),
      paidMonths: items.filter(isPaid).length,
      remainingMonths: items.filter((item) => !isPaid(item)).length,
    };
  }, [selectedLoanSchedules]);

  const totalPeriods = schedules.length;
  const upcomingCount = schedules.filter(isUpcoming).length;
  const paidCount = schedules.filter(isPaid).length;
  const overdueCount = schedules.filter(isOverdue).length;

  if (!token) {
    return (
      <div className="flex min-h-screen items-center justify-center bg-[#f6f7fb] px-6">
        <div className="rounded-xl bg-white p-6 text-sm text-gray-600">Vui long dang nhap de tiep tuc.</div>
      </div>
    );
  }

  return (
    <AdminShell title="Lich tra no" subtitle="Theo doi lich tra no cua tat ca cac khoan vay">
      <div className="space-y-6 font-sans">
        <div className="flex flex-col items-end justify-between gap-4 rounded-xl border border-gray-100 bg-white p-4 shadow-sm md:flex-row md:items-center">
          <div className="w-full md:w-2/3">
            <div className="relative">
              <Search className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400" size={16} />
              <input
                type="text"
                placeholder="Tim theo ten KH, ma khoan vay..."
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                className="w-full rounded-lg border border-gray-200 bg-gray-50/50 py-2 pl-9 pr-4 text-sm outline-none focus:border-blue-500"
              />
            </div>
          </div>
          <div className="relative w-full md:w-56">
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

        <div className="overflow-hidden rounded-xl border border-gray-100 bg-white shadow-sm">
          <div className="grid grid-cols-4 border-b border-gray-100 bg-gray-50/50 p-4 text-center text-xs font-semibold text-gray-500">
            <div className="border-r border-gray-100"><p>Tong so ky</p><p className="mt-1 text-sm font-bold text-gray-900">{totalPeriods}</p></div>
            <div className="border-r border-gray-100"><p className="text-blue-600">Sap den han</p><p className="mt-1 text-sm font-bold text-blue-600">{upcomingCount}</p></div>
            <div className="border-r border-gray-100"><p className="text-emerald-600">Da tra</p><p className="mt-1 text-sm font-bold text-emerald-600">{paidCount}</p></div>
            <div><p className="text-red-600">Qua han</p><p className="mt-1 text-sm font-bold text-red-600">{overdueCount}</p></div>
          </div>

          <div className="overflow-x-auto">
            <table className="w-full border-collapse text-left">
              <thead>
                <tr className="border-b border-gray-100 bg-gray-50 text-xs font-semibold uppercase text-gray-500">
                  <th className="px-4 py-3.5">Ky</th>
                  <th className="px-4 py-3.5">Ma khoan vay</th>
                  <th className="px-4 py-3.5">Khach hang</th>
                  <th className="px-4 py-3.5">Ngay den han</th>
                  <th className="px-4 py-3.5 text-right">Tien goc</th>
                  <th className="px-4 py-3.5 text-right">Tien lai</th>
                  <th className="px-4 py-3.5 text-right">Tong phai tra</th>
                  <th className="px-4 py-3.5 text-right">Da tra</th>
                  <th className="px-4 py-3.5 text-center">Trang thai</th>
                  <th className="px-4 py-3.5 text-center">Chi tiet</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-100 text-sm text-gray-700">
                {loading ? (
                  <tr><td colSpan={10} className="py-8 text-center text-gray-400">Dang tai...</td></tr>
                ) : filteredSchedules.length === 0 ? (
                  <tr><td colSpan={10} className="py-8 text-center text-gray-400">Khong co lich tra no thang hien tai</td></tr>
                ) : filteredSchedules.map((row) => (
                  <tr key={row.id} className="transition-colors hover:bg-gray-50/50">
                    <td className="px-4 py-4 font-medium text-gray-900">Ky {row.installmentNo}</td>
                    <td className="px-4 py-4 font-semibold text-blue-600">{row.loanCode ?? "-"}</td>
                    <td className="px-4 py-4 font-medium text-gray-900">{row.customerName ?? "-"}</td>
                    <td className="px-4 py-4 text-gray-600">{formatDate(row.dueDate)}</td>
                    <td className="px-4 py-4 text-right font-medium">{formatVND(row.principalAmount)}</td>
                    <td className="px-4 py-4 text-right font-medium">{formatVND(row.interestAmount)}</td>
                    <td className="px-4 py-4 text-right font-semibold text-gray-900">{formatVND(row.totalAmount)}</td>
                    <td className="px-4 py-4 text-right font-semibold text-emerald-600">{formatVND(row.paidAmount)}</td>
                    <td className="px-4 py-4 text-center"><StatusBadge item={row} /></td>
                    <td className="px-4 py-4 text-center">
                      <button type="button" onClick={() => setSelectedLoanCode(row.loanCode)} className="inline-flex items-center gap-1 rounded-lg border border-gray-200 px-2.5 py-1 text-xs font-semibold text-blue-600 hover:bg-blue-50">
                        <Eye size={13} /> Xem
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
        {selectedLoanCode ? (
          <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40 p-4 backdrop-blur-sm">
            <div className="w-full max-w-2xl rounded-2xl bg-white p-6 shadow-2xl">
              <div className="mb-4 flex items-start justify-between">
                <div>
                  <h3 className="text-lg font-bold text-gray-900">Chi tiet lich tra no {selectedLoanCode}</h3>
                  <p className="text-sm text-gray-500">{selectedLoanSchedules[0]?.customerName ?? "-"}</p>
                </div>
                <button type="button" onClick={() => setSelectedLoanCode(null)} className="rounded-full p-1 text-gray-400 hover:bg-gray-100"><X size={18} /></button>
              </div>
              <div className="grid grid-cols-2 gap-3 md:grid-cols-4">
                <div className="rounded-xl bg-gray-50 p-3"><p className="text-xs text-gray-500">Tong phai tra</p><p className="font-bold text-gray-900">{formatVND(selectedLoanSummary.totalDue)}</p></div>
                <div className="rounded-xl bg-emerald-50 p-3"><p className="text-xs text-emerald-600">Da tra</p><p className="font-bold text-emerald-700">{formatVND(selectedLoanSummary.paidDue)}</p></div>
                <div className="rounded-xl bg-blue-50 p-3"><p className="text-xs text-blue-600">So thang da tra</p><p className="font-bold text-blue-700">{selectedLoanSummary.paidMonths}</p></div>
                <div className="rounded-xl bg-amber-50 p-3"><p className="text-xs text-amber-600">So thang con lai</p><p className="font-bold text-amber-700">{selectedLoanSummary.remainingMonths}</p></div>
              </div>
            </div>
          </div>
        ) : null}
      </div>
    </AdminShell>
  );
}
