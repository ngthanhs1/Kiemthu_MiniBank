"use client";

import { useEffect, useMemo, useState } from "react";
import AdminShell from "../../components/admin-shell";
import { Search, Filter, Eye, Check, X } from "lucide-react";

interface LoanItem {
    id: number;
    requestCode: string;
    customerName: string;
    userPhone: string;
    productName: string;
    requestedAmount: number;
    termMonths: number;
    purpose: string;
    priorityTag: string | null;
    status: string;
    submittedAt: string;
}

const API_BASE = process.env.NEXT_PUBLIC_API_URL ?? process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8080";

async function apiFetch<T>(path: string, options?: RequestInit): Promise<T> {
    const token =
        typeof window !== "undefined"
            ? localStorage.getItem("adminToken") ||
            localStorage.getItem("token") ||
            localStorage.getItem("accessToken")
            : null;
    const res = await fetch(`${API_BASE}${path}`, {
        ...options,
        headers: {
            "Content-Type": "application/json",
            ...(token ? { Authorization: `Bearer ${token}` } : {}),
            ...options?.headers,
        },
    });
    if (!res.ok) throw new Error((await res.text()) || `HTTP ${res.status}`);
    return res.json();
}

const formatCurrency = (n: number) =>
    new Intl.NumberFormat("vi-VN", { style: "currency", currency: "VND" }).format(n);

