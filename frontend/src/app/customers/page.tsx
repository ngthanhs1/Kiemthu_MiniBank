"use client";

import { useRouter } from "next/navigation";
import { useState, useEffect, useCallback } from "react";
import AdminShell from "../components/admin-shell";

interface UserSummary {
    id: number;
    phone: string;
    email: string;
    fullName: string;
    status: string;
    customerRank: string;
    deviceId: string;
}

type CustomerModalMode = "balance" | "edit";

const API_BASE = (
    process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8080"
).replace(/\/+$/, "");

export default function CustomerListPage() {
    const router = useRouter();

    // States cho danh sách và lọc
    const [customers, setCustomers] = useState<UserSummary[]>([]);
    const [loading, setLoading] = useState(true);
    const [searchQuery, setSearchQuery] = useState("");
    const [activeTab, setActiveTab] = useState<"PENDING" | "ALL">("ALL");

    // States cho Modal Nạp/Rút tiền
    const [selectedUser, setSelectedUser] = useState<UserSummary | null>(null);
    const [modalMode, setModalMode] = useState<CustomerModalMode>("balance");
    const [editForm, setEditForm] = useState({ fullName: "", phone: "", email: "", customerRank: "", status: "" });
    const [amount, setAmount] = useState("");
    const [isAdjusting, setIsAdjusting] = useState(false);
    const [adjustType, setAdjustType] = useState<"cash-in" | "cash-out" | null>(null);

    const fetchCustomers = useCallback(async () => {
        setLoading(true);
        try {
            const token = localStorage.getItem("adminToken");
            if (!token) {
                router.push("/login");
                return;
            }

            const url = new URL(`${API_BASE}/api/admin/customers`);
            if (searchQuery) url.searchParams.append("q", searchQuery);

            const res = await fetch(url.toString(), {
                headers: {
                    Authorization: `Bearer ${token}`,
                    "Content-Type": "application/json"
                },
            });

            if (res.ok) {
                const data = await res.json();
                setCustomers(data);
            }
        } catch (error) {
            console.error("Lỗi tải dữ liệu:", error);
        } finally {
            setLoading(false);
        }
    }, [searchQuery, router]);

    useEffect(() => {
        const timer = setTimeout(() => fetchCustomers(), 300);
        return () => clearTimeout(timer);
    }, [fetchCustomers]);

    const handleAdjustBalance = async () => {
        if (!selectedUser || !amount || !adjustType) return;
        setIsAdjusting(true);
        try {
            const token = localStorage.getItem("adminToken");
            const res = await fetch(`${API_BASE}/api/admin/customers/${selectedUser.id}/${adjustType}`, {
                method: "POST",
                headers: {
                    "Content-Type": "application/json",
                    Authorization: `Bearer ${token}`,
                },
                body: JSON.stringify({ amount: Number(amount) }),
            });

            if (res.ok) {
                alert(`${adjustType === 'cash-in' ? 'Nạp tiền' : 'Rút tiền'} thành công!`);
                setSelectedUser(null);
                setAmount("");
                fetchCustomers();
            } else {
                const errorMsg = await res.text();
                alert("Thất bại: " + errorMsg);
            }
        } catch (err) {
            alert("Lỗi kết nối hệ thống.");
        } finally {
            setIsAdjusting(false);
        }
    };

    const openEditModal = (user: UserSummary) => {
        setSelectedUser(user);
        setModalMode("edit");
        setEditForm({
            fullName: user.fullName || "",
            phone: user.phone || "",
            email: user.email || "",
            customerRank: user.customerRank || "",
            status: user.status || "active",
        });
    };

    const handleUpdateCustomer = async () => {
        if (!selectedUser) return;
        try {
            const token = localStorage.getItem("adminToken");
            const res = await fetch(`${API_BASE}/api/admin/customers/${selectedUser.id}`, {
                method: "PUT",
                headers: { "Content-Type": "application/json", Authorization: `Bearer ${token}` },
                body: JSON.stringify(editForm),
            });
            if (!res.ok) throw new Error(await res.text());
            setSelectedUser(null);
            fetchCustomers();
        } catch (err) {
            alert(`Không thể cập nhật khách hàng: ${err instanceof Error ? err.message : "Lỗi kết nối"}`);
        }
    };

    const handleToggleLock = async (user: UserSummary) => {
        const isLocked = user.status?.toLowerCase() === "locked";
        if (!confirm(`${isLocked ? "Mở khóa" : "Khóa"} khách hàng ${user.fullName || user.id}?`)) return;
        try {
            const token = localStorage.getItem("adminToken");
            const res = await fetch(`${API_BASE}/api/admin/customers/${user.id}/${isLocked ? "unlock" : "lock"}`, {
                method: "POST",
                headers: { Authorization: `Bearer ${token}` },
            });
            if (!res.ok) throw new Error(await res.text());
            fetchCustomers();
        } catch (err) {
            alert(`Không thể đổi trạng thái khách hàng: ${err instanceof Error ? err.message : "Lỗi kết nối"}`);
        }
    };

    return (
        <AdminShell
            title="Quan ly khach hang"
            subtitle="Phe duyet KYC va quan ly bien dong so du noi bo."
            actions={
                <div className="relative">
                    <input
                        type="text"
                        placeholder="Tim theo ten, SDT..."
                        className="h-10 w-72 rounded-xl border border-black/10 bg-white px-4 text-sm outline-none transition focus:border-blue-400 focus:ring-4 focus:ring-blue-50"
                        value={searchQuery}
                        onChange={(e) => setSearchQuery(e.target.value)}
                    />
                </div>
            }
        >
            <div className="mb-6 flex gap-1 border-b border-black/5">

                <button
                    onClick={() => setActiveTab("ALL")}
                    className={`px-4 py-2 text-sm font-medium transition-all ${activeTab === "ALL" ? "border-b-2 border-blue-600 text-blue-600" : "text-zinc-500"
                        }`}
                >
                    Danh sách User ({customers.length})
                </button>
            </div>

            <div className="overflow-hidden rounded-2xl border border-black/5 bg-white shadow-sm">
                <table className="w-full text-left text-sm">
                    <thead className="bg-zinc-50/50 text-xs font-semibold uppercase tracking-wider text-zinc-500">
                        <tr>
                            <th className="px-6 py-4 font-semibold">Khách hàng</th>
                            <th className="px-6 py-4 font-semibold">Thông tin liên lạc</th>
                            <th className="px-6 py-4 font-semibold">Hạng</th>
                            <th className="px-6 py-4 font-semibold">Trạng thái</th>
                            <th className="px-6 py-4 text-right font-semibold">Hành động</th>
                        </tr>
                    </thead>
                    <tbody className="divide-y divide-black/5 text-[#111827]">
                        {loading ? (
                            <tr><td colSpan={5} className="py-10 text-center text-zinc-400 italic">Đang tải dữ liệu khách hàng...</td></tr>
                        ) : customers.length === 0 ? (
                            <tr><td colSpan={5} className="py-10 text-center text-zinc-400 italic">Không tìm thấy khách hàng nào.</td></tr>
                        ) : (
                            customers.map((user) => (
                                <tr key={user.id} className="hover:bg-zinc-50/50 transition-colors">
                                    <td className="px-6 py-4">
                                        <div className="font-semibold text-blue-600 uppercase">{user.fullName || "Chưa cập nhật"}</div>
                                        <div className="text-[10px] text-zinc-400 mt-0.5 tracking-widest uppercase">ID: {user.id}</div>
                                    </td>
                                    <td className="px-6 py-4">
                                        <div className="font-medium">{user.phone}</div>
                                        <div className="text-xs text-zinc-400">{user.email}</div>
                                    </td>
                                    <td className="px-6 py-4">
                                        <span className="rounded-md bg-blue-50 px-2 py-1 text-[10px] font-bold text-blue-700 uppercase">
                                            {user.customerRank}
                                        </span>
                                    </td>
                                    <td className="px-6 py-4">
                                        <span className={`inline-flex items-center gap-1.5 rounded-full px-2 py-0.5 text-xs font-medium ${user.status === 'ACTIVE' ? 'bg-green-50 text-green-700' : 'bg-amber-50 text-amber-700'
                                            }`}>
                                            <span className={`h-1.5 w-1.5 rounded-full ${user.status === 'ACTIVE' ? 'bg-green-600' : 'bg-amber-600'}`} />
                                            {user.status}
                                        </span>
                                    </td>
                                    <td className="px-6 py-4 text-right">
                                        <button
                                            onClick={() => { setSelectedUser(user); setModalMode("balance"); setAdjustType("cash-in"); }}
                                            className="rounded-lg border border-black/5 bg-white px-3 py-1.5 text-xs font-semibold text-blue-600 shadow-sm transition hover:bg-blue-50"
                                        >
                                            Điều chỉnh số dư
                                        </button>
                                        <button
                                            onClick={() => openEditModal(user)}
                                            className="ml-2 rounded-lg border border-black/5 bg-white px-3 py-1.5 text-xs font-semibold text-zinc-700 shadow-sm transition hover:bg-zinc-50"
                                        >
                                            Sửa thông tin
                                        </button>
                                        <button
                                            onClick={() => handleToggleLock(user)}
                                            className="ml-2 rounded-lg border border-black/5 bg-white px-3 py-1.5 text-xs font-semibold text-red-600 shadow-sm transition hover:bg-red-50"
                                        >
                                            {user.status?.toLowerCase() === "locked" ? "Mở khóa" : "Khóa"}
                                        </button>
                                    </td>
                                </tr>
                            ))
                        )}
                    </tbody>
                </table>
            </div>

            {selectedUser && modalMode === "balance" && (
                <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40 backdrop-blur-sm p-4">
                    <div className="w-full max-w-md rounded-3xl bg-white p-8 shadow-2xl transition-all">
                        <div className="mb-6 flex items-center justify-between">
                            <h2 className="text-xl font-semibold">Điều chỉnh số dư</h2>
                            <button onClick={() => setSelectedUser(null)} className="text-zinc-400 hover:text-black text-2xl">×</button>
                        </div>

                        <div className="mb-6 space-y-1">
                            <p className="text-sm text-zinc-500 font-medium tracking-tight">KHÁCH HÀNG</p>
                            <p className="text-lg font-semibold text-blue-600">{selectedUser.fullName}</p>
                        </div>

                        <div className="mb-6">
                            <label className="text-[10px] font-bold uppercase text-zinc-400 tracking-wider">Chọn loại giao dịch</label>
                            <div className="mt-2 grid grid-cols-2 gap-3">
                                <button
                                    onClick={() => setAdjustType("cash-in")}
                                    className={`h-11 rounded-xl border text-sm font-semibold transition-all ${adjustType === 'cash-in' ? 'border-blue-600 bg-blue-50 text-blue-600' : 'border-black/5'}`}
                                >
                                    Nạp tiền (Cash In)
                                </button>
                                <button
                                    onClick={() => setAdjustType("cash-out")}
                                    className={`h-11 rounded-xl border text-sm font-semibold transition-all ${adjustType === 'cash-out' ? 'border-red-600 bg-red-50 text-red-600' : 'border-black/5'}`}
                                >
                                    Rút tiền (Cash Out)
                                </button>
                            </div>
                        </div>

                        <div className="mb-8">
                            <label className="text-[10px] font-bold uppercase text-zinc-400 tracking-wider">Số tiền (VNĐ)</label>
                            <input
                                type="number"
                                placeholder="Nhập số tiền..."
                                className="mt-2 h-12 w-full rounded-xl border border-black/10 bg-white px-4 font-semibold outline-none focus:border-blue-400"
                                value={amount}
                                onChange={(e) => setAmount(e.target.value)}
                            />
                        </div>

                        <button
                            onClick={handleAdjustBalance}
                            disabled={isAdjusting || !amount}
                            className={`h-12 w-full rounded-xl text-sm font-bold text-white shadow-md transition-all ${adjustType === 'cash-in' ? 'bg-blue-600 hover:bg-blue-700' : 'bg-red-600 hover:bg-red-700'
                                } disabled:opacity-50`}
                        >
                            {isAdjusting ? "Đang xử lý..." : "Xác nhận giao dịch"}
                        </button>
                    </div>
                </div>
            )}

            {selectedUser && modalMode === "edit" && (
                <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40 p-4 backdrop-blur-sm">
                    <div className="w-full max-w-lg rounded-3xl bg-white p-8 shadow-2xl">
                        <div className="mb-6 flex items-center justify-between">
                            <h2 className="text-xl font-semibold">Sửa thông tin khách hàng</h2>
                            <button onClick={() => setSelectedUser(null)} className="text-2xl text-zinc-400 hover:text-black">×</button>
                        </div>
                        <div className="grid gap-4">
                            {[
                                ["fullName", "Họ tên"],
                                ["phone", "Số điện thoại"],
                                ["email", "Email"],
                                ["customerRank", "Hạng khách hàng"],
                                ["status", "Trạng thái"],
                            ].map(([key, label]) => (
                                <label key={key} className="text-xs font-bold uppercase text-zinc-500">
                                    {label}
                                    <input
                                        className="mt-1 h-11 w-full rounded-xl border border-black/10 px-4 text-sm font-medium normal-case text-zinc-800 outline-none focus:border-blue-400"
                                        value={editForm[key as keyof typeof editForm]}
                                        onChange={(e) => setEditForm((prev) => ({ ...prev, [key]: e.target.value }))}
                                    />
                                </label>
                            ))}
                        </div>
                        <button onClick={handleUpdateCustomer} className="mt-6 h-12 w-full rounded-xl bg-blue-600 text-sm font-bold text-white shadow-md hover:bg-blue-700">
                            Lưu thay đổi
                        </button>
                    </div>
                </div>
            )}
        </AdminShell>
    );
}