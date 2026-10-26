"use client";

import { useEffect, useState } from "react";
import AdminShell from "../../components/admin-shell";

type FluctuationItem = {
    id: number;
    createdAt: string;
    accountNumber: string;
    customerName: string;
    type: string;
    amount: number;
    balanceBefore: number | null;
    balanceAfter: number | null;
    description: string;
};

export default function AdminBalanceFluctuationsPage() {
    const [fluctuations, setFluctuations] = useState<FluctuationItem[]>([]);
    const [loading, setLoading] = useState(true);
    const [search, setSearch] = useState("");
    const [typeFilter, setTypeFilter] = useState("");

    const toNumberOrNull = (value: unknown): number | null => {
        if (value === null || value === undefined || value === "") return null;
        const parsed = Number(value);
        return Number.isFinite(parsed) ? parsed : null;
    };

    const loadData = async () => {
        try {
            setLoading(true);
            const token = localStorage.getItem("adminToken") || localStorage.getItem("token");
            const headers = { "Authorization": `Bearer ${token}`, "Content-Type": "application/json" };

            // Trỏ trực tiếp về API quản lý sổ cái/giao dịch thực tế của hệ thống Core-Banking
            let url = `${process.env.NEXT_PUBLIC_API_BASE_URL}/api/admin/transactions`;

            // Nếu bạn cấu hình endpoint riêng cho biến động số dư trong AdminLedgerController, hãy đổi đường dẫn thành:
            // let url = `${process.env.NEXT_PUBLIC_API_BASE_URL}/api/admin/ledgers`;

            const params = new URLSearchParams();
            if (search) params.append("q", search);
            if (params.toString()) url += `?${params.toString()}`;

            const res = await fetch(url, { headers });
            if (res.ok) {
                const data = await res.json();

                // Chuẩn hóa dữ liệu trả về từ database: map các trường của API chung thành các trường hiển thị của Biến động số dư
                const formattedData = (Array.isArray(data) ? data : []).map((item: any) => {
                    // Xác định trạng thái dòng tiền dựa trên cấu trúc dữ liệu transaction từ database của bạn
                    // Nếu tài khoản đích trùng hoặc loại giao dịch mang tính chất cộng tiền
                    const isInflow = item.transactionType?.toLowerCase().includes("deposit") ||
                        item.transactionType?.toLowerCase().includes("receive") ||
                        !item.fromAccountNumber;

                    return {
                        id: item.id,
                        createdAt: item.createdAt,
                        // Nếu là tiền vào lấy số tài khoản đích (to), tiền ra lấy số tài khoản nguồn (from) để hiển thị đúng thực tế chủ thể
                        accountNumber: isInflow ? (item.toAccountNumber || "---") : (item.fromAccountNumber || "---"),
                        customerName: isInflow ? (item.toAccountName || "Hệ thống") : (item.fromAccountName || "Khách hàng"),
                        type: isInflow ? "INFLOW" : "OUTFLOW",
                        amount: Number(item.amount ?? 0),
                        balanceBefore: toNumberOrNull(item.balanceBefore ?? item.beforeBalance ?? item.previousBalance),
                        balanceAfter: toNumberOrNull(item.balanceAfter ?? item.afterBalance ?? item.newBalance),
                        description: item.description || `Giao dịch hệ thống ${item.transactionCode || ""}`
                    };
                });

                setFluctuations(formattedData);
            }
        } catch (error) {
            console.error("Lỗi đồng bộ dữ liệu biến động số dư:", error);
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        loadData();
    }, []);

    const handleSearchKeyDown = (e: React.KeyboardEvent) => {
        if (e.key === "Enter") loadData();
    };

    // Lọc Client-side để đảm bảo bộ lọc "Loại hình" hoạt động ngay lập tức
    const filteredFluctuations = fluctuations.filter(item => {
        if (!typeFilter) return true;
        return item.type === typeFilter;
    });

    // Tính toán các thông số Thống kê trực tiếp từ mảng dữ liệu database đã nạp thành công
    const displayTotalTx = filteredFluctuations.length;
    const displayInflow = filteredFluctuations.filter(f => f.type === "INFLOW").reduce((sum, item) => sum + item.amount, 0);
    const displayOutflow = filteredFluctuations.filter(f => f.type === "OUTFLOW").reduce((sum, item) => sum + item.amount, 0);

    return (
        <AdminShell title="Biến động số dư" subtitle="Theo dõi lịch sử biến động số dư của tất cả tài khoản">


            <div className="flex flex-wrap items-center justify-between gap-4 bg-white p-4 rounded-xl border border-black/5 shadow-3xs mb-4">
                <div className="flex h-9 w-96 items-center gap-2 rounded-lg border border-black/10 bg-zinc-50 px-3">
                    <span className="text-zinc-400 text-xs">🔎</span>
                    <input
                        className="w-full bg-transparent text-xs font-medium text-zinc-700 outline-none"
                        placeholder="Tìm theo STK, tên khách hàng... (Ấn Enter)"
                        value={search}
                        onChange={e => setSearch(e.target.value)}
                        onKeyDown={handleSearchKeyDown}
                    />
                </div>

                <div className="flex items-center gap-2">
                    <select
                        className="h-9 rounded-lg border border-black/10 bg-white px-3 text-xs font-semibold text-zinc-700 outline-none cursor-pointer"
                        value={typeFilter}
                        onChange={e => setTypeFilter(e.target.value)}
                    >
                        <option value="">Tất cả loại biến động</option>
                        <option value="INFLOW">Tiền vào (+)</option>
                        <option value="OUTFLOW">Tiền ra (-)</option>
                    </select>
                </div>
            </div>

            {/* Thẻ Thống kê 3 nhóm Chỉ số dòng tiền mini */}
            <div className="grid grid-cols-1 md:grid-cols-3 gap-4 mb-6">
                <div className="bg-white p-4 rounded-xl border border-black/5 shadow-3xs">
                    <div className="text-[10px] font-bold text-zinc-400 uppercase tracking-wider">📈 Tổng số giao dịch</div>
                    <div className="text-xl font-black text-zinc-900 mt-1">{displayTotalTx}</div>
                </div>
                <div className="bg-white p-4 rounded-xl border border-black/5 shadow-3xs">
                    <div className="text-[10px] font-bold text-zinc-400 uppercase tracking-wider text-green-600">⬆ Tổng tiền vào</div>
                    <div className="text-xl font-black text-green-600 mt-1">+{displayInflow.toLocaleString()} đ</div>
                </div>
                <div className="bg-white p-4 rounded-xl border border-black/5 shadow-3xs">
                    <div className="text-[10px] font-bold text-zinc-400 uppercase tracking-wider text-red-600">⬇ Tổng tiền ra</div>
                    <div className="text-xl font-black text-red-600 mt-1">-{displayOutflow.toLocaleString()} đ</div>
                </div>
            </div>


            <div className="bg-white rounded-2xl border border-black/5 overflow-hidden shadow-xs">
                <table className="w-full text-left text-xs border-collapse">
                    <thead>
                        <tr className="bg-zinc-50 border-b border-zinc-100 text-zinc-400 font-bold uppercase text-[10px] tracking-wider">
                            <th className="p-4 font-mono">Thời gian</th>
                            <th className="p-4">STK</th>
                            <th className="p-4">Khách hàng</th>
                            <th className="p-4 text-center">Loại</th>
                            <th className="p-4 text-right">Số tiền phát sinh</th>
                            <th className="p-4 text-right">Số dư trước</th>
                            <th className="p-4 text-right">Số dư sau</th>
                            <th className="p-4 pl-6">Mô tả nội dung</th>
                        </tr>
                    </thead>
                    <tbody className="divide-y divide-zinc-100 font-semibold text-zinc-700">
                        {loading ? (
                            <tr><td colSpan={8} className="p-8 text-center text-zinc-400 animate-pulse">Đang nạp dữ liệu dòng tiền từ database...</td></tr>
                        ) : filteredFluctuations.length === 0 ? (
                            <tr><td colSpan={8} className="p-8 text-center text-zinc-400">Không tìm thấy lịch sử biến động số dư phù hợp.</td></tr>
                        ) : (
                            filteredFluctuations.map((item) => {
                                const isInflow = item.type === "INFLOW";
                                return (
                                    <tr key={item.id} className="hover:bg-zinc-50/50 transition-colors">
                                        <td className="p-4 font-mono text-zinc-400">{new Date(item.createdAt).toLocaleString("vi-VN")}</td>
                                        <td className="p-4 font-bold font-mono text-zinc-900">{item.accountNumber}</td>
                                        <td className="p-4 text-zinc-900">{item.customerName}</td>
                                        <td className="p-4 text-center">
                                            <span className={`px-2 py-0.5 rounded text-[9px] font-bold ${isInflow ? "bg-green-50 text-green-700" : "bg-red-50 text-red-700"
                                                }`}>
                                                {isInflow ? "Tiền vào" : "Tiền ra"}
                                            </span>
                                        </td>
                                        <td className={`p-4 text-right font-mono font-bold ${isInflow ? "text-green-600" : "text-red-600"}`}>
                                            {isInflow ? "+" : "-"}{item.amount.toLocaleString()} đ
                                        </td>
                                        <td className="p-4 text-right font-mono text-zinc-400">{item.balanceBefore === null ? "-" : `${item.balanceBefore.toLocaleString()} đ`}</td>
                                        <td className="p-4 text-right font-mono font-bold text-zinc-950">{item.balanceAfter === null ? "-" : `${item.balanceAfter.toLocaleString()} đ`}</td>
                                        <td className="p-4 pl-6 text-zinc-500 font-medium max-w-xs truncate" title={item.description}>{item.description}</td>
                                    </tr>
                                );
                            })
                        )}
                    </tbody>
                </table>
            </div>
        </AdminShell>
    );
}