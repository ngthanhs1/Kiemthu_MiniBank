"use client";

import { useCallback, useEffect, useMemo, useState } from "react";
import AdminShell from "../../components/admin-shell";

interface LargeTransactionSummary {
  id: number;
  transactionCode: string;
  fromName: string;
  fromAccountNumber: string;
  toName: string;
  toAccountNumber: string;
  amount: number;
  currency: string;
  riskLevel: string;
  reviewStatus: string;
  createdAt: string;
}

interface LargeTransactionDetail extends LargeTransactionSummary {
  transactionType: string;
  feeAmount: number;
  description: string | null;
  status: string;
  completedAt: string | null;
}

const API_BASE = (
  process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8080"
).replace(/\/+$/, "");

const STATUS_OPTIONS = [
  { label: "Tat ca trang thai", value: "all" },
  { label: "Cho kiem tra", value: "pending_review" },
  { label: "Cho quan ly duyet", value: "pending_manager" },
];

const RISK_OPTIONS = [
  { label: "Tat ca muc rui ro", value: "all" },
  { label: "Cao", value: "high" },
  { label: "Trung binh", value: "medium" },
  { label: "Thap", value: "low" },
];

const STATUS_LABELS: Record<string, string> = {
  pending_review: "Cho kiem tra",
  pending_manager: "Cho quan ly duyet",
  completed: "Da hoan tat",
  rejected: "Tu choi",
};

const STATUS_STYLES: Record<string, string> = {
  pending_review: "bg-amber-50 text-amber-700",
  pending_manager: "bg-orange-50 text-orange-700",
  completed: "bg-emerald-50 text-emerald-700",
  rejected: "bg-rose-50 text-rose-700",
};

const RISK_LABELS: Record<string, string> = {
  high: "Cao",
  medium: "Trung binh",
  low: "Thap",
};

const RISK_STYLES: Record<string, string> = {
  high: "bg-rose-50 text-rose-700",
  medium: "bg-amber-50 text-amber-700",
  low: "bg-emerald-50 text-emerald-700",
};

function formatCurrency(amount: number, currency: string) {
  if (!Number.isFinite(amount)) return "0";
  if (currency && currency.toUpperCase() !== "VND") {
    return new Intl.NumberFormat("en-US", {
      style: "currency",
      currency,
    }).format(amount);
  }
  return new Intl.NumberFormat("vi-VN", {
    style: "currency",
    currency: "VND",
    maximumFractionDigits: 0,
  }).format(amount);
}

