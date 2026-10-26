"use client";

import { useEffect, useMemo, useState } from "react";
import AdminShell from "../../../components/admin-shell";
import {
    Search,
    Filter,
    Eye,
    Check,
    X,
    AlertCircle,
    Clock,
    User,
} from "lucide-react";

interface SavingClosureRequest {
    id: number;
    requestCode: string;
    customerId: string;
    customerName: string;
    savingCode: string;
    productName: string;
    principalAmount: number;
    interestRate: number;
    openDate: string;
    maturityDate: string;
    closureType: "on_time" | "early";
    estimatedInterest: number;
    penaltyFee: number;
    totalAmount: number;
    status:
    | "PENDING"
    | "STAFF_REVIEWING"
    | "WAITING_MANAGER_APPROVAL"
    | "APPROVED"
    | "REJECTED";
    submittedAt: string;
    reviewedBy?: string;
    reviewNotes?: string;
    requiresManagerApproval: boolean;
}

const API_BASE = (
    process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8080"
).replace(/\/+$/, "");

async function apiFetch<T>(
    path: string,
    options?: RequestInit
): Promise<T> {
    const token =
        typeof window !== "undefined"
            ? localStorage.getItem("adminToken")
            : null;

    const res = await fetch(`${API_BASE}${path}`, {
        ...options,
        headers: {
            "Content-Type": "application/json",
            ...(token
                ? {
                    Authorization: `Bearer ${token}`,
                }
                : {}),
            ...options?.headers,
        },
    });

    if (!res.ok) {
        throw new Error(
            (await res.text()) || `HTTP ${res.status}`
        );
    }

    return res.json();
}

const formatCurrency = (amount: number) =>
    new Intl.NumberFormat("vi-VN", {
        style: "currency",
        currency: "VND",
    }).format(amount);

function mapStatus(
    beStatus: string
): SavingClosureRequest["status"] {
    switch (beStatus) {
        case "SUBMITTED":
        case "closure_requested":
            return "PENDING";

        case "IN_REVIEW":
        case "reviewing":
            return "STAFF_REVIEWING";

        case "MANAGER_REVIEW":
        case "manager_approval":
            return "WAITING_MANAGER_APPROVAL";

        case "APPROVED":
            return "APPROVED";

        case "REJECTED":
            return "REJECTED";

        default:
            return "PENDING";
    }
}

