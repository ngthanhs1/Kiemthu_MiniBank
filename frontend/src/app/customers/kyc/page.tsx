"use client";

import { useState, useEffect } from "react";
import AdminShell from "../../components/admin-shell";

interface KycRequestSummary {
    id: number;
    userId: number;
    phone: string;
    email: string;
    fullName: string;
    dob: string;
    citizenId: string;
    address: string;
    occupation: string;
    monthlyIncome: string;
    citizenFrontImageUrl: string;
    citizenBackImageUrl: string;
    portraitImageUrl: string;
    status: string;
    submittedAt: string;
}

const API_BASE = (
    process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8080"
).replace(/\/+$/, "");

const STATUS_TABS = [
    { label: "Cho duyet", value: "pending" },
    { label: "Da duyet", value: "approved" },
    { label: "Tu choi", value: "rejected" },
    { label: "Tat ca", value: "all" },
];

const STATUS_STYLES: Record<string, string> = {
    pending: "bg-amber-50 text-amber-700",
    approved: "bg-emerald-50 text-emerald-700",
    rejected: "bg-rose-50 text-rose-700",
};

export default function KycAdminPage() {
    const [requests, setRequests] = useState<KycRequestSummary[]>([]);
    const [selectedKyc, setSelectedKyc] = useState<KycRequestSummary | null>(null);
    const [note, setNote] = useState("");
    const [accountNumber, setAccountNumber] = useState("");
    const [activeTab, setActiveTab] = useState("pending");
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState<string | null>(null);

    const fetchKyc = async (status: string) => {
        try {
            setLoading(true);
            setError(null);
            const token = localStorage.getItem("adminToken");
            if (!token) {
                setError("Vui long dang nhap de tiep tuc.");
                setRequests([]);
                return;
            }
            const statusQuery = status !== "all" ? `?status=${encodeURIComponent(status)}` : "";
            const res = await fetch(`${API_BASE}/api/admin/kyc${statusQuery}`, {
                headers: { Authorization: `Bearer ${token}` }
            });
            if (!res.ok) {
                const text = await res.text();
                throw new Error(text || "Khong the tai du lieu KYC.");
            }
            setRequests(await res.json());
        } catch (error) {
            const message = error instanceof Error ? error.message : "Khong the tai du lieu KYC.";
            setError(message);
            setRequests([]);
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        fetchKyc(activeTab);
    }, [activeTab]);

    const handleDecision = async (type: 'approve' | 'reject') => {
        if (!selectedKyc) return;
        try {
            const token = localStorage.getItem("adminToken");
            const res = await fetch(`${API_BASE}/api/admin/kyc/${selectedKyc.id}/${type}`, {
                method: "POST",
                headers: {
                    "Content-Type": "application/json",
                    Authorization: `Bearer ${token}`
                },
                body: JSON.stringify({
                    note,
                    accountNumber: type === 'approve' ? accountNumber : null
                })
            });

            if (res.ok) {
                alert(type === 'approve' ? "Đã phê duyệt!" : "Đã từ chối!");
                setSelectedKyc(null);
                fetchKyc(activeTab);
            }
        } catch (error) {
            alert("Lỗi xử lý hệ thống");
        }
    };

    return (
        <AdminShell
            title="Kiem duyet KYC"
            subtitle="Xem xet va phe duyet ho so xac minh danh tinh."
        >
            <div className="flex flex-wrap gap-2">
                {STATUS_TABS.map((tab) => (
                    <button
                        key={tab.value}
                        className={`rounded-full px-4 py-2 text-sm font-semibold transition ${
                            activeTab === tab.value
                                ? "bg-blue-600 text-white"
                                : "bg-white text-zinc-600 border border-black/10 hover:bg-zinc-50"
                        }`}
                        onClick={() => setActiveTab(tab.value)}
                        type="button"
                    >
                        {tab.label}
                    </button>
                ))}
            </div>

            <div className="overflow-hidden rounded-2xl border border-black/5 bg-white shadow-sm">
                <table className="w-full text-left text-sm">
                    <thead className="bg-zinc-50/50 text-xs font-bold uppercase text-zinc-500">
                        <tr>
                            <th className="px-6 py-4">Mã KYC</th>
                            <th className="px-6 py-4">Khách hàng</th>
                            <th className="px-6 py-4">Ngày gửi</th>
                            <th className="px-6 py-4">Trạng thái</th>
                            <th className="px-6 py-4 text-right">Thao tác</th>
                        </tr>
                    </thead>
                    <tbody className="divide-y divide-black/5">
                        {loading ? (
                            <tr>
                                <td colSpan={5} className="px-6 py-8 text-center text-zinc-400">
                                    Dang tai du lieu KYC...
                                </td>
                            </tr>
                        ) : error ? (
                            <tr>
                                <td colSpan={5} className="px-6 py-8 text-center text-rose-600">
                                    {error}
                                </td>
                            </tr>
                        ) : requests.length === 0 ? (
                            <tr>
                                <td colSpan={5} className="px-6 py-8 text-center text-zinc-400">
                                    Khong co ho so KYC nao.
                                </td>
                            </tr>
                        ) : requests.map((req) => (
                            <tr key={req.id} className="hover:bg-zinc-50/50">
                                <td className="px-6 py-4 font-medium">KYC{req.id.toString().padStart(3, '0')}</td>
                                <td className="px-6 py-4">
                                    <div className="font-semibold text-blue-600">{req.fullName}</div>
                                    <div className="text-xs text-zinc-400">{req.phone}</div>
                                </td>
                                <td className="px-6 py-4 text-zinc-500">{new Date(req.submittedAt).toLocaleString()}</td>
                                <td className="px-6 py-4">
                                    <span
                                        className={`rounded-full px-2 py-1 text-xs font-medium ${
                                            STATUS_STYLES[req.status?.toLowerCase()] ?? "bg-zinc-50 text-zinc-600"
                                        }`}
                                    >
                                        {req.status}
                                    </span>
                                </td>
                                <td className="px-6 py-4 text-right">
                                    <button onClick={() => setSelectedKyc(req)} className="text-blue-600 font-bold text-xs hover:underline">
                                        XEM CHI TIẾT
                                    </button>
                                </td>
                            </tr>
                        ))}
                    </tbody>
                </table>
            </div>

            {selectedKyc && (
                <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40 backdrop-blur-sm p-4">
                    <div className="w-full max-w-4xl max-h-[90vh] overflow-y-auto rounded-3xl bg-white p-8 shadow-2xl">
                        <div className="flex justify-between items-center mb-6">
                            <h2 className="text-xl font-bold">Chi tiết KYC - KYC{selectedKyc.id}</h2>
                            <button onClick={() => setSelectedKyc(null)} className="text-2xl text-zinc-400">×</button>
                        </div>

                        <div className="grid grid-cols-2 gap-8">
                            {/* Cột trái: Thông tin cá nhân */}
                            <div className="space-y-4">
                                <h3 className="text-sm font-bold text-zinc-400 uppercase tracking-widest">Thông tin cá nhân</h3>
                                <div className="grid grid-cols-2 gap-4 text-sm">
                                    <span className="text-zinc-500">Họ tên:</span> <span className="font-bold">{selectedKyc.fullName}</span>
                                    <span className="text-zinc-500">Ngày sinh:</span> <span className="font-bold">{selectedKyc.dob}</span>
                                    <span className="text-zinc-500">Số CCCD:</span> <span className="font-bold">{selectedKyc.citizenId}</span>
                                    <span className="text-zinc-500">Địa chỉ:</span> <span className="font-bold">{selectedKyc.address}</span>
                                </div>

                                {selectedKyc.status?.toLowerCase() === "pending" ? (
                                    <div className="mt-6 pt-6 border-t border-zinc-100">
                                        <label className="text-xs font-bold text-zinc-400 uppercase">Lý do từ chối (nếu có)</label>
                                        <textarea
                                            className="mt-2 w-full p-3 border rounded-xl outline-none focus:border-blue-400 min-h-[100px]"
                                            placeholder="Nhập lý do..."
                                            value={note}
                                            onChange={(e) => setNote(e.target.value)}
                                        />
                                    </div>
                                ) : null}
                            </div>

                            <div className="space-y-4">
                                <h3 className="text-sm font-bold text-zinc-400 uppercase tracking-widest">Tài liệu đính kèm</h3>
                                <div className="grid grid-cols-1 gap-4">
                                    {[
                                        { label: "Mat truoc CCCD", url: selectedKyc.citizenFrontImageUrl },
                                        { label: "Mat sau CCCD", url: selectedKyc.citizenBackImageUrl },
                                        { label: "Anh chan dung", url: selectedKyc.portraitImageUrl },
                                    ].map((item) => (
                                        <div key={item.label} className="border rounded-xl p-4 text-center">
                                            <p className="text-xs text-zinc-400 mb-2">{item.label}</p>
                                            {item.url ? (
                                                <img
                                                    src={item.url}
                                                    alt={item.label}
                                                    className="h-40 w-full rounded-lg object-cover"
                                                />
                                            ) : (
                                                <div className="h-32 bg-zinc-50 rounded flex items-center justify-center border-dashed border-2 text-zinc-300">
                                                    Khong co anh
                                                </div>
                                            )}
                                        </div>
                                    ))}
                                </div>
                            </div>
                        </div>

                        {selectedKyc.status?.toLowerCase() === "pending" ? (
                            <div className="mt-8 flex justify-end gap-3 pt-6 border-t">
                                <button
                                    onClick={() => handleDecision('reject')}
                                    className="px-6 py-2.5 rounded-xl border font-bold text-red-600 hover:bg-red-50"
                                >
                                    TU CHOI
                                </button>
                                <button
                                    onClick={() => handleDecision('approve')}
                                    className="px-6 py-2.5 rounded-xl bg-green-600 text-white font-bold hover:bg-green-700"
                                >
                                    PHE DUYET
                                </button>
                            </div>
                        ) : null}
                    </div>
                </div>
            )}
        </AdminShell>
    );
}