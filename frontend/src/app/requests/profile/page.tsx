"use client";

import React, { useEffect, useState, useCallback } from "react";
import { Eye } from "lucide-react";
import { useRouter } from "next/navigation";

import AdminShell from "../../components/admin-shell";

import {
    fetchServiceRequests,
    ServiceRequestSummary,
} from "../../../lib/api/service-requests";

import { StatusBadge } from "../../components/requests/StatusBadge";

function formatDate(isoStr: string) {
    const d = new Date(isoStr);

    return d.toLocaleString("vi-VN", {
        year: "numeric",
        month: "2-digit",
        day: "2-digit",
        hour: "2-digit",
        minute: "2-digit",
    });
}

function getInitial(name: string) {
    const parts = name?.trim().split(" ") ?? [];

    return (
        parts[parts.length - 1]
            ?.charAt(0)
            ?.toUpperCase() ?? "?"
    );
}

export default function ProfileRequestsPage() {
    const router = useRouter();

    const [requests, setRequests] = useState<
        ServiceRequestSummary[]
    >([]);

    const [loading, setLoading] = useState(true);

    const [error, setError] = useState("");

    const load = useCallback(async () => {
        setLoading(true);
        setError("");

        try {
            const data = await fetchServiceRequests(
                undefined,
                "profile_change"
            );

            setRequests(data);
        } catch (e: unknown) {
            setError(
                e instanceof Error
                    ? e.message
                    : "Lỗi không xác định"
            );
        } finally {
            setLoading(false);
        }
    }, []);

    useEffect(() => {
        load();
    }, [load]);

    return (
        <AdminShell
            title="Yêu cầu đổi thông tin"
            subtitle="Quản lý yêu cầu cập nhật thông tin cá nhân từ khách hàng"
        >
            <div className="min-h-screen bg-gray-50">
                <div className="bg-white rounded-xl border border-gray-200 overflow-hidden">
                    {loading ? (
                        <div className="flex items-center justify-center py-16 text-gray-400 text-sm">
                            Đang tải...
                        </div>
                    ) : error ? (
                        <div className="flex items-center justify-center py-16 text-red-500 text-sm">
                            {error}
                        </div>
                    ) : requests.length === 0 ? (
                        <div className="flex items-center justify-center py-16 text-gray-400 text-sm">
                            Không có yêu cầu đổi thông tin nào
                        </div>
                    ) : (
                        <table className="w-full text-sm">
                            <thead>
                                <tr className="border-b border-gray-100 bg-gray-50">
                                    <th className="text-left px-6 py-3 text-xs font-semibold text-gray-500 uppercase">
                                        Mã YC
                                    </th>

                                    <th className="text-left px-6 py-3 text-xs font-semibold text-gray-500 uppercase">
                                        Khách hàng
                                    </th>

                                    <th className="text-left px-6 py-3 text-xs font-semibold text-gray-500 uppercase">
                                        Mô tả
                                    </th>

                                    <th className="text-left px-6 py-3 text-xs font-semibold text-gray-500 uppercase">
                                        Ngày gửi
                                    </th>

                                    <th className="text-left px-6 py-3 text-xs font-semibold text-gray-500 uppercase">
                                        Trạng thái
                                    </th>

                                    <th className="text-left px-6 py-3 text-xs font-semibold text-gray-500 uppercase">
                                        Thao tác
                                    </th>
                                </tr>
                            </thead>

                            <tbody>
                                {requests.map((req) => (
                                    <tr
                                        key={req.id}
                                        className="border-b border-gray-50 hover:bg-gray-50 transition-colors"
                                    >
                                        <td className="px-6 py-4 font-semibold text-gray-800">
                                            SR
                                            {String(req.id).padStart(
                                                3,
                                                "0"
                                            )}
                                        </td>

                                        <td className="px-6 py-4">
                                            <div className="flex items-center gap-2">
                                                <span className="w-7 h-7 rounded-full bg-teal-500 text-white text-xs font-bold flex items-center justify-center flex-shrink-0">
                                                    {getInitial(
                                                        req.userName || ""
                                                    )}
                                                </span>

                                                <div>
                                                    <div className="font-medium text-gray-800">
                                                        {req.userName}
                                                    </div>

                                                    <div className="text-xs text-gray-400">
                                                        {req.userPhone}
                                                    </div>
                                                </div>
                                            </div>
                                        </td>

                                        <td className="px-6 py-4 text-gray-600 max-w-xs truncate">
                                            {req.title || "—"}
                                        </td>

                                        <td className="px-6 py-4 text-gray-500">
                                            {formatDate(
                                                req.submittedAt
                                            )}
                                        </td>

                                        <td className="px-6 py-4">
                                            <StatusBadge
                                                status={req.status}
                                            />
                                        </td>

                                        <td className="px-6 py-4">
                                            <button
                                                onClick={() =>
                                                    router.push(
                                                        `/requests/profile/${req.id}`
                                                    )
                                                }
                                                className="flex items-center gap-1 text-blue-600 hover:text-blue-800 text-sm font-medium transition-colors"
                                            >
                                                <Eye className="w-4 h-4" />
                                                Chi tiết
                                            </button>
                                        </td>
                                    </tr>
                                ))}
                            </tbody>
                        </table>
                    )}
                </div>
            </div>
        </AdminShell>
    );
}