export default function SavingClosureRequestsPage() {
    const [requests, setRequests] = useState<
        SavingClosureRequest[]
    >([]);

    const [loading, setLoading] = useState(true);

    const [searchQuery, setSearchQuery] = useState("");

    const [typeFilter, setTypeFilter] = useState<
        "all" | "on_time" | "early"
    >("all");

    const [statusFilter, setStatusFilter] = useState<
        | "all"
        | "PENDING"
        | "STAFF_REVIEWING"
        | "WAITING_MANAGER_APPROVAL"
    >("all");

    const [selectedRequest, setSelectedRequest] =
        useState<SavingClosureRequest | null>(null);

    const [showDetailModal, setShowDetailModal] =
        useState(false);

    const [rejectReason, setRejectReason] = useState("");

    async function fetchRequests() {
        setLoading(true);

        try {
            const data = await apiFetch<any[]>(
                "/api/admin/financial-products/saving-settlement-requests"
            );
            console.log("SETTLEMENT REQUESTS", data);

            const mapped: SavingClosureRequest[] =
                data.map((item) => ({
                    id: item.id,

                    requestCode:
                        `REQ202605${String(item.id).padStart(
                            2,
                            "0"
                        )}`,

                    customerId: String(item.userId ?? ""),

                    customerName:
                        item.customerName ?? "",

                    savingCode: item.savingCode ?? "",

                    productName:
                        item.productName ?? item.savingProductName ?? "",

                    principalAmount:
                        item.principalAmount ?? 0,

                    interestRate:
                        item.interestRate ?? 0,

                    openDate: item.openDate
                        ? new Date(
                            item.openDate
                        ).toLocaleDateString("vi-VN")
                        : "",

                    maturityDate: item.maturityDate
                        ? new Date(
                            item.maturityDate
                        ).toLocaleDateString("vi-VN")
                        : "",

                    closureType:
                        item.closureType === "early" || item.settlementType === "early"
                            ? "early"
                            : "on_time",

                    estimatedInterest:
                        item.estimatedInterest ?? 0,

                    penaltyFee:
                        item.penaltyFee ?? 0,

                    totalAmount:
                        item.totalAmount ?? item.settlementAmount ?? 0,

                    status: mapStatus(item.status),

                    submittedAt:
                        item.submittedAt ?? "",

                    requiresManagerApproval:
                        item.status === "MANAGER_REVIEW" || item.settlementType === "early",
                }));

            setRequests(mapped);
        } catch (error) {
            console.error(error);
        } finally {
            setLoading(false);
        }
    }

    useEffect(() => {
        fetchRequests();
    }, []);

    async function decideRequest(type: "approve" | "reject") {
        if (!selectedRequest) return;
        if (type === "reject" && !rejectReason.trim()) {
            alert("Vui long nhap ly do tu choi.");
            return;
        }

        try {
            await apiFetch(
                `/api/admin/financial-products/saving-settlement-requests/${selectedRequest.id}/${type}`,
                {
                    method: "POST",
                    body: JSON.stringify({
                        note: type === "reject" ? rejectReason.trim() : undefined,
                    }),
                }
            );
            setShowDetailModal(false);
            setSelectedRequest(null);
            setRejectReason("");
            await fetchRequests();
        } catch (error) {
            alert(error instanceof Error ? error.message : "Khong the xu ly yeu cau.");
        }
    }

    const filteredRequests = useMemo(() => {
        return requests.filter((req) => {
            const matchesSearch =
                req.customerName
                    .toLowerCase()
                    .includes(
                        searchQuery.toLowerCase()
                    ) ||
                req.savingCode
                    .toLowerCase()
                    .includes(
                        searchQuery.toLowerCase()
                    ) ||
                req.requestCode
                    .toLowerCase()
                    .includes(
                        searchQuery.toLowerCase()
                    );

            const matchesType =
                typeFilter === "all" ||
                req.closureType === typeFilter;

            const matchesStatus =
                statusFilter === "all" ||
                req.status === statusFilter;

            return (
                matchesSearch &&
                matchesType &&
                matchesStatus
            );
        });
    }, [
        requests,
        searchQuery,
        typeFilter,
        statusFilter,
    ]);

    return (
        <AdminShell
            title="Yêu cầu tất toán sổ tiết kiệm"
            subtitle="Xử lý yêu cầu tất toán sổ tiết kiệm của khách hàng"
        >
            <section className="overflow-hidden rounded-2xl border border-black/5 bg-white shadow-sm">

                {/* FILTER */}
                <div className="flex items-center gap-4 border-b border-zinc-200 p-5">

                    <div className="relative flex-1">
                        <Search
                            size={18}
                            className="absolute left-4 top-1/2 -translate-y-1/2 text-zinc-400"
                        />

                        <input
                            type="text"
                            placeholder="Tìm theo tên KH, mã số, mã yêu cầu..."
                            value={searchQuery}
                            onChange={(e) =>
                                setSearchQuery(
                                    e.target.value
                                )
                            }
                            className="h-11 w-full rounded-xl border border-zinc-200 bg-white pl-11 pr-4 text-sm outline-none focus:ring-2 focus:ring-blue-500"
                        />
                    </div>

                    <button className="flex h-11 w-11 items-center justify-center rounded-xl border border-zinc-200 hover:bg-zinc-50">
                        <Filter
                            size={18}
                            className="text-zinc-500"
                        />
                    </button>

                    <select
                        value={typeFilter}
                        onChange={(e) =>
                            setTypeFilter(
                                e.target.value as any
                            )
                        }
                        className="h-11 rounded-xl border border-zinc-200 px-4 text-sm outline-none"
                    >
                        <option value="all">
                            Tất cả loại
                        </option>

                        <option value="on_time">
                            Đúng hạn
                        </option>

                        <option value="early">
                            Trước hạn
                        </option>
                    </select>

                    <select
                        value={statusFilter}
                        onChange={(e) =>
                            setStatusFilter(
                                e.target.value as any
                            )
                        }
                        className="h-11 rounded-xl border border-zinc-200 px-4 text-sm outline-none"
                    >
                        <option value="all">
                            Tất cả trạng thái
                        </option>

                        <option value="PENDING">
                            Chờ xử lý
                        </option>

                        <option value="STAFF_REVIEWING">
                            Đang kiểm tra
                        </option>

                        <option value="WAITING_MANAGER_APPROVAL">
                            Chờ quản lý duyệt
                        </option>
                    </select>
                </div>

                {/* STATS */}
                <div className="border-b border-zinc-200 bg-[#faf5ff] px-5 py-5">

                    <div className="flex items-center justify-between gap-6">

                        <div className="flex-1 min-w-0">
                            <p className="text-sm text-zinc-500 whitespace-nowrap">
                                Tổng yêu cầu
                            </p>

                            <p className="mt-2 text-[38px] font-semibold leading-none text-zinc-900">
                                {requests.length}
                            </p>
                        </div>

                        <div className="h-16 w-px bg-zinc-200" />

                        <div className="flex-1 min-w-0">
                            <p className="text-sm text-zinc-500 whitespace-nowrap">
                                Chờ xử lý
                            </p>

                            <p className="mt-2 text-[38px] font-semibold leading-none text-amber-500">
                                {
                                    requests.filter(
                                        (r) =>
                                            r.status ===
                                            "PENDING"
                                    ).length
                                }
                            </p>
                        </div>

                        <div className="h-16 w-px bg-zinc-200" />

                        <div className="flex-1 min-w-0">
                            <p className="text-sm text-zinc-500 whitespace-nowrap">
                                Tất toán trước hạn
                            </p>

                            <p className="mt-2 text-[38px] font-semibold leading-none text-orange-500">
                                {
                                    requests.filter(
                                        (r) =>
                                            r.closureType ===
                                            "early"
                                    ).length
                                }
                            </p>
                        </div>

                        <div className="h-16 w-px bg-zinc-200" />

                        <div className="flex-1 min-w-0">
                            <p className="text-sm text-zinc-500 whitespace-nowrap">
                                Cần quản lý duyệt
                            </p>

                            <p className="mt-2 text-[38px] font-semibold leading-none text-red-500">
                                {
                                    requests.filter(
                                        (r) =>
                                            r.requiresManagerApproval
                                    ).length
                                }
                            </p>
                        </div>

                        <div className="h-16 w-px bg-zinc-200" />

                        <div className="flex-[1.4] min-w-0">
                            <p className="text-sm text-zinc-500 whitespace-nowrap">
                                Tổng giá trị
                            </p>

                            <p className="mt-2 truncate text-[38px] font-semibold leading-none text-violet-600">
                                {formatCurrency(
                                    requests.reduce(
                                        (sum, r) =>
                                            sum +
                                            r.totalAmount,
                                        0
                                    )
                                )}
                            </p>
                        </div>

                    </div>
                </div>

                {/* TABLE */}
                <div className="overflow-x-auto">

                    <table className="w-full">

                        <thead className="border-b border-zinc-200 bg-white">
                            <tr>

                                <th className="px-5 py-4 text-left text-sm font-medium text-zinc-600">
                                    Mã YC
                                </th>

                                <th className="px-5 py-4 text-left text-sm font-medium text-zinc-600">
                                    Khách hàng
                                </th>

                                <th className="px-5 py-4 text-left text-sm font-medium text-zinc-600">
                                    Mã số
                                </th>

                                <th className="px-5 py-4 text-left text-sm font-medium text-zinc-600">
                                    Số tiền gốc
                                </th>

                                <th className="px-5 py-4 text-left text-sm font-medium text-zinc-600">
                                    Lãi tạm tính
                                </th>

                                <th className="px-5 py-4 text-left text-sm font-medium text-zinc-600">
                                    Loại TT
                                </th>

                                <th className="px-5 py-4 text-left text-sm font-medium text-zinc-600">
                                    Trạng thái
                                </th>

                                <th className="px-5 py-4 text-left text-sm font-medium text-zinc-600">
                                    Thao tác
                                </th>
                            </tr>
                        </thead>

                        <tbody>

                            {loading ? (
                                <tr>
                                    <td
                                        colSpan={8}
                                        className="py-10 text-center text-sm text-zinc-400"
                                    >
                                        Đang tải...
                                    </td>
                                </tr>
                            ) : filteredRequests.length === 0 ? (
                                <tr>
                                    <td
                                        colSpan={8}
                                        className="py-10 text-center text-sm text-zinc-400"
                                    >
                                        Không có dữ liệu
                                    </td>
                                </tr>
                            ) : (
                                filteredRequests.map(
                                    (req) => (
                                        <tr
                                            key={req.id}
                                            className="border-b border-zinc-100 hover:bg-zinc-50"
                                        >

                                            <td className="px-5 py-5 text-[15px] font-medium text-blue-600">
                                                {
                                                    req.requestCode
                                                }
                                            </td>

                                            <td className="px-5 py-5">

                                                <div className="flex items-center gap-3">

                                                    <div className="flex h-11 w-11 items-center justify-center rounded-full bg-violet-100 text-sm font-semibold text-violet-700">
                                                        {req.customerName.charAt(
                                                            0
                                                        )}
                                                    </div>

                                                    <div>
                                                        <p className="text-[15px] font-medium text-zinc-900">
                                                            {
                                                                req.customerName
                                                            }
                                                        </p>
                                                    </div>
                                                </div>
                                            </td>

                                            <td className="px-5 py-5 text-[15px] font-semibold text-zinc-900">
                                                {
                                                    req.savingCode
                                                }
                                            </td>

                                            <td className="px-5 py-5 text-[15px] font-semibold text-zinc-900">
                                                {formatCurrency(
                                                    req.principalAmount
                                                )}
                                            </td>

                                            <td className="px-5 py-5 text-[15px] font-semibold text-emerald-600">
                                                {formatCurrency(
                                                    req.estimatedInterest
                                                )}
                                            </td>

                                            <td className="px-5 py-5">

                                                <span
                                                    className={`inline-flex rounded-full px-4 py-2 text-sm font-medium ${req.closureType ===
                                                        "on_time"
                                                        ? "bg-emerald-100 text-emerald-700"
                                                        : "bg-orange-100 text-orange-600"
                                                        }`}
                                                >
                                                    {req.closureType ===
                                                        "on_time"
                                                        ? "Đúng hạn"
                                                        : "Trước hạn"}
                                                </span>

                                            </td>

                                            <td className="px-5 py-5">

                                                <span
                                                    className={`inline-flex rounded-full px-4 py-2 text-sm font-medium ${req.status ===
                                                        "PENDING"
                                                        ? "bg-yellow-100 text-yellow-700"
                                                        : req.status ===
                                                            "STAFF_REVIEWING"
                                                            ? "bg-blue-100 text-blue-700"
                                                            : "bg-orange-100 text-orange-700"
                                                        }`}
                                                >
                                                    {req.status ===
                                                        "PENDING"
                                                        ? "Chờ xử lý"
                                                        : req.status ===
                                                            "STAFF_REVIEWING"
                                                            ? "Đang kiểm tra"
                                                            : "Chờ quản lý duyệt"}
                                                </span>

                                            </td>

                                            <td className="px-5 py-5">

                                                <button
                                                    onClick={() => {
                                                        console.log(selectedRequest);
                                                        setSelectedRequest(
                                                            req
                                                        );

                                                        setShowDetailModal(
                                                            true
                                                        );
                                                    }}
                                                    className="inline-flex items-center gap-2 rounded-xl bg-blue-50 px-4 py-2 text-sm font-medium text-blue-600 hover:bg-blue-100"
                                                >
                                                    <Eye size={16} />
                                                    Chi tiết
                                                </button>

                                            </td>
                                        </tr>
                                    )
                                )
                            )}

                        </tbody>
                    </table>
                </div>
            </section>
            {showDetailModal && selectedRequest && (

                <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40">
                    <div className="max-h-[90vh] w-[900px] overflow-y-auto rounded-2xl bg-white shadow-xl">

                        <div className="flex items-center justify-between border-b p-6">
                            <h2 className="text-2xl font-bold">
                                Chi tiết yêu cầu tất toán
                            </h2>

                            <button
                                onClick={() =>
                                    setShowDetailModal(false)
                                }
                                className="text-2xl"
                            >
                                ×
                            </button>
                        </div>

                        <div className="space-y-6 p-6">

                            <div className="grid grid-cols-2 gap-10">

                                <div>
                                    <h3 className="mb-4 text-xl font-semibold">
                                        Thông tin khách hàng
                                    </h3>

                                    <p>
                                        <b>Họ tên:</b>{" "}
                                        {selectedRequest.customerName}
                                    </p>

                                    <p>
                                        <b>Mã KH:</b>{" "}
                                        {selectedRequest.customerId}
                                    </p>
                                </div>

                                <div>
                                    <h3 className="mb-4 text-xl font-semibold">
                                        Thông tin sổ tiết kiệm
                                    </h3>

                                    <p>
                                        <b>Mã sổ:</b>{" "}
                                        {selectedRequest.savingCode}
                                    </p>

                                    <p>
                                        <b>Sản phẩm:</b>{" "}
                                        {selectedRequest.productName}
                                    </p>

                                    <p>
                                        <b>Ngày mở:</b>{" "}
                                        {selectedRequest.openDate}
                                    </p>

                                    <p>
                                        <b>Ngày đáo hạn:</b>{" "}
                                        {selectedRequest.maturityDate}
                                    </p>
                                </div>
                            </div>

                            <div className="rounded-2xl bg-violet-50 p-6">
                                <h3 className="mb-4 text-xl font-semibold">
                                    Tính toán tất toán
                                </h3>

                                <div className="space-y-3">

                                    <div className="flex justify-between">
                                        <span>Số tiền gốc</span>

                                        <span>
                                            {formatCurrency(
                                                selectedRequest.principalAmount
                                            )}
                                        </span>
                                    </div>

                                    <div className="flex justify-between">
                                        <span>Lãi tạm tính</span>

                                        <span className="text-green-600">
                                            {formatCurrency(
                                                selectedRequest.estimatedInterest
                                            )}
                                        </span>
                                    </div>

                                    <div className="flex justify-between">
                                        <span>Tổng nhận</span>

                                        <span className="font-bold text-violet-700">
                                            {formatCurrency(
                                                selectedRequest.totalAmount
                                            )}
                                        </span>
                                    </div>

                                </div>
                            </div>

                            {selectedRequest.status !== "APPROVED" && selectedRequest.status !== "REJECTED" && (
                            <div className="flex justify-end gap-4">

                                <textarea
                                    value={rejectReason}
                                    onChange={(event) => setRejectReason(event.target.value)}
                                    placeholder="Ly do tu choi (neu tu choi)"
                                    className="min-h-12 flex-1 rounded-xl border border-zinc-200 px-4 py-3 text-sm outline-none"
                                />

                                <button
                                    onClick={() => decideRequest("reject")}
                                    className="rounded-xl bg-red-50 px-6 py-3 font-medium text-red-600"
                                >
                                    Từ chối
                                </button>

                                <button
                                    onClick={() => decideRequest("approve")}
                                    className="rounded-xl bg-green-600 px-6 py-3 font-medium text-white"
                                >
                                    Phê duyệt
                                </button>

                            </div>
                            )}

                        </div>

                    </div>
                </div>
            )}
        </AdminShell>
    );
}
