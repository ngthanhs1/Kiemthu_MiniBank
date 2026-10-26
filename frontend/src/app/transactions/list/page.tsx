"use client";
//install recharts
import { useEffect, useState } from "react";
import AdminShell from "../../components/admin-shell";
import { LineChart, Line, XAxis, YAxis, Tooltip, ResponsiveContainer } from "recharts";

type TransactionOverview = {
    totalTransactions: number;
    completedTransactions: number;
    totalCompletedAmount: number;
};

type TransactionItem = {
    id: number;
    transactionCode: string;
    fromAccountNumber: string | null;
    fromAccountName: string | null;
    toAccountNumber: string | null;
    toAccountName: string | null;
    amount: number;
    feeAmount: number;
    transactionType: string;
    status: string;
    createdAt: string;
};

export default function AdminTransactionsListPage() {
    const [overview, setOverview] = useState<TransactionOverview | null>(null);
    const [transactions, setTransactions] = useState<TransactionItem[]>([]);
    const [loading, setLoading] = useState(true);
    const [search, setSearch] = useState("");
    const [statusFilter, setStatusFilter] = useState("");
    const [refundingId, setRefundingId] = useState<number | null>(null);

    const loadData = async () => {
        try {
            setLoading(true);
            const token = localStorage.getItem("adminToken") || localStorage.getItem("token");
            const headers = { "Authorization": `Bearer ${token}`, "Content-Type": "application/json" };

            // 1. Fetch Overview Metrics
            const overviewRes = await fetch(`${process.env.NEXT_PUBLIC_API_BASE_URL}/api/admin/transactions/overview`, { headers });
            if (overviewRes.ok) {
                const overviewData = await overviewRes.json();
                setOverview(overviewData);
            }

            // 2. Fetch Transaction List with queries
            let url = `${process.env.NEXT_PUBLIC_API_BASE_URL}/api/admin/transactions`;
            const params = new URLSearchParams();
            if (search) params.append("q", search);
            if (statusFilter) params.append("status", statusFilter);
            if (params.toString()) url += `?${params.toString()}`;

            const listRes = await fetch(url, { headers });
            if (listRes.ok) {
                const listData = await listRes.json();
                setTransactions(Array.isArray(listData) ? listData : []);
            }
        } catch (error) {
            console.error("Lỗi đồng bộ cơ sở dữ liệu giao dịch:", error);
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        loadData();
    }, [statusFilter]);

    const handleSearchKeyDown = (e: React.KeyboardEvent) => {
        if (e.key === "Enter") loadData();
    };

    const handleRefund = async (tx: TransactionItem) => {
        if (!confirm(`Hoàn lại giao dịch ${tx.transactionCode}?`)) return;
        try {
            setRefundingId(tx.id);
            const token = localStorage.getItem("adminToken") || localStorage.getItem("token");
            const res = await fetch(`${process.env.NEXT_PUBLIC_API_BASE_URL}/api/admin/transactions/${tx.id}/refund`, {
                method: "POST",
                headers: { "Authorization": `Bearer ${token}`, "Content-Type": "application/json" },
            });
            if (!res.ok) throw new Error(await res.text());
            await loadData();
        } catch (err) {
            alert(`Không thể hoàn giao dịch: ${err instanceof Error ? err.message : "Lỗi kết nối"}`);
        } finally {
            setRefundingId(null);
        }
    };

    // Hàm xử lý: Tự động gom nhóm số tiền các giao dịch thành công (completed) từ DB theo mốc giờ
    const getChartData = () => {
        const timeSlots = ["00:00", "03:00", "06:00", "09:00", "12:00", "15:00", "18:00", "21:00"];
        const slotValues: { [key: string]: number } = {
            "00:00": 0, "03:00": 0, "06:00": 0, "09:00": 0, "12:00": 0, "15:00": 0, "18:00": 0, "21:00": 0
        };

        transactions.forEach(tx => {
            if (tx.status?.toLowerCase() === "completed") {
                const hours = new Date(tx.createdAt).getHours();
                let slot = "00:00";

                if (hours >= 21) slot = "21:00";
                else if (hours >= 18) slot = "18:00";
                else if (hours >= 15) slot = "15:00";
                else if (hours >= 12) slot = "12:00";
                else if (hours >= 9) slot = "09:00";
                else if (hours >= 6) slot = "06:00";
                else if (hours >= 3) slot = "03:00";

                // Quy đổi ra đơn vị Triệu đồng để biểu đồ hiển thị thanh thoát, không bị tràn hàng số dài
                slotValues[slot] += (tx.amount || 0) / 1000000;
            }
        });

        return timeSlots.map(slot => ({
            time: slot,
            amount: Math.round(slotValues[slot] * 100) / 100 // Làm tròn 2 chữ số thập phân
        }));
    };

    return (
        <AdminShell title="Lịch sử giao dịch" subtitle="Giám sát và kiểm toán toàn bộ luồng giao dịch tài chính trên hệ thống Core">

            {/* Khối thống kê Overview lấy trực tiếp từ API overview */}
            <div className="grid grid-cols-1 md:grid-cols-3 gap-4 mb-6">
                <div className="bg-white p-4 rounded-xl border border-black/5 shadow-3xs">
                    <div className="text-[11px] font-bold text-zinc-400 uppercase">Tổng số giao dịch</div>
                    <div className="text-xl font-black text-zinc-900 mt-1">{overview?.totalTransactions.toLocaleString() || 0}</div>
                </div>
                <div className="bg-white p-4 rounded-xl border border-black/5 shadow-3xs">
                    <div className="text-[11px] font-bold text-zinc-400 uppercase">Giao dịch thành công</div>
                    <div className="text-xl font-black text-green-600 mt-1">{overview?.completedTransactions.toLocaleString() || 0}</div>
                </div>
                <div className="bg-white p-4 rounded-xl border border-black/5 shadow-3xs">
                    <div className="text-[11px] font-bold text-zinc-400 uppercase font-mono">Tổng doanh số thành công</div>
                    <div className="text-xl font-black text-blue-600 mt-1">{(overview?.totalCompletedAmount || 0).toLocaleString()} đ</div>
                </div>
            </div>

            {/* Khối Biểu đồ biến động dòng tiền theo giờ - Lấy dữ liệu thực tế từ Database */}
            <div className="bg-white p-6 rounded-xl border border-black/5 shadow-3xs mb-6">
                <h3 className="text-xs font-bold text-zinc-800 mb-4 uppercase tracking-wider">Biểu đồ giao dịch theo giờ (Triệu đ)</h3>
                <div className="w-full h-56">
                    <ResponsiveContainer width="100%" height="100%">
                        <LineChart data={getChartData()} margin={{ top: 10, right: 20, left: -20, bottom: 0 }}>
                            <XAxis dataKey="time" stroke="#a1a1aa" fontSize={10} tickLine={false} />
                            <YAxis stroke="#a1a1aa" fontSize={10} tickLine={false} axisLine={false} />
                            <Tooltip
                                contentStyle={{ backgroundColor: "#18181b", borderRadius: "8px", border: "none" }}
                                labelStyle={{ color: "#a1a1aa", fontSize: "10px", fontWeight: "bold" }}
                                itemStyle={{ color: "#ffffff", fontSize: "11px" }}
                                formatter={(value: any) => [`${value.toLocaleString()} Triệu đ`, "Doanh số giao dịch"]}
                            />
                            <Line
                                type="monotone"
                                dataKey="amount"
                                stroke="#3b82f6"
                                strokeWidth={2.5}
                                dot={{ r: 3, fill: "#3b82f6", strokeWidth: 2 }}
                                activeDot={{ r: 5 }}
                            />
                        </LineChart>
                    </ResponsiveContainer>
                </div>
            </div>

            {/* Bộ tìm kiếm thanh công cụ */}
            <div className="flex flex-wrap items-center justify-between gap-4 bg-white p-4 rounded-xl border border-black/5 shadow-3xs mb-4">
                <div className="flex h-9 w-96 items-center gap-2 rounded-lg border border-black/10 bg-zinc-50 px-3">
                    <span className="text-zinc-400 text-xs">🔎</span>
                    <input
                        className="w-full bg-transparent text-xs font-medium text-zinc-700 outline-none"
                        placeholder="Tìm mã giao dịch, số tài khoản, tên... (Ấn Enter)"
                        value={search}
                        onChange={e => setSearch(e.target.value)}
                        onKeyDown={handleSearchKeyDown}
                    />
                </div>

                <select
                    className="h-9 rounded-lg border border-black/10 bg-white px-3 text-xs font-semibold text-zinc-700 outline-none"
                    value={statusFilter}
                    onChange={e => setStatusFilter(e.target.value)}
                >
                    <option value="">Tất cả trạng thái</option>
                    <option value="completed">Thành công (completed)</option>
                    <option value="pending">Chờ xử lý (pending)</option>
                    <option value="failed">Thất bại (failed)</option>
                </select>
            </div>

            {/* Bảng kết xuất dữ liệu */}
            <div className="bg-white rounded-2xl border border-black/5 overflow-hidden shadow-xs">
                <table className="w-full text-left text-xs border-collapse">
                    <thead>
                        <tr className="bg-zinc-50 border-b border-zinc-100 text-zinc-400 font-bold uppercase text-[10px]">
                            <th className="p-4">Mã giao dịch</th>
                            <th className="p-4">Tài khoản nguồn</th>
                            <th className="p-4">Tài khoản đích</th>
                            <th className="p-4 text-right">Số tiền</th>
                            <th className="p-4 text-center">Loại</th>
                            <th className="p-4 font-mono">Thời gian</th>
                            <th className="p-4 text-center">Trạng thái</th>
                            <th className="p-4 text-right">Thao tác</th>
                        </tr>
                    </thead>
                    <tbody className="divide-y divide-zinc-100 font-semibold text-zinc-700">
                        {loading ? (
                            <tr><td colSpan={8} className="p-8 text-center text-zinc-400 animate-pulse">Đang đồng bộ dữ liệu giao dịch từ DB...</td></tr>
                        ) : transactions.length === 0 ? (
                            <tr><td colSpan={8} className="p-8 text-center text-zinc-400">Không tìm thấy bản ghi giao dịch nào.</td></tr>
                        ) : (
                            transactions.map((tx) => (
                                <tr key={tx.id} className="hover:bg-zinc-50/50">
                                    <td className="p-4 text-zinc-950 font-bold font-mono text-[11px]">{tx.transactionCode}</td>
                                    <td className="p-4">
                                        <div className="text-zinc-900">{tx.fromAccountName || "Hệ thống / Cash-in"}</div>
                                        {tx.fromAccountNumber && <div className="text-[10px] text-zinc-400 font-mono mt-0.5">{tx.fromAccountNumber}</div>}
                                    </td>
                                    <td className="p-4">
                                        <div className="text-zinc-900">{tx.toAccountName || "Hệ thống / Fee-collection"}</div>
                                        {tx.toAccountNumber && <div className="text-[10px] text-zinc-400 font-mono mt-0.5">{tx.toAccountNumber}</div>}
                                    </td>
                                    <td className="p-4 text-right font-mono font-bold text-zinc-950">
                                        {tx.amount.toLocaleString()} đ
                                    </td>
                                    <td className="p-4 text-center text-[10px] uppercase text-zinc-500">{tx.transactionType}</td>
                                    <td className="p-4 font-mono text-zinc-500">{new Date(tx.createdAt).toLocaleString("vi-VN")}</td>
                                    <td className="p-4 text-center">
                                        <span className={`px-2 py-0.5 rounded text-[10px] font-bold uppercase ${tx.status.toLowerCase() === "completed" ? "bg-green-50 text-green-700" :
                                            tx.status.toLowerCase() === "failed" ? "bg-red-50 text-red-700" : "bg-amber-50 text-amber-700"
                                            }`}>
                                            {tx.status}
                                        </span>
                                    </td>
                                    <td className="p-4 text-right">
                                        <button
                                            disabled={tx.status.toLowerCase() !== "completed" || tx.transactionType.toLowerCase() === "refund" || refundingId === tx.id}
                                            onClick={() => handleRefund(tx)}
                                            className="rounded-lg border border-black/5 bg-white px-3 py-1.5 text-[11px] font-bold text-blue-600 shadow-sm hover:bg-blue-50 disabled:cursor-not-allowed disabled:opacity-40"
                                        >
                                            {refundingId === tx.id ? "Đang hoàn..." : "Hoàn lại"}
                                        </button>
                                    </td>
                                </tr>
                            ))
                        )}
                    </tbody>
                </table>
            </div>
        </AdminShell>
    );
}