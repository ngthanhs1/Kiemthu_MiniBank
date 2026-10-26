"use client";

import { useEffect, useState } from "react";
import AdminShell from "../../components/admin-shell";

type BankAccountItem = {
    id: number;
    accountNumber: string;
    accountName: string;
    customerName: string;
    accountType: string;
    actualBalance: number;
    holdingBalance: number;
    availableBalance: number;
    status: string;
    limitPerDay: number;
    createdAt: string;
};

export default function AdminBankAccountsPage() {
    const [accounts, setAccounts] = useState<BankAccountItem[]>([]);
    const [loading, setLoading] = useState(true);
    const [search, setSearch] = useState("");
    const [statusFilter, setStatusFilter] = useState("");

    // Trạng thái điều khiển Modals và xử lý dữ liệu dòng tiền
    const [activeModal, setActiveModal] = useState<"detail" | "qr" | "deposit" | "withdraw" | null>(null);
    const [selectedAccount, setSelectedAccount] = useState<BankAccountItem | null>(null);
    const [amountInput, setAmountInput] = useState("");
    const [isSubmitting, setIsSubmitting] = useState(false);
    const [toastMessage, setToastMessage] = useState<string | null>(null);

    const toNumber = (value: unknown, fallback = 0) => {
        if (value === null || value === undefined || value === "") return fallback;
        const parsed = Number(value);
        return Number.isFinite(parsed) ? parsed : fallback;
    };

    const showToast = (msg: string) => {
        setToastMessage(msg);
        setTimeout(() => setToastMessage(null), 4000);
    };

    // 1. ĐỒNG BỘ TẢI DANH SÁCH TÀI KHOẢN TỪ DATABASE
    const loadData = async () => {
        try {
            setLoading(true);
            const token = localStorage.getItem("adminToken") || localStorage.getItem("token");
            const headers = { "Authorization": `Bearer ${token}`, "Content-Type": "application/json" };

            let url = `${process.env.NEXT_PUBLIC_API_BASE_URL}/api/admin/accounts`;
            const params = new URLSearchParams();
            if (search) params.append("q", search);
            if (statusFilter) params.append("status", statusFilter);
            if (params.toString()) url += `?${params.toString()}`;

            const res = await fetch(url, { headers });
            if (res.ok) {
                const data = await res.json();

                const formattedData = (Array.isArray(data) ? data : []).map((item: any) => {
                    // Cơ chế bóc tách tên thông minh tránh lỗi hiển thị cứng "Nguyễn Văn A"
                    const resolvedOwnerName =
                        item.customerName ||
                        item.accountName ||
                        item.fullName ||
                        item.user?.fullName ||
                        item.customer?.fullName ||
                        "Khách hàng hệ thống";

                    const actualBalance = toNumber(item.actualBalance ?? item.balance ?? item.currentBalance);
                    const holdingBalance = toNumber(item.holdingBalance ?? item.holdBalance ?? item.heldBalance);
                    const availableBalance = toNumber(item.availableBalance, actualBalance - holdingBalance);

                    return {
                        id: item.id,
                        accountNumber: item.accountNumber || "---",
                        accountName: item.accountName || "Tài khoản thanh toán",
                        customerName: resolvedOwnerName,
                        accountType: item.accountType || "payment",
                        actualBalance,
                        holdingBalance,
                        availableBalance,
                        status: item.status || "ACTIVE",
                        limitPerDay: item.limitPerDay || 100000000,
                        createdAt: item.createdAt || "2026-01-15"
                    };
                });
                setAccounts(formattedData);
            }
        } catch (error) {
            console.error("Lỗi đồng bộ danh sách tài khoản ngân hàng:", error);
        } finally {
            setLoading(false);
        }
    };

    // Tự động reload danh sách khi thay đổi bộ lọc trạng thái nhanh
    useEffect(() => {
        loadData();
    }, [statusFilter]);

    const handleSearchKeyDown = (e: React.KeyboardEvent) => {
        if (e.key === "Enter") loadData();
    };

    // 2. CHỨC NĂNG KHÓA / MỞ KHÓA (Cơ chế quét kép PUT & PATCH bao vây lỗi 405)
    const toggleLockAccount = async (acc: BankAccountItem) => {
        try {
            const token = localStorage.getItem("adminToken") || localStorage.getItem("token");
            const isLocked = acc.status === "LOCKED" || acc.status === "Đang khóa";
            const newStatus = isLocked ? "ACTIVE" : "LOCKED";

            const payload = { status: newStatus };

            // Thử nghiệm cấu trúc 1: Gửi qua PUT Method phổ thông
            let res = await fetch(`${process.env.NEXT_PUBLIC_API_BASE_URL}/api/admin/accounts/${acc.accountNumber}/status`, {
                method: "PUT",
                headers: { "Authorization": `Bearer ${token}`, "Content-Type": "application/json" },
                body: JSON.stringify(payload)
            });

            // Fallback cấu trúc 2: Nếu Server chặn 405/404, tự động chuyển đổi sang PATCH Method hoặc Request Parameter
            if (!res.ok) {
                res = await fetch(`${process.env.NEXT_PUBLIC_API_BASE_URL}/api/admin/accounts/${acc.accountNumber}/status`, {
                    method: "PATCH",
                    headers: { "Authorization": `Bearer ${token}`, "Content-Type": "application/json" },
                    body: JSON.stringify(payload)
                });
            }

            if (res.ok) {
                showToast(isLocked ? `Mở khóa thành công tài khoản ${acc.accountNumber}` : `Đã khóa thành công tài khoản ${acc.accountNumber}`);
                loadData();
            } else {
                showToast("Lỗi cập nhật trạng thái. Hãy kiểm tra cấu trúc API Server.");
            }
        } catch (error) {
            console.error("Lỗi thực thi khóa tài khoản:", error);
        }
    };

    // 3. CHỨC NĂNG ĐIỀU CHỈNH SỐ DƯ (Bao vây lỗi 404 bằng cơ chế định tuyến kép)
    const handleBalanceAdjustment = async () => {
        const amount = parseFloat(amountInput);
        if (!amount || amount <= 0 || !selectedAccount) return;

        try {
            setIsSubmitting(true);
            const token = localStorage.getItem("adminToken") || localStorage.getItem("token");
            const action = activeModal === "deposit" ? "deposit-test" : "withdraw-test";

            // Cấu hình URL dạng 1: Truyền trực tiếp Số tài khoản vào Path Variable
            let url = `${process.env.NEXT_PUBLIC_API_BASE_URL}/api/admin/accounts/${selectedAccount.accountNumber}/${action}`;
            let res = await fetch(url, {
                method: "POST",
                headers: { "Authorization": `Bearer ${token}`, "Content-Type": "application/json" },
                body: JSON.stringify({ amount: amount, description: "Admin can thiệp điều chỉnh số dư số liệu" })
            });

            // Fallback cấu hình URL dạng 2: Nếu lỗi 404, thử lại bằng cách đẩy Số tài khoản qua Query String (?accountNumber=...)
            if (!res.ok) {
                url = `${process.env.NEXT_PUBLIC_API_BASE_URL}/api/admin/accounts/${action}?accountNumber=${selectedAccount.accountNumber}`;
                res = await fetch(url, {
                    method: "POST",
                    headers: { "Authorization": `Bearer ${token}`, "Content-Type": "application/json" },
                    body: JSON.stringify({ amount: amount })
                });
            }

            if (res.ok) {
                showToast(activeModal === "deposit"
                    ? `Đã cộng thành công +${amount.toLocaleString()} đ cho ${selectedAccount.customerName}`
                    : `Đã khấu trừ thành công -${amount.toLocaleString()} đ từ ${selectedAccount.customerName}`
                );
                setActiveModal(null);
                setAmountInput("");
                loadData();
            } else {
                showToast("Giao dịch thất bại. Vui lòng xác minh lại quyền Admin hoặc kết nối.");
            }
        } catch (error) {
            console.error("Lỗi can thiệp dòng tiền test:", error);
        } finally {
            setIsSubmitting(false);
        }
    };

    const totalAccounts = accounts.length;
    const activeAccounts = accounts.filter(acc => acc.status === "ACTIVE" || acc.status === "Hoạt động").length;
    const lockedAccounts = accounts.filter(acc => acc.status === "LOCKED" || acc.status === "Đang khóa").length;
    const totalSystemBalance = accounts.reduce((sum, acc) => sum + acc.actualBalance, 0);

    return (
        <AdminShell title="Quản lý tài khoản ngân hàng" subtitle="Quản lý danh sách, số dư và trạng thái bảo mật của hệ thống Core Banking">

            <style jsx global>{`
                body, input, select, button, table, div, span, h3 {
                    font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif !important;
                    letter-spacing: -0.01em;
                }
            `}</style>

            {/* Thông báo Toast dạng mờ mịn hiện đại */}
            {toastMessage && (
                <div className="fixed top-5 right-5 z-50 flex items-center gap-2 bg-zinc-900 text-white text-xs font-semibold px-4 py-3 rounded-xl shadow-xl border border-white/10 transition-all">
                    <svg className="w-4 h-4 text-emerald-400" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={3}>
                        <path strokeLinecap="round" strokeLinejoin="round" d="M5 13l4 4L19 7" />
                    </svg>
                    <span>{toastMessage}</span>
                </div>
            )}

            {/* Thanh tìm kiếm & bộ lọc */}
            <div className="flex flex-wrap items-center justify-between gap-4 bg-white p-4 rounded-xl border border-black/5 shadow-3xs mb-4">
                <div className="flex h-9 w-96 items-center gap-2 rounded-lg border border-black/10 bg-zinc-50 px-3">
                    <svg className="w-4 h-4 text-zinc-400" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2.5}>
                        <path strokeLinecap="round" strokeLinejoin="round" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
                    </svg>
                    <input
                        className="w-full bg-transparent text-xs font-semibold text-zinc-700 outline-none"
                        placeholder="Tìm theo STK, tên chủ tài khoản... (Ấn Enter)"
                        value={search}
                        onChange={e => setSearch(e.target.value)}
                        onKeyDown={handleSearchKeyDown}
                    />
                </div>

                <select
                    className="h-9 rounded-lg border border-black/10 bg-white px-3 text-xs font-bold text-zinc-700 outline-none cursor-pointer"
                    value={statusFilter}
                    onChange={e => setStatusFilter(e.target.value)}
                >
                    <option value="">Tất cả trạng thái</option>
                    <option value="ACTIVE">Hoạt động</option>
                    <option value="LOCKED">Đã khóa</option>
                </select>
            </div>

            {/* Widgets thống kê nhanh */}
            <div className="grid grid-cols-2 md:grid-cols-4 gap-4 bg-white p-4 rounded-xl border border-black/5 shadow-3xs mb-6">
                <div><div className="text-[10px] font-bold text-zinc-400 uppercase">Tổng số tài khoản</div><div className="text-lg font-black text-zinc-900 mt-0.5">{totalAccounts}</div></div>
                <div><div className="text-[10px] font-bold text-green-600 uppercase">Đang hoạt động</div><div className="text-lg font-black text-green-600 mt-0.5">{activeAccounts}</div></div>
                <div><div className="text-[10px] font-bold text-red-500 uppercase">Đã khóa</div><div className="text-lg font-black text-red-500 mt-0.5">{lockedAccounts}</div></div>
                <div><div className="text-[10px] font-bold text-blue-600 uppercase">Tổng số dư hệ thống</div><div className="text-lg font-black text-blue-600 mt-0.5">{totalSystemBalance.toLocaleString()} đ</div></div>
            </div>

            {/* Bảng kết xuất dữ liệu */}
            <div className="bg-white rounded-2xl border border-black/5 overflow-hidden shadow-xs">
                <table className="w-full text-left text-xs border-collapse">
                    <thead>
                        <tr className="bg-zinc-50 border-b border-zinc-100 text-zinc-400 font-bold text-[10px] uppercase tracking-wider">
                            <th className="p-4 font-mono">STK</th>
                            <th className="p-4">Tên tài khoản</th>
                            <th className="p-4">Chủ tài khoản</th>
                            <th className="p-4 text-center">Loại TK</th>
                            <th className="p-4 text-right">Số dư thực tế</th>
                            <th className="p-4 text-right text-amber-600">Đang tạm giữ</th>
                            <th className="p-4 text-right text-zinc-950">Số dư khả dụng</th>
                            <th className="p-4 text-center">Trạng thái</th>
                            <th className="p-4 text-center">Thao tác</th>
                        </tr>
                    </thead>
                    <tbody className="divide-y divide-zinc-100 font-semibold text-zinc-700">
                        {loading ? (
                            <tr><td colSpan={9} className="p-8 text-center text-zinc-400 animate-pulse">Đang nạp dữ liệu tài khoản từ Database Core...</td></tr>
                        ) : accounts.length === 0 ? (
                            <tr><td colSpan={9} className="p-8 text-center text-zinc-400">Không tìm thấy dữ liệu tài khoản phù hợp.</td></tr>
                        ) : (
                            accounts.map((acc) => {
                                const isItemLocked = acc.status === "LOCKED" || acc.status === "Đang khóa";
                                return (
                                    <tr key={acc.id} className="hover:bg-zinc-50/50 transition-colors">
                                        <td className="p-4 font-bold font-mono text-zinc-900 text-[12px]">{acc.accountNumber}</td>
                                        <td className="p-4 text-zinc-400 text-[11px] font-normal">{acc.accountName}</td>

                                        <td className="p-4">
                                            <div className="flex items-center gap-2">
                                                <div className="w-6 h-6 rounded-full bg-blue-50 text-blue-600 flex items-center justify-center text-[9px] font-black">
                                                    {acc.customerName.split(" ").pop()?.substring(0, 2).toUpperCase()}
                                                </div>
                                                <span className="text-zinc-900 font-bold">{acc.customerName}</span>
                                            </div>
                                        </td>

                                        <td className="p-4 text-center">
                                            <span className="px-2 py-0.5 rounded text-[9px] font-bold bg-zinc-100 text-zinc-700 uppercase">{acc.accountType}</span>
                                        </td>
                                        <td className="p-4 text-right font-mono text-zinc-900">{acc.actualBalance.toLocaleString()} đ</td>
                                        <td className={`p-4 text-right font-mono ${acc.holdingBalance > 0 ? "text-amber-600 font-bold" : "text-zinc-300 font-normal"}`}>{acc.holdingBalance > 0 ? `${acc.holdingBalance.toLocaleString()} đ` : "-"}</td>
                                        <td className="p-4 text-right font-mono font-bold text-zinc-950">{acc.availableBalance.toLocaleString()} đ</td>
                                        <td className="p-4 text-center">
                                            <span className={`px-2 py-0.5 rounded text-[9px] font-bold ${isItemLocked ? "bg-red-50 text-red-700" : "bg-green-50 text-green-700"}`}>{isItemLocked ? "Đã khóa" : "Hoạt động"}</span>
                                        </td>

                                        <td className="p-4 text-center">
                                            <div className="flex items-center justify-center gap-3">
                                                <button onClick={() => { setSelectedAccount(acc); setActiveModal("detail"); }} className="text-zinc-400 hover:text-zinc-900 transition-colors" title="Xem chi tiết">
                                                    <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2.5}>
                                                        <path strokeLinecap="round" strokeLinejoin="round" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
                                                        <path strokeLinecap="round" strokeLinejoin="round" d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z" />
                                                    </svg>
                                                </button>
                                                <button onClick={() => { setSelectedAccount(acc); setActiveModal("qr"); }} className="text-zinc-400 hover:text-zinc-900 transition-colors" title="Mã QR tài khoản">
                                                    <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2.5}>
                                                        <path strokeLinecap="round" strokeLinejoin="round" d="M12 4v1m6 11h2m-6 0h-2v4m0-11v3m0 0h.01M12 12h.01M16 12h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
                                                    </svg>
                                                </button>
                                                <button onClick={() => toggleLockAccount(acc)} className={`transition-colors ${isItemLocked ? "text-red-500 hover:text-red-600" : "text-zinc-400 hover:text-zinc-900"}`} title="Khóa/Mở khóa">
                                                    <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2.5}>
                                                        <path strokeLinecap="round" strokeLinejoin="round" d={isItemLocked ? "M12 15v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2zm10-10V7a4 4 0 00-8 0v4h8z" : "M8 11V7a4 4 0 118 0m-4 8v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2z"} />
                                                    </svg>
                                                </button>
                                                <button onClick={() => { setSelectedAccount(acc); setActiveModal("deposit"); }} className="text-emerald-500 hover:text-emerald-600 transition-colors" title="Cộng tiền test">
                                                    <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={3}>
                                                        <path strokeLinecap="round" strokeLinejoin="round" d="M12 4v16m8-8H4" />
                                                    </svg>
                                                </button>
                                                <button onClick={() => { setSelectedAccount(acc); setActiveModal("withdraw"); }} className="text-zinc-400 hover:text-zinc-900 transition-colors" title="Trừ tiền test">
                                                    <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={3}>
                                                        <path strokeLinecap="round" strokeLinejoin="round" d="M20 12H4" />
                                                    </svg>
                                                </button>
                                            </div>
                                        </td>
                                    </tr>
                                );
                            })
                        )}
                    </tbody>
                </table>
            </div>

            {/* --- DIALOG MODALS KHỐI CHỨC NĂNG --- */}
            {activeModal && selectedAccount && (
                <div className="fixed inset-0 z-40 flex items-center justify-center bg-zinc-900/30 backdrop-blur-xs">
                    <div className="bg-white rounded-2xl w-[440px] border border-black/5 shadow-2xl p-6 relative overflow-hidden text-zinc-800">

                        <button onClick={() => { setActiveModal(null); setAmountInput(""); }} className="absolute top-4 right-4 text-zinc-400 hover:text-zinc-600 font-bold text-sm">✕</button>

                        {/* Modal 1: Xem chi tiết số dư */}
                        {activeModal === "detail" && (
                            <div>
                                <h3 className="text-xs font-bold uppercase tracking-wider text-zinc-400 mb-4">📋 Chi tiết tài khoản</h3>
                                <div className="grid grid-cols-2 gap-y-3 gap-x-4 text-xs border-b border-zinc-100 pb-4 mb-4">
                                    <div><div className="text-zinc-400 font-bold text-[10px] uppercase">Số tài khoản</div><div className="font-mono font-bold text-zinc-900 text-sm mt-0.5">{selectedAccount.accountNumber}</div></div>
                                    <div><div className="text-zinc-400 font-bold text-[10px] uppercase">Tên tài khoản</div><div className="font-bold text-zinc-900 mt-0.5">{selectedAccount.accountName}</div></div>
                                    <div><div className="text-zinc-400 font-bold text-[10px] uppercase">Chủ tài khoản</div><div className="font-bold text-emerald-700 mt-0.5">{selectedAccount.customerName}</div></div>
                                    <div><div className="text-zinc-400 font-bold text-[10px] uppercase">Ngày khởi tạo</div><div className="font-bold text-zinc-900 mt-0.5">{selectedAccount.createdAt}</div></div>
                                </div>
                                <div className="bg-zinc-50 p-3 rounded-xl space-y-2 text-xs">
                                    <div className="flex justify-between"><span className="text-zinc-500">Số dư thực tế:</span><span className="font-mono font-bold text-zinc-900">{selectedAccount.actualBalance.toLocaleString()} đ</span></div>
                                    <div className="flex justify-between text-amber-600"><span className="font-medium">Đang tạm giữ:</span><span className="font-mono font-bold">-{selectedAccount.holdingBalance.toLocaleString()} đ</span></div>
                                    <div className="flex justify-between border-t border-zinc-200/60 pt-2 text-zinc-950 font-bold"><span>Số dư khả dụng:</span><span className="font-mono text-emerald-600">{selectedAccount.availableBalance.toLocaleString()} đ</span></div>
                                </div>
                            </div>
                        )}

                        {/* Modal 2: Mã QR hiển thị tên động */}
                        {activeModal === "qr" && (
                            <div className="text-center py-2">
                                <h3 className="text-xs font-bold uppercase tracking-wider text-zinc-400 mb-4">🔲 Mã QR Tài Khoản</h3>
                                <div className="w-40 h-40 bg-zinc-50 mx-auto rounded-xl border border-zinc-200 p-4 flex flex-col items-center justify-center shadow-inner mb-4 relative">
                                    <div className="absolute top-2 left-2 w-4 h-4 border-t-2 border-l-2 border-zinc-900"></div>
                                    <div className="absolute top-2 right-2 w-4 h-4 border-t-2 border-r-2 border-zinc-900"></div>
                                    <div className="absolute bottom-2 left-2 w-4 h-4 border-b-2 border-l-2 border-zinc-900"></div>
                                    <div className="absolute bottom-2 right-2 w-4 h-4 border-b-2 border-r-2 border-zinc-900"></div>
                                    <span className="text-zinc-400 font-mono text-[9px] font-bold">CORE BANKING QR</span>
                                </div>
                                <div className="text-xs font-mono font-bold text-zinc-900">STK: {selectedAccount.accountNumber}</div>
                                <div className="text-[10px] font-bold text-zinc-400 uppercase tracking-wider mt-0.5">{selectedAccount.customerName}</div>
                            </div>
                        )}

                        {/* Modal 3 & 4: Biểu mẫu cộng / trừ tiền test */}
                        {(activeModal === "deposit" || activeModal === "withdraw") && (
                            <div>
                                <h3 className="text-sm font-black text-zinc-900 mb-1">{activeModal === "deposit" ? " Tăng số dư" : " Giảm số dư"}</h3>
                                <div className="text-zinc-400 text-[11px] mb-4">Chủ tài khoản thụ hưởng: <span className="font-bold text-zinc-900">{selectedAccount.customerName}</span> (<span className="font-mono">{selectedAccount.accountNumber}</span>)</div>

                                <div className="space-y-1.5 mb-5">
                                    <label className="text-[10px] font-bold text-zinc-400 uppercase tracking-wider">Số tiền cần xử lý (đ)</label>
                                    <input
                                        type="number"
                                        className="w-full h-10 border border-zinc-200 rounded-xl px-3 text-sm font-bold text-zinc-800 bg-zinc-50 outline-none focus:border-zinc-400 focus:bg-white transition-all"
                                        placeholder="Nhập số tiền..."
                                        value={amountInput}
                                        onChange={e => setAmountInput(e.target.value)}
                                        disabled={isSubmitting}
                                    />
                                </div>

                                <div className="flex items-center justify-end gap-2 text-xs font-bold">
                                    <button onClick={() => { setActiveModal(null); setAmountInput(""); }} className="h-9 px-4 border border-zinc-200 text-zinc-500 rounded-xl hover:bg-zinc-50" disabled={isSubmitting}>Hủy bỏ</button>
                                    <button onClick={handleBalanceAdjustment} className={`h-9 px-5 text-white rounded-xl active:scale-95 flex items-center gap-1 transition-all ${activeModal === "deposit" ? "bg-emerald-600 hover:bg-emerald-700" : "bg-zinc-900 hover:bg-black"}`} disabled={isSubmitting}>
                                        {isSubmitting && <span className="w-3 h-3 border-2 border-white border-t-transparent rounded-full animate-spin"></span>}
                                        Xác nhận
                                    </button>
                                </div>
                            </div>
                        )}

                    </div>
                </div>
            )}
        </AdminShell>
    );
}