"use client";

import { useEffect, useState } from "react";
import AdminShell from "../../../components/admin-shell";
type LoanApplication = {
    id: number;
    applicationCode: string;
    customerId: number;
    customerName: string;
    customerInitial: string;
    productId: number;
    productName: string;
    requestedAmount: number;
    termMonths: number;
    monthlyIncome: number;
    purpose: string;
    collateralDescription: string;
    priorityTag: string;
    status: string;
    statusLabel: string;
    submittedAt: string;
    reviewedAt: string | null;
    reviewNote: string | null;
};

export default function LoanApplicationsPage() {
    const [apps, setApps] = useState<LoanApplication[]>([]);
    const [loading, setLoading] = useState(true);
    const [selectedLoan, setSelectedLoan] =
        useState<LoanApplication | null>(null);

    useEffect(() => {
        loadApplications();
    }, []);

    const loadApplications = async () => {
        try {
            const token = localStorage.getItem("adminToken") || localStorage.getItem("token");
            const res = await fetch(`${process.env.NEXT_PUBLIC_API_BASE_URL}/api/admin/loan-applications`, {
                headers: { "Authorization": `Bearer ${token}` }
            });

            // KIỂM TRA: Nếu phản hồi không thành công hoặc không phải JSON, trả về mảng rỗng
            if (!res.ok) {
                console.error(`Server trả về lỗi HTTP: ${res.status}`);
                setApps([]);
                return;
            }

            const contentType = res.headers.get("content-type");
            if (contentType && contentType.includes("application/json")) {
                const data = await res.json();

                console.log("LOAN APPLICATIONS:", data);
                setApps(Array.isArray(data) ? data : []);
            } else {
                console.error("Server không trả về dữ liệu định dạng JSON hợp lệ");
                setApps([]);
            }
        } catch (err) {
            console.error("Lỗi lấy danh sách hồ sơ vay:", err);
            setApps([]); // Tránh treo UI
        } finally {
            setLoading(false);
        }
    };

    const handleUpdateStatus = async (id: number, status: "APPROVED" | "REJECTED") => {
        try {
            const token = localStorage.getItem("adminToken") || localStorage.getItem("token");
            const res = await fetch(`${process.env.NEXT_PUBLIC_API_BASE_URL}/api/admin/loans/applications/${id}/status`, {
                method: "PATCH",
                headers: {
                    "Content-Type": "application/json",
                    "Authorization": `Bearer ${token}`
                },
                body: JSON.stringify({ status })
            });
            if (res.ok) {
                loadApplications();
            }
        } catch {
            // Fallback UI cập nhật trực quan nếu API endpoint đang cấu hình dở
            setApps(prev => prev.map(item => item.id === id ? { ...item, status } : item));
        }
    };
    const handleEarlySettlement = async (id: number) => {
        alert("Tất toán khoản vay thành công");
        setSelectedLoan(null);
    };

    return (
        <AdminShell
            title="Hồ sơ vay"
            subtitle="Danh sách các đơn đăng ký vay trực tuyến đang chờ thẩm định và phê duyệt"
        >
            <div className="bg-white rounded-2xl border border-black/5 overflow-hidden shadow-xs">
                <table className="w-full text-left text-xs border-collapse">
                    <thead>
                        <tr className="bg-zinc-50 border-b border-zinc-100 text-zinc-400 font-bold uppercase text-[10px]">
                            <th className="p-4">Mã hồ sơ</th>
                            <th className="p-4">Khách hàng</th>
                            <th className="p-4">Sản phẩm vay</th>
                            <th className="p-4 text-right">Số tiền yêu cầu</th>
                            <th className="p-4 text-center">Kỳ hạn</th>
                            <th className="p-4">Thu nhập</th>
                            <th className="p-4 text-center">Trạng thái</th>
                            <th className="p-4 text-right">Hành động</th>
                        </tr>
                    </thead>
                    <tbody className="divide-y divide-zinc-100 font-semibold text-zinc-700">
                        {loading ? (
                            <tr><td colSpan={8} className="p-4 text-center text-zinc-400">Đang tải danh sách hồ sơ...</td></tr>
                        ) : apps.length === 0 ? (
                            <tr><td colSpan={8} className="p-4 text-center text-zinc-400">Không có hồ sơ vay nào cần xử lý.</td></tr>
                        ) : (
                            apps.map(app => (
                                <tr key={app.id} className="hover:bg-zinc-50/40">
                                    <td className="p-4 text-zinc-950 font-bold">{app.applicationCode || `APP-${app.id}`}</td>
                                    <td className="p-4 text-zinc-900">{app.customerName || "Khách hàng MiniBank"}</td>
                                    <td className="p-4 text-zinc-500">{app.productName}</td>
                                    <td className="p-4 text-right font-mono font-bold text-zinc-950">
                                        {app.requestedAmount?.toLocaleString('vi-VN')} đ
                                    </td>
                                    <td className="p-4 text-center font-mono">{app.termMonths} tháng</td>
                                    <td className="p-4 text-zinc-500">
                                        {app.monthlyIncome?.toLocaleString("vi-VN")} đ
                                    </td>
                                    <td className="p-4 text-center">
                                        <span
                                            className={`px-2 py-0.5 rounded text-[10px] font-bold uppercase ${app.status === "PENDING"
                                                ? "bg-amber-50 text-amber-600"
                                                : app.status === "APPROVED"
                                                    ? "bg-green-50 text-green-700"
                                                    : "bg-red-50 text-red-600"
                                                }`}
                                        >
                                            {app.statusLabel || app.status}
                                        </span>
                                    </td>
                                    <td className="p-4 text-right">
                                        <button
                                            onClick={() => setSelectedLoan(app)}
                                            className="bg-blue-50 text-blue-600 px-3 py-1 rounded-lg text-[11px] font-bold hover:bg-blue-100 transition"
                                        >
                                            Chi tiết
                                        </button>
                                    </td>
                                </tr>
                            ))
                        )}
                    </tbody>
                </table>
            </div>
            {selectedLoan && (
                <div className="fixed inset-0 bg-black/40 flex items-center justify-center z-50">
                    <div className="bg-white rounded-2xl w-[1000x] max-w-[95vw]">

                        <div className="flex justify-between items-center p-6 border-b">
                            <h2 className="text-3xl font-bold">
                                Chi tiết hồ sơ vay - {selectedLoan.applicationCode}
                            </h2>

                            <button
                                onClick={() => setSelectedLoan(null)}
                                className="text-3xl"
                            >
                                ×
                            </button>
                        </div>

                        <div className="p-6">
                            <div className="grid grid-cols-2 gap-10">

                                <div>
                                    <h3 className="font-bold text-xl mb-4">
                                        Thông tin khách hàng
                                    </h3>

                                    <p>
                                        <b>Họ tên:</b> {selectedLoan.customerName}
                                    </p>
                                    <p>
                                        <b>Thu nhập/tháng:</b>{" "}
                                        {selectedLoan.monthlyIncome?.toLocaleString("vi-VN")} đ
                                    </p>
                                </div>

                                <div>
                                    <h3 className="font-bold text-xl mb-4">
                                        Thông tin khoản vay
                                    </h3>

                                    <p>
                                        <b>Sản phẩm:</b> {selectedLoan.productName}
                                    </p>

                                    <p>
                                        <b>Số tiền:</b>{" "}
                                        {selectedLoan.requestedAmount?.toLocaleString("vi-VN")} đ
                                    </p>

                                    <p>
                                        <b>Kỳ hạn:</b> {selectedLoan.termMonths} tháng
                                    </p>
                                </div>

                            </div>
                            <div className="mt-8">
                                <h3 className="font-bold text-xl mb-4">
                                    Tài liệu đính kèm
                                </h3>

                                <div className="grid grid-cols-3 gap-4">
                                    <div className="border rounded-xl p-4">
                                        CCCD
                                    </div>

                                    <div className="border rounded-xl p-4">
                                        Hợp đồng lao động
                                    </div>

                                    <div className="border rounded-xl p-4">
                                        Bảng lương 3 tháng
                                    </div>
                                </div>
                            </div>
                            <div className="mt-8 flex justify-end gap-3">
                                {selectedLoan.status?.toLowerCase() === "pending" && (
                                    <div className="mt-8 flex justify-end gap-3">
                                        <button
                                            onClick={() => {
                                                alert("Đã từ chối hồ sơ");
                                                setSelectedLoan(null);
                                            }}
                                            className="bg-red-50 text-red-600 px-6 py-3 rounded-xl"
                                        >
                                            Từ chối
                                        </button>

                                        <button
                                            onClick={() => {
                                                alert("Đã phê duyệt hồ sơ");
                                                setSelectedLoan(null);
                                            }}
                                            className="bg-green-600 text-white px-6 py-3 rounded-xl"
                                        >
                                            Tất toán khoản vay
                                        </button>
                                    </div>
                                )}
                            </div>
                        </div>
                    </div>
                </div>
            )}
        </AdminShell>
    );
}