const formatDate = (iso: string) => {
    if (!iso) return "";
    const d = new Date(iso);
    return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, "0")}-${String(d.getDate()).padStart(2, "0")} ${String(d.getHours()).padStart(2, "0")}:${String(d.getMinutes()).padStart(2, "0")}`;
};

function statusBadge(status: string): { label: string; className: string } {
    switch (status?.toLowerCase()) {
        case "pending": return { label: "Chờ xử lý", className: "bg-yellow-100 text-yellow-700" };
        case "approved": return { label: "Đã duyệt", className: "bg-green-100 text-green-700" };
        case "rejected": return { label: "Từ chối", className: "bg-red-100 text-red-700" };
        default: return { label: status ?? "—", className: "bg-zinc-100 text-zinc-600" };
    }
}

export default function LoanApprovalPage() {
    const [items, setItems] = useState<LoanItem[]>([]);
    const [loading, setLoading] = useState(true);
    const [searchQuery, setSearchQuery] = useState("");
    const [statusFilter, setStatusFilter] = useState("all");
    const [selected, setSelected] = useState<LoanItem | null>(null);
    const [modalType, setModalType] = useState<"approve" | "reject" | null>(null);
    const [rejectReason, setRejectReason] = useState("");
    const [submitting, setSubmitting] = useState(false);

    async function fetchItems() {
        setLoading(true);
        try {
            const [r1, r2, r3] = await Promise.allSettled([
                apiFetch<any[]>("/api/admin/approvals/loan-applications?status=pending"),
                apiFetch<any[]>("/api/admin/approvals/loan-applications?status=approved"),
                apiFetch<any[]>("/api/admin/approvals/loan-applications?status=rejected"),
            ]);
            const all: any[] = [];
            for (const r of [r1, r2, r3]) {
                if (r.status === "fulfilled" && Array.isArray(r.value)) all.push(...r.value);
            }
            setItems(
                all.map((item) => ({
                    id: item.id,
                    requestCode: `LA${String(item.id).padStart(3, "0")}`,
                    customerName: item.userFullName ?? "Khách hàng",
                    userPhone: item.userPhone ?? "",
                    productName: item.productName ?? "Vay tiêu dùng",
                    requestedAmount: Number(item.requestedAmount) || 0,
                    termMonths: item.termMonths ?? item.requestedTermMonths ?? 0,
                    purpose: item.purpose ?? "",
                    priorityTag: item.priorityTag ?? null,
                    status: item.status ?? "pending",
                    submittedAt: item.submittedAt ?? "",
                })).sort((a, b) => new Date(b.submittedAt).getTime() - new Date(a.submittedAt).getTime())
            );
        } catch (err) {
            console.error("FETCH ERROR =", err);
        } finally {
            setLoading(false);
        }
    }

    useEffect(() => { fetchItems(); }, []);

    const filtered = useMemo(() =>
        items.filter((item) => {
            const matchSearch =
                item.customerName.toLowerCase().includes(searchQuery.toLowerCase()) ||
                item.requestCode.toLowerCase().includes(searchQuery.toLowerCase()) ||
                item.productName.toLowerCase().includes(searchQuery.toLowerCase());
            const matchStatus = statusFilter === "all" || item.status === statusFilter;
            return matchSearch && matchStatus;
        }),
        [items, searchQuery, statusFilter]
    );

    async function handleDecision() {
        if (!selected || !modalType) return;
        setSubmitting(true);
        try {
            if (modalType === "approve") {
                await apiFetch(`/api/admin/approvals/loan-applications/${selected.id}/approve`, {
                    method: "POST",
                    body: JSON.stringify({}),
                });
            } else {
                await apiFetch(`/api/admin/approvals/loan-applications/${selected.id}/reject`, {
                    method: "POST",
                    body: JSON.stringify({ reason: rejectReason }),
                });
            }
            setModalType(null);
            setSelected(null);
            setRejectReason("");
            fetchItems();
        } catch (err) {
            console.error("DECISION ERROR =", err);
        } finally {
            setSubmitting(false);
        }
    }

    const pendingCount = items.filter((r) => r.status === "pending").length;
    const approvedCount = items.filter((r) => r.status === "approved").length;
    const highCount = items.filter((r) => r.priorityTag === "high").length;

    return (
        <AdminShell
            title="Duyệt vay vốn"
            subtitle="Xem xét và phê duyệt yêu cầu vay vốn của khách hàng"
        >
            <section className="overflow-hidden rounded-2xl border border-black/5 bg-white shadow-sm">

                {/* FILTER */}
                <div className="flex items-center gap-4 border-b border-zinc-200 p-5">
                    <div className="relative flex-1">
                        <Search size={18} className="absolute left-4 top-1/2 -translate-y-1/2 text-zinc-400" />
                        <input
                            type="text"
                            placeholder="Tìm theo tên KH, mã yêu cầu, sản phẩm..."
                            value={searchQuery}
                            onChange={(e) => setSearchQuery(e.target.value)}
                            className="h-11 w-full rounded-xl border border-zinc-200 bg-white pl-11 pr-4 text-sm outline-none focus:ring-2 focus:ring-blue-500"
                        />
                    </div>
                    <button className="flex h-11 w-11 items-center justify-center rounded-xl border border-zinc-200 hover:bg-zinc-50">
                        <Filter size={18} className="text-zinc-500" />
                    </button>
                    <select
                        value={statusFilter}
                        onChange={(e) => setStatusFilter(e.target.value)}
                        className="h-11 rounded-xl border border-zinc-200 px-4 text-sm outline-none"
                    >
                        <option value="all">Tất cả trạng thái</option>
                        <option value="pending">Chờ xử lý</option>
                        <option value="approved">Đã duyệt</option>
                        <option value="rejected">Từ chối</option>
                    </select>
                </div>

                {/* STATS */}
                <div className="border-b border-zinc-200 bg-zinc-50 px-5 py-5">
                    <div className="flex items-center gap-6">
                        <div className="flex-1 min-w-0">
                            <p className="text-sm text-zinc-500">Tổng yêu cầu</p>
                            <p className="mt-1 text-[36px] font-semibold leading-none text-zinc-900">{items.length}</p>
                        </div>
                        <div className="h-14 w-px bg-zinc-200" />
                        <div className="flex-1 min-w-0">
                            <p className="text-sm text-zinc-500">Chờ xử lý</p>
                            <p className="mt-1 text-[36px] font-semibold leading-none text-amber-500">{pendingCount}</p>
                        </div>
                        <div className="h-14 w-px bg-zinc-200" />
                        <div className="flex-1 min-w-0">
                            <p className="text-sm text-zinc-500">Đang xử lý</p>
                            <p className="mt-1 text-[36px] font-semibold leading-none text-blue-500">{approvedCount}</p>
                        </div>
                        <div className="h-14 w-px bg-zinc-200" />
                        <div className="flex-1 min-w-0">
                            <p className="text-sm text-zinc-500">Ưu tiên cao</p>
                            <p className="mt-1 text-[36px] font-semibold leading-none text-red-500">{highCount}</p>
                        </div>
                    </div>
                </div>

                {/* TABLE */}
                <div className="overflow-x-auto">
                    <table className="w-full">
                        <thead className="border-b border-zinc-200 bg-white">
                            <tr>
                                <th className="px-5 py-4 text-left text-sm font-medium text-zinc-600">Mã YC</th>
                                <th className="px-5 py-4 text-left text-sm font-medium text-zinc-600">Khách hàng</th>
                                <th className="px-5 py-4 text-left text-sm font-medium text-zinc-600">Loại yêu cầu</th>
                                <th className="px-5 py-4 text-left text-sm font-medium text-zinc-600">Mô tả</th>
                                <th className="px-5 py-4 text-left text-sm font-medium text-zinc-600">Ngày gửi</th>
                                <th className="px-5 py-4 text-left text-sm font-medium text-zinc-600">Trạng thái</th>
                                <th className="px-5 py-4 text-left text-sm font-medium text-zinc-600">Thao tác</th>
                            </tr>
                        </thead>
                        <tbody>
                            {loading ? (
                                <tr><td colSpan={7} className="py-10 text-center text-sm text-zinc-400">Đang tải...</td></tr>
                            ) : filtered.length === 0 ? (
                                <tr><td colSpan={7} className="py-10 text-center text-sm text-zinc-400">Không có dữ liệu</td></tr>
                            ) : filtered.map((item) => {
                                const s = statusBadge(item.status);
                                const canAct = item.status === "pending";
                                return (
                                    <tr key={item.id} className="border-b border-zinc-100 hover:bg-zinc-50">
                                        <td className="px-5 py-5">
                                            <div className="flex items-center gap-2">
                                                {item.priorityTag === "high" && <span className="text-red-500">★</span>}
                                                <span className="font-semibold text-zinc-900">{item.requestCode}</span>
                                            </div>
                                        </td>
                                        <td className="px-5 py-5">
                                            <div className="flex items-center gap-3">
                                                <div className="flex h-10 w-10 items-center justify-center rounded-full bg-violet-100 text-sm font-semibold text-violet-700">
                                                    {item.customerName.charAt(0)}
                                                </div>
                                                <div>
                                                    <p className="font-medium text-zinc-900">{item.customerName}</p>
                                                    <p className="text-xs text-zinc-400">{item.userPhone}</p>
                                                </div>
                                            </div>
                                        </td>
                                        <td className="px-5 py-5">
                                            <span className="inline-flex rounded-full bg-violet-100 px-3 py-1.5 text-sm font-medium text-violet-700">
                                                Duyệt vay vốn
                                            </span>
                                        </td>
                                        <td className="px-5 py-5 max-w-xs">
                                            <p className="truncate text-sm text-zinc-600">
                                                {item.productName} — {formatCurrency(item.requestedAmount)} — {item.termMonths} tháng
                                            </p>
                                        </td>
                                        <td className="px-5 py-5 text-sm text-zinc-600 whitespace-nowrap">
                                            {formatDate(item.submittedAt)}
                                        </td>
                                        <td className="px-5 py-5">
                                            <span className={`inline-flex rounded-full px-4 py-1.5 text-sm font-medium ${s.className}`}>
                                                {s.label}
                                            </span>
                                        </td>
                                        <td className="px-5 py-5">
                                            <div className="flex items-center gap-2">
                                                {canAct && (
                                                    <>
                                                        <button
                                                            onClick={() => { setSelected(item); setModalType("approve"); setRejectReason(""); }}
                                                            className="inline-flex items-center gap-1.5 rounded-xl bg-green-50 px-3 py-2 text-sm font-medium text-green-700 hover:bg-green-100"
                                                        >
                                                            <Check size={15} />Duyệt
                                                        </button>
                                                        <button
                                                            onClick={() => { setSelected(item); setModalType("reject"); setRejectReason(""); }}
                                                            className="inline-flex items-center gap-1.5 rounded-xl bg-red-50 px-3 py-2 text-sm font-medium text-red-600 hover:bg-red-100"
                                                        >
                                                            <X size={15} />Từ chối
                                                        </button>
                                                    </>
                                                )}
                                                <button className="inline-flex items-center gap-1.5 rounded-xl bg-blue-50 px-3 py-2 text-sm font-medium text-blue-600 hover:bg-blue-100">
                                                    <Eye size={15} />Chi tiết
                                                </button>
                                            </div>
                                        </td>
                                    </tr>
                                );
                            })}
                        </tbody>
                    </table>
                </div>
            </section>

            {selected && modalType && (
                <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40">
                    <div className="w-full max-w-md rounded-2xl bg-white p-6 shadow-xl">
                        <h3 className="text-lg font-semibold text-zinc-900">
                            {modalType === "approve" ? "Xác nhận duyệt vay vốn" : "Xác nhận từ chối"}
                        </h3>
                        <p className="mt-1 text-sm text-zinc-500">
                            Mã: <span className="font-medium text-zinc-700">{selected.requestCode}</span> — {selected.customerName}
                        </p>
                        <p className="mt-1 text-sm text-zinc-500">
                            Số tiền: <span className="font-medium text-zinc-700">{formatCurrency(selected.requestedAmount)}</span> — {selected.termMonths} tháng
                        </p>
                        {modalType === "reject" && (
                            <textarea
                                value={rejectReason}
                                onChange={(e) => setRejectReason(e.target.value)}
                                placeholder="Lý do từ chối (tuỳ chọn)"
                                rows={3}
                                className="mt-4 w-full rounded-xl border border-zinc-200 px-4 py-3 text-sm outline-none focus:ring-2 focus:ring-blue-500"
                            />
                        )}
                        <div className="mt-4 flex justify-end gap-3">
                            <button
                                onClick={() => { setModalType(null); setSelected(null); }}
                                className="rounded-xl border border-zinc-200 px-5 py-2.5 text-sm font-medium text-zinc-700 hover:bg-zinc-50"
                            >
                                Huỷ
                            </button>
                            <button
                                onClick={handleDecision}
                                disabled={submitting}
                                className={`rounded-xl px-5 py-2.5 text-sm font-medium text-white disabled:opacity-60 ${modalType === "approve" ? "bg-green-600 hover:bg-green-700" : "bg-red-600 hover:bg-red-700"}`}
                            >
                                {submitting ? "Đang xử lý..." : modalType === "approve" ? "Xác nhận duyệt" : "Xác nhận từ chối"}
                            </button>
                        </div>
                    </div>
                </div>
            )}
        </AdminShell>
    );
}