export default function LargeApprovalPage() {
  const [items, setItems] = useState<LargeTransactionSummary[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [search, setSearch] = useState("");
  const [statusFilter, setStatusFilter] = useState("all");
  const [riskFilter, setRiskFilter] = useState("all");
  const [selectedId, setSelectedId] = useState<number | null>(null);
  const [detail, setDetail] = useState<LargeTransactionDetail | null>(null);
  const [note, setNote] = useState("");
  const [actionLoading, setActionLoading] = useState(false);
  const canDecide =
    detail?.reviewStatus === "pending_review" || detail?.reviewStatus === "pending_manager";

  const fetchItems = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const token = localStorage.getItem("adminToken");
      if (!token) {
        setError("Vui long dang nhap de tiep tuc.");
        setItems([]);
        return;
      }
      const url = new URL(`${API_BASE}/api/admin/transactions/large`);
      if (search.trim()) url.searchParams.append("q", search.trim());
      if (statusFilter !== "all") url.searchParams.append("status", statusFilter);
      if (riskFilter !== "all") url.searchParams.append("risk", riskFilter);

      const res = await fetch(url.toString(), {
        headers: { Authorization: `Bearer ${token}` },
      });
      if (!res.ok) {
        const text = await res.text();
        throw new Error(text || "Khong the tai du lieu giao dich.");
      }
      setItems(await res.json());
    } catch (err) {
      const message = err instanceof Error ? err.message : "Khong the tai du lieu giao dich.";
      setError(message);
      setItems([]);
    } finally {
      setLoading(false);
    }
  }, [search, statusFilter, riskFilter]);

  useEffect(() => {
    const timer = setTimeout(() => fetchItems(), 250);
    return () => clearTimeout(timer);
  }, [fetchItems]);

  useEffect(() => {
    if (!selectedId) {
      setDetail(null);
      setNote("");
      return;
    }

    const loadDetail = async () => {
      try {
        const token = localStorage.getItem("adminToken");
        if (!token) return;
        const res = await fetch(`${API_BASE}/api/admin/transactions/large/${selectedId}`, {
          headers: { Authorization: `Bearer ${token}` },
        });
        if (!res.ok) return;
        setDetail(await res.json());
      } catch {
        setDetail(null);
      }
    };

    loadDetail();
  }, [selectedId]);

  const summary = useMemo(() => {
    const total = items.length;
    const highRisk = items.filter((item) => item.riskLevel === "high").length;
    const pendingManager = items.filter((item) => item.reviewStatus === "pending_manager").length;
    const totalAmount = items.reduce((sum, item) => sum + (Number(item.amount) || 0), 0);
    return { total, highRisk, pendingManager, totalAmount };
  }, [items]);

  const handleDecision = async (type: "approve" | "reject") => {
    if (!detail) return;
    setActionLoading(true);
    try {
      const token = localStorage.getItem("adminToken");
      const res = await fetch(
        `${API_BASE}/api/admin/transactions/large/${detail.id}/${type}`,
        {
          method: "POST",
          headers: {
            "Content-Type": "application/json",
            Authorization: `Bearer ${token}`,
          },
          body: JSON.stringify({ note }),
        }
      );

      if (!res.ok) {
        const text = await res.text();
        throw new Error(text || "Khong the xu ly giao dich.");
      }

      setSelectedId(null);
      setDetail(null);
      setNote("");
      fetchItems();
    } catch (err) {
      const message = err instanceof Error ? err.message : "Khong the xu ly giao dich.";
      alert(message);
    } finally {
      setActionLoading(false);
    }
  };

  return (
    <AdminShell
      title="Giao dich lon cho duyet"
      subtitle="Kiem tra va phe duyet cac giao dich vuot nguong."
    >
      <div className="flex flex-wrap items-center gap-3">
        <div className="flex flex-1 items-center gap-2 rounded-xl border border-black/10 bg-white px-4 py-2">
          <span className="text-zinc-400">?</span>
          <input
            className="w-full text-sm outline-none"
            placeholder="Tim theo ma GD, nguoi gui, nguoi nhan..."
            value={search}
            onChange={(event) => setSearch(event.target.value)}
          />
        </div>

        <select
          className="h-10 rounded-xl border border-black/10 bg-white px-3 text-sm"
          value={riskFilter}
          onChange={(event) => setRiskFilter(event.target.value)}
        >
          {RISK_OPTIONS.map((option) => (
            <option key={option.value} value={option.value}>
              {option.label}
            </option>
          ))}
        </select>

        <select
          className="h-10 rounded-xl border border-black/10 bg-white px-3 text-sm"
          value={statusFilter}
          onChange={(event) => setStatusFilter(event.target.value)}
        >
          {STATUS_OPTIONS.map((option) => (
            <option key={option.value} value={option.value}>
              {option.label}
            </option>
          ))}
        </select>
      </div>

      <div className="grid gap-4 rounded-2xl border border-black/5 bg-rose-50/50 p-4 shadow-sm md:grid-cols-4">
        <div>
          <div className="text-xs text-zinc-500">Tong so GD cho duyet</div>
          <div className="mt-1 text-2xl font-semibold text-zinc-900">{summary.total}</div>
        </div>
        <div>
          <div className="text-xs text-zinc-500">Rui ro cao</div>
          <div className="mt-1 text-2xl font-semibold text-rose-600">{summary.highRisk}</div>
        </div>
        <div>
          <div className="text-xs text-zinc-500">Cho quan ly duyet</div>
          <div className="mt-1 text-2xl font-semibold text-orange-600">
            {summary.pendingManager}
          </div>
        </div>
        <div>
          <div className="text-xs text-zinc-500">Tong gia tri cho duyet</div>
          <div className="mt-1 text-2xl font-semibold text-blue-600">
            {formatCurrency(summary.totalAmount, "VND")}
          </div>
        </div>
      </div>

      <div className="overflow-hidden rounded-2xl border border-black/5 bg-white shadow-sm">
        <table className="w-full text-left text-sm">
          <thead className="bg-zinc-50/50 text-xs font-semibold uppercase tracking-wider text-zinc-500">
            <tr>
              <th className="px-6 py-4">Ma GD</th>
              <th className="px-6 py-4">Nguoi gui</th>
              <th className="px-6 py-4">Nguoi nhan</th>
              <th className="px-6 py-4">So tien</th>
              <th className="px-6 py-4">Muc rui ro</th>
              <th className="px-6 py-4">Thoi gian</th>
              <th className="px-6 py-4">Trang thai</th>
              <th className="px-6 py-4 text-right">Thao tac</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-black/5">
            {loading ? (
              <tr>
                <td colSpan={8} className="px-6 py-10 text-center text-zinc-400">
                  Dang tai giao dich...
                </td>
              </tr>
            ) : error ? (
              <tr>
                <td colSpan={8} className="px-6 py-10 text-center text-rose-600">
                  {error}
                </td>
              </tr>
            ) : items.length === 0 ? (
              <tr>
                <td colSpan={8} className="px-6 py-10 text-center text-zinc-400">
                  Khong co giao dich nao.
                </td>
              </tr>
            ) : (
              items.map((item) => (
                <tr key={item.id} className="hover:bg-zinc-50/60">
                  <td className="px-6 py-4 font-semibold text-blue-600">
                    {item.transactionCode}
                  </td>
                  <td className="px-6 py-4">
                    <div className="font-medium">{item.fromName}</div>
                    <div className="text-xs text-zinc-400">{item.fromAccountNumber}</div>
                  </td>
                  <td className="px-6 py-4">
                    <div className="font-medium">{item.toName}</div>
                    <div className="text-xs text-zinc-400">{item.toAccountNumber}</div>
                  </td>
                  <td className="px-6 py-4 font-semibold">
                    {formatCurrency(item.amount, item.currency)}
                  </td>
                  <td className="px-6 py-4">
                    <span
                      className={`inline-flex items-center rounded-full px-2 py-0.5 text-xs font-semibold ${
                        RISK_STYLES[item.riskLevel] ?? "bg-zinc-50 text-zinc-600"
                      }`}
                    >
                      {RISK_LABELS[item.riskLevel] ?? item.riskLevel}
                    </span>
                  </td>
                  <td className="px-6 py-4 text-zinc-500">
                    {new Date(item.createdAt).toLocaleString("vi-VN")}
                  </td>
                  <td className="px-6 py-4">
                    <span
                      className={`rounded-full px-2 py-0.5 text-xs font-semibold ${
                        STATUS_STYLES[item.reviewStatus] ?? "bg-zinc-50 text-zinc-600"
                      }`}
                    >
                      {STATUS_LABELS[item.reviewStatus] ?? item.reviewStatus}
                    </span>
                  </td>
                  <td className="px-6 py-4 text-right">
                    <button
                      onClick={() => setSelectedId(item.id)}
                      className="rounded-lg border border-black/5 bg-blue-50 px-3 py-1.5 text-xs font-semibold text-blue-600 hover:bg-blue-100"
                    >
                      Xem chi tiet
                    </button>
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>

      {selectedId && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40 p-4 backdrop-blur-sm">
          <div className="w-full max-w-3xl rounded-3xl bg-white p-8 shadow-2xl">
            <div className="mb-6 flex items-center justify-between">
              <div>
                <div className="text-xs font-semibold uppercase text-zinc-400">Chi tiet giao dich</div>
                <div className="text-xl font-semibold text-zinc-900">
                  {detail?.transactionCode ?? "Dang tai..."}
                </div>
              </div>
              <button
                onClick={() => setSelectedId(null)}
                className="text-2xl text-zinc-400 hover:text-zinc-800"
              >
                x
              </button>
            </div>

            {detail ? (
              <div className="grid gap-6 md:grid-cols-2">
                <div className="space-y-4">
                  <div>
                    <div className="text-xs font-semibold uppercase text-zinc-400">Nguoi gui</div>
                    <div className="mt-1 text-sm font-semibold">{detail.fromName}</div>
                    <div className="text-xs text-zinc-500">{detail.fromAccountNumber}</div>
                  </div>
                  <div>
                    <div className="text-xs font-semibold uppercase text-zinc-400">Nguoi nhan</div>
                    <div className="mt-1 text-sm font-semibold">{detail.toName}</div>
                    <div className="text-xs text-zinc-500">{detail.toAccountNumber}</div>
                  </div>
                  <div>
                    <div className="text-xs font-semibold uppercase text-zinc-400">Gia tri giao dich</div>
                    <div className="mt-1 text-lg font-semibold text-blue-600">
                      {formatCurrency(detail.amount, detail.currency)}
                    </div>
                    <div className="text-xs text-zinc-500">
                      Phi: {formatCurrency(detail.feeAmount, detail.currency)}
                    </div>
                  </div>
                  <div>
                    <div className="text-xs font-semibold uppercase text-zinc-400">Rui ro</div>
                    <span
                      className={`mt-1 inline-flex items-center rounded-full px-2 py-0.5 text-xs font-semibold ${
                        RISK_STYLES[detail.riskLevel] ?? "bg-zinc-50 text-zinc-600"
                      }`}
                    >
                      {RISK_LABELS[detail.riskLevel] ?? detail.riskLevel}
                    </span>
                  </div>
                </div>

                <div className="space-y-4">
                  <div>
                    <div className="text-xs font-semibold uppercase text-zinc-400">Trang thai</div>
                    <span
                      className={`mt-1 inline-flex items-center rounded-full px-2 py-0.5 text-xs font-semibold ${
                        STATUS_STYLES[detail.reviewStatus] ?? "bg-zinc-50 text-zinc-600"
                      }`}
                    >
                      {STATUS_LABELS[detail.reviewStatus] ?? detail.reviewStatus}
                    </span>
                  </div>
                  <div>
                    <div className="text-xs font-semibold uppercase text-zinc-400">Thoi gian</div>
                    <div className="mt-1 text-sm text-zinc-600">
                      Tao: {new Date(detail.createdAt).toLocaleString("vi-VN")}
                    </div>
                    {detail.completedAt ? (
                      <div className="text-sm text-zinc-600">
                        Hoan tat: {new Date(detail.completedAt).toLocaleString("vi-VN")}
                      </div>
                    ) : null}
                  </div>
                  <div>
                    <div className="text-xs font-semibold uppercase text-zinc-400">Ghi chu</div>
                    <p className="mt-1 text-sm text-zinc-600">
                      {detail.description || "Khong co"}
                    </p>
                  </div>
                  <div>
                    <div className="text-xs font-semibold uppercase text-zinc-400">Ly do quyet dinh</div>
                    <textarea
                      className="mt-2 w-full rounded-xl border border-black/10 p-3 text-sm outline-none focus:border-blue-400"
                      placeholder="Nhap ghi chu (neu can)..."
                      value={note}
                      onChange={(event) => setNote(event.target.value)}
                    />
                  </div>
                </div>
              </div>
            ) : (
              <div className="text-center text-sm text-zinc-400">Dang tai chi tiet...</div>
            )}

            <div className="mt-8 flex justify-end gap-3 border-t border-black/5 pt-6">
              <button
                onClick={() => handleDecision("reject")}
                disabled={actionLoading || !canDecide}
                className="rounded-xl border border-red-200 px-5 py-2 text-sm font-semibold text-red-600 hover:bg-red-50 disabled:opacity-50"
              >
                Tu choi
              </button>
              <button
                onClick={() => handleDecision("approve")}
                disabled={actionLoading || !canDecide}
                className="rounded-xl bg-emerald-600 px-5 py-2 text-sm font-semibold text-white hover:bg-emerald-700 disabled:opacity-50"
              >
                Phe duyet
              </button>
            </div>
          </div>
        </div>
      )}
    </AdminShell>
  );
}
