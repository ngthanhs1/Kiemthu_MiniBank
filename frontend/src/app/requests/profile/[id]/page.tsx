"use client";

import React, { useCallback, useEffect, useMemo, useState } from "react";
import { ArrowLeft, Check, X } from "lucide-react";
import { useParams, useRouter } from "next/navigation";

import AdminShell from "../../../components/admin-shell";
import { StatusBadge } from "../../../components/requests/StatusBadge";
import {
    approveServiceRequest,
    fetchServiceRequestDetail,
    rejectServiceRequest,
    ServiceRequestDetail,
} from "../../../../lib/api/service-requests";

function formatDate(isoStr?: string | null) {
    if (!isoStr) return "-";
    return new Date(isoStr).toLocaleString("vi-VN", {
        year: "numeric",
        month: "2-digit",
        day: "2-digit",
        hour: "2-digit",
        minute: "2-digit",
    });
}

function prettyJson(raw?: string | null) {
    if (!raw) return null;
    try {
        return JSON.stringify(JSON.parse(raw), null, 2);
    } catch {
        return raw;
    }
}

export default function ProfileRequestDetailPage() {
    const router = useRouter();
    const params = useParams<{ id: string }>();
    const requestId = Number(params.id);

    const [detail, setDetail] = useState<ServiceRequestDetail | null>(null);
    const [loading, setLoading] = useState(true);
    const [submitting, setSubmitting] = useState(false);
    const [error, setError] = useState("");
    const [note, setNote] = useState("");

    const payload = useMemo(() => prettyJson(detail?.payloadJson), [detail?.payloadJson]);

    const load = useCallback(async () => {
        if (!Number.isFinite(requestId)) {
            setError("Mã yêu cầu không hợp lệ");
            setLoading(false);
            return;
        }

        setLoading(true);
        setError("");
        try {
            const data = await fetchServiceRequestDetail(requestId);
            setDetail(data);
        } catch (e) {
            setError(e instanceof Error ? e.message : "Không thể tải chi tiết yêu cầu");
        } finally {
            setLoading(false);
        }
    }, [requestId]);

    useEffect(() => {
        load();
    }, [load]);

    const handleDecision = async (decision: "approve" | "reject") => {
        if (!detail) return;
        if (decision === "reject" && !note.trim()) {
            alert("Vui lòng nhập lý do từ chối yêu cầu.");
            return;
        }

        setSubmitting(true);
        try {
            if (decision === "approve") {
                await approveServiceRequest(detail.id, note.trim());
            } else {
                await rejectServiceRequest(detail.id, note.trim());
            }
            await load();
            setNote("");
        } catch (e) {
            alert(e instanceof Error ? e.message : "Không thể xử lý yêu cầu");
        } finally {
            setSubmitting(false);
        }
    };

    return (
        <AdminShell
            title="Chi tiết yêu cầu đổi thông tin"
            subtitle="Xem nội dung khách hàng gửi và xử lý yêu cầu cập nhật hồ sơ"
        >
            <div className="min-h-screen bg-gray-50">
                <button
                    type="button"
                    onClick={() => router.push("/requests/profile")}
                    className="mb-4 inline-flex items-center gap-2 text-sm font-medium text-gray-600 hover:text-gray-900"
                >
                    <ArrowLeft className="h-4 w-4" />
                    Quay lại danh sách
                </button>

                {loading ? (
                    <div className="rounded-xl border border-gray-200 bg-white py-16 text-center text-sm text-gray-400">
                        Đang tải...
                    </div>
                ) : error ? (
                    <div className="rounded-xl border border-red-100 bg-white py-16 text-center text-sm text-red-500">
                        {error}
                    </div>
                ) : detail ? (
                    <div className="grid gap-4 lg:grid-cols-[1fr_360px]">
                        <section className="rounded-xl border border-gray-200 bg-white p-6">
                            <div className="mb-6 flex flex-col gap-3 border-b border-gray-100 pb-5 md:flex-row md:items-start md:justify-between">
                                <div>
                                    <p className="text-xs font-semibold uppercase tracking-wide text-gray-400">
                                        SR{String(detail.id).padStart(3, "0")}
                                    </p>
                                    <h2 className="mt-1 text-xl font-bold text-gray-900">
                                        {detail.title || "Yêu cầu đổi thông tin"}
                                    </h2>
                                </div>
                                <StatusBadge status={detail.status} />
                            </div>

                            <div className="grid gap-4 text-sm md:grid-cols-2">
                                <Info label="Khách hàng" value={detail.userName || "-"} />
                                <Info label="Số điện thoại" value={detail.userPhone || "-"} />
                                <Info label="Ngày gửi" value={formatDate(detail.submittedAt)} />
                                <Info label="Ngày xử lý" value={formatDate(detail.processedAt)} />
                            </div>

                            <div className="mt-6">
                                <h3 className="mb-2 text-sm font-semibold text-gray-700">Mô tả</h3>
                                <div className="rounded-lg border border-gray-100 bg-gray-50 p-4 text-sm leading-6 text-gray-700">
                                    {detail.description || "Không có mô tả"}
                                </div>
                            </div>

                            {payload && (
                                <div className="mt-6">
                                    <h3 className="mb-2 text-sm font-semibold text-gray-700">Dữ liệu thay đổi</h3>
                                    <pre className="overflow-auto rounded-lg border border-gray-100 bg-slate-950 p-4 text-xs leading-5 text-slate-100">
                                        {payload}
                                    </pre>
                                </div>
                            )}

                            {detail.processNote && (
                                <div className="mt-6 rounded-lg border border-amber-100 bg-amber-50 p-4 text-sm text-amber-900">
                                    <span className="font-semibold">Ghi chú xử lý: </span>
                                    {detail.processNote}
                                </div>
                            )}
                        </section>

                        <aside className="rounded-xl border border-gray-200 bg-white p-6">
                            <h3 className="text-base font-bold text-gray-900">Xử lý yêu cầu</h3>
                            <p className="mt-1 text-sm text-gray-500">
                                Nhập ghi chú nội bộ hoặc lý do từ chối trước khi cập nhật trạng thái.
                            </p>

                            <textarea
                                value={note}
                                onChange={(e) => setNote(e.target.value)}
                                rows={5}
                                className="mt-4 w-full rounded-lg border border-gray-200 p-3 text-sm outline-none focus:border-blue-400"
                                placeholder="Ghi chú xử lý..."
                            />

                            <div className="mt-4 grid grid-cols-2 gap-3">
                                <button
                                    type="button"
                                    disabled={submitting}
                                    onClick={() => handleDecision("approve")}
                                    className="inline-flex items-center justify-center gap-2 rounded-lg bg-emerald-600 px-4 py-2 text-sm font-semibold text-white hover:bg-emerald-700 disabled:opacity-60"
                                >
                                    <Check className="h-4 w-4" />
                                    Duyệt
                                </button>
                                <button
                                    type="button"
                                    disabled={submitting}
                                    onClick={() => handleDecision("reject")}
                                    className="inline-flex items-center justify-center gap-2 rounded-lg bg-red-600 px-4 py-2 text-sm font-semibold text-white hover:bg-red-700 disabled:opacity-60"
                                >
                                    <X className="h-4 w-4" />
                                    Từ chối
                                </button>
                            </div>
                        </aside>
                    </div>
                ) : null}
            </div>
        </AdminShell>
    );
}

function Info({ label, value }: { label: string; value: string }) {
    return (
        <div className="rounded-lg border border-gray-100 bg-gray-50 p-4">
            <div className="text-xs font-semibold uppercase tracking-wide text-gray-400">{label}</div>
            <div className="mt-1 font-medium text-gray-900">{value}</div>
        </div>
    );
}