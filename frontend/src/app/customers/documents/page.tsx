"use client";

import { useCallback, useEffect, useMemo, useState } from "react";
import { Search, ChevronDown, CheckCircle2, XCircle, AlertCircle, FileText, Eye, RefreshCw, X } from "lucide-react";
import AdminShell from "../../components/admin-shell";
const API_BASE = (
    process.env.NEXT_PUBLIC_API_BASE_URL ??
    process.env.NEXT_PUBLIC_API_URL ??
    "http://localhost:8080"
).replace(/\/+$/, "");

// --- CẤU TRÚC ĐỐI TƯỢNG ĐỒNG BỘ 100% VỚI RECORD DOCUMENTSUMMARY TRONG DB ---
type DocumentSummary = {
    id: number;
    ownerType: string;       // Khớp với cột owner_type trong DB (CUSTOMER, LOAN,...)
    ownerId: number;         // Khớp với cột owner_id
    documentType: string;    // Khớp với cột document_type (IDENTITY_CARD, INCOME_PROOF,...)
    fileName: string | null; // Khớp với cột file_name
    fileUrl: string;         // Đường dẫn URL lưu trong DB để xem/tải file
    mimeType: string | null; // Định dạng file (image/png, application/pdf)
    verifiedStatus: "pending" | "approved" | "rejected" | "PENDING" | "APPROVED" | "REJECTED";
    uploadedByType: string;
    uploadedById: number;
    uploadedAt: string;      // Thời gian Instant từ DB chuyển về dạng chuỗi ISO
    verifiedById: number | null;
    verifiedAt: string | null;
    note: string | null;     // Cột ghi chú hoặc lý do từ chối
};

const resolveFileUrl = (fileUrl?: string | null) => {
    if (!fileUrl) return "";
    if (/^(https?:|blob:|data:)/i.test(fileUrl)) return fileUrl;
    return `${API_BASE}${fileUrl.startsWith("/") ? "" : "/"}${fileUrl}`;
};

const safeText = (value?: string | number | null) => String(value ?? "");

const formatUploadedAt = (value?: string | null) => {
    if (!value) return "--";
    const date = new Date(value);
    return Number.isNaN(date.getTime()) ? "--" : date.toLocaleString("vi-VN");
};

const statusOptions = [
    { value: "ALL", label: "Tất cả trạng thái DB" },
    { value: "PENDING", label: "Chờ xem (PENDING)" },
    { value: "APPROVED", label: "Đã phê duyệt (APPROVED)" },
    { value: "REJECTED", label: "Đã từ chối (REJECTED)" },
];

export default function AdminCustomerDocumentsPage() {
    const [token] = useState<string | null>(() => {
        if (typeof window === "undefined") return null;
        return localStorage.getItem("adminToken");
    });
    const [documents, setDocuments] = useState<DocumentSummary[]>([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);

    // Bộ lọc tìm kiếm & trạng thái liên kết giao diện
    const [searchQuery, setSearchQuery] = useState("");
    const [statusFilter, setStatusFilter] = useState("ALL");
    const [isDropdownOpen, setIsDropdownOpen] = useState(false);

    // States quản lý Modal xem hồ sơ
    const [selectedDoc, setSelectedDoc] = useState<DocumentSummary | null>(null);
    const [reviewNote, setReviewNote] = useState("");

    // ==========================================
    // HÀM 1: TRUY VẤN ĐỌC DỮ LIỆU TỪ DATABASE (API GET)
    // ==========================================
    const fetchDocumentsFromDB = useCallback(async () => {
        if (!token) return;
        setLoading(true);
        setError(null);
        try {
            // Tab này chỉ lấy các tài liệu do khách hàng upload (CCCD, ảnh minh chứng, hồ sơ...).
            const res = await fetch(`${API_BASE}/api/admin/documents?ownerType=USER&page=0&size=100`, {
                method: "GET",
                headers: {
                    "Content-Type": "application/json",
                    Authorization: `Bearer ${token}`, // Chứng thực quyền Admin để DB cho phép đọc dữ liệu
                },
            });

            if (!res.ok) {
                const detail = await res.text();
                throw new Error(`Lỗi máy chủ (${res.status}): ${detail || "Không thể kết nối truy vấn cơ sở dữ liệu."}`);
            }

            const data = await res.json();

            if (data && data.items) {
                setDocuments(data.items as DocumentSummary[]);
            } else if (data && data.content) {
                setDocuments(data.content as DocumentSummary[]);
            } else if (Array.isArray(data)) {
                setDocuments(data as DocumentSummary[]);
            } else {
                setDocuments([]);
            }
        } catch (err) {
            setError(err instanceof Error ? err.message : "Thất bại khi đồng bộ hóa luồng dữ liệu DB.");
        } finally {
            setLoading(false);
        }
    }, [token]);

    useEffect(() => {
        // eslint-disable-next-line react-hooks/set-state-in-effect
        fetchDocumentsFromDB();
    }, [fetchDocumentsFromDB]);

    // Hàm lọc tìm kiếm Client-side tiện dụng trên tập dữ liệu đã tải từ DB về
    const filteredDocs = useMemo(() => {
        const q = searchQuery.toLowerCase().trim();
        return documents.filter((doc) => {
            const matchesSearch = !q ||
                safeText(doc.fileName).toLowerCase().includes(q) ||
                safeText(doc.ownerId).includes(q) ||
                safeText(doc.documentType).toLowerCase().includes(q);
            const matchesStatus = statusFilter === "ALL" || doc.verifiedStatus?.toUpperCase() === statusFilter;
            return matchesSearch && matchesStatus;
        });
    }, [documents, searchQuery, statusFilter]);

    // Giải nghĩa chuỗi viết hoa trong DB thành tên hiển thị tiếng Việt trực quan
    const translateDocType = (type: string) => {
        switch (type?.toUpperCase()) {
            case "IDENTITY_CARD": return "Căn cước công dân (CCCD)";
            case "PASSPORT": return "Hộ chiếu (Passport)";
            case "INCOME_PROOF": return "Chứng minh thu nhập / Sao kê";
            case "LOAN_AGREEMENT": return "Khế ước / Hợp đồng tín dụng";
            default: return type || "Tài liệu khác";
        }
    };

    if (!token) {
        return (
            <div className="flex min-h-screen items-center justify-center bg-zinc-50 p-6 text-xs text-zinc-400">
                Yêu cầu mã định danh quyền truy cập Admin để liên kết Cơ sở dữ liệu...
            </div>
        );
    }

    return (
        <AdminShell title=" Tài liệu khách hàng" subtitle="Quản lý tất cả tài liệu và giấy tờ của khách hàng">
            <div className="space-y-5 font-sans text-zinc-800">

                <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between bg-white p-4 rounded-2xl border border-black/5 shadow-sm">
                    <div className="relative flex-1 max-w-md">
                        <Search className="absolute left-3 top-1/2 -translate-y-1/2 text-zinc-400" size={15} />
                        <input
                            type="text"
                            placeholder="Tìm kiếm dữ liệu theo tên tệp, Mã khách hàng, Loại hồ sơ..."
                            value={searchQuery}
                            onChange={(e) => setSearchQuery(e.target.value)}
                            className="h-9 w-full rounded-xl border border-black/10 bg-zinc-50/50 pl-9 pr-4 text-xs outline-none focus:border-orange-600 focus:bg-white transition-all"
                        />
                    </div>

                    <div className="relative w-full sm:w-52">
                        <button
                            type="button"
                            onClick={() => setIsDropdownOpen(!isDropdownOpen)}
                            className="flex h-9 w-full items-center justify-between rounded-xl border border-black/10 bg-white px-3 text-left text-xs font-medium text-zinc-700 transition"
                        >
                            <span>{statusOptions.find((o) => o.value === statusFilter)?.label}</span>
                            <ChevronDown size={14} className={`text-zinc-400 transition-transform ${isDropdownOpen ? "rotate-180" : ""}`} />
                        </button>
                        {isDropdownOpen && (
                            <div className="absolute right-0 top-full z-50 mt-1 w-full overflow-hidden rounded-xl border border-zinc-100 bg-white py-1 shadow-xl">
                                {statusOptions.map((o) => (
                                    <button
                                        key={o.value}
                                        type="button"
                                        onClick={() => { setStatusFilter(o.value); setIsDropdownOpen(false); }}
                                        className="w-full px-4 py-2 text-left text-xs text-zinc-600 hover:bg-zinc-50"
                                    >
                                        {o.label}
                                    </button>
                                ))}
                            </div>
                        )}
                    </div>
                </div>

                {error && (
                    <div className="flex items-center gap-2 rounded-xl border border-red-100 bg-red-50 p-3 text-xs text-red-600">
                        <AlertCircle size={14} /> {error}
                        <button onClick={fetchDocumentsFromDB} className="ml-auto underline flex items-center gap-1"><RefreshCw size={12} />Làm mới liên kết DB</button>
                    </div>
                )}

                {/* BẢNG DỮ LIỆU ĐỌC RA TỪ DATABASE */}
                <div className="overflow-hidden rounded-2xl border border-black/5 bg-white shadow-sm">
                    <div className="overflow-x-auto">
                        <table className="w-full border-collapse text-left text-xs">
                            <thead>
                                <tr className="border-b border-zinc-100 bg-zinc-50/70 font-bold uppercase text-zinc-500">
                                    <th className="px-4 py-3.5">ID bản ghi</th>
                                    <th className="px-4 py-3.5">ID Khách hàng (DB Owner)</th>
                                    <th className="px-4 py-3.5">Loại văn bản</th>
                                    <th className="px-4 py-3.5">Tên tệp lưu trữ</th>
                                    <th className="px-4 py-3.5">Ngày khởi tạo</th>
                                    <th className="px-4 py-3.5">Trạng thái phê duyệt</th>
                                    <th className="px-4 py-3.5 text-center">Thao tác dữ liệu</th>
                                </tr>
                            </thead>
                            <tbody className="divide-y divide-zinc-100 text-zinc-600">
                                {loading ? (
                                    <tr><td colSpan={7} className="py-8 text-center text-zinc-400 animate-pulse">Đang thực hiện truy vấn luồng dữ liệu từ DB...</td></tr>
                                ) : filteredDocs.length === 0 ? (
                                    <tr><td colSpan={7} className="py-8 text-center text-zinc-400">Không có bản ghi dữ liệu tài liệu nào khớp điều kiện lọc.</td></tr>
                                ) : (
                                    filteredDocs.map((doc) => (
                                        <tr key={doc.id} className="transition-colors hover:bg-zinc-50/30">
                                            <td className="px-4 py-3.5 font-mono font-semibold text-zinc-400">#{doc.id}</td>
                                            <td className="px-4 py-3.5 font-bold text-blue-600">KH-{doc.ownerId} <span className="text-[10px] text-zinc-400 font-normal">({doc.ownerType})</span></td>
                                            <td className="px-4 py-3.5 font-medium text-zinc-900">{translateDocType(doc.documentType)}</td>
                                            <td className="px-4 py-3.5 max-w-xs truncate font-mono text-zinc-600 flex items-center gap-1.5 py-4">
                                                <FileText size={14} className="text-zinc-400 flex-shrink-0" />
                                                <span className="truncate" title={doc.fileName ?? doc.fileUrl}>{doc.fileName ?? doc.fileUrl}</span>
                                            </td>
                                            <td className="px-4 py-3.5 text-zinc-500">{formatUploadedAt(doc.uploadedAt)}</td>
                                            <td className="px-4 py-3.5">
                                                <span className={`inline-flex items-center gap-1 rounded-full border px-2.5 py-0.5 text-[10px] font-bold ${doc.verifiedStatus?.toUpperCase() === "APPROVED" ? "border-emerald-100 bg-emerald-50 text-emerald-600" :
                                                    doc.verifiedStatus?.toUpperCase() === "REJECTED" ? "border-red-100 bg-red-50 text-red-600" :
                                                        "border-amber-100 bg-amber-50 text-amber-600"
                                                    }`}>
                                                    {doc.verifiedStatus?.toUpperCase() === "APPROVED" ? <CheckCircle2 size={11} /> :
                                                        doc.verifiedStatus?.toUpperCase() === "REJECTED" ? <XCircle size={11} /> :
                                                            <AlertCircle size={11} />}
                                                    {doc.verifiedStatus?.toUpperCase() === "APPROVED" ? "Đã duyệt" : doc.verifiedStatus?.toUpperCase() === "REJECTED" ? "Bị từ chối" : "Chờ xem"}
                                                </span>
                                            </td>
                                            <td className="px-4 py-3.5 text-center">
                                                <button
                                                    type="button"
                                                    onClick={() => { setSelectedDoc(doc); setReviewNote(doc.note ?? ""); }}
                                                    className="inline-flex h-7 items-center gap-1 rounded-lg border border-black/10 bg-white px-2.5 text-[11px] font-bold text-zinc-700 shadow-sm hover:bg-zinc-50 transition"
                                                >
                                                    <Eye size={12} /> Xem / tải về
                                                </button>
                                            </td>
                                        </tr>
                                    ))
                                )}
                            </tbody>
                        </table>
                    </div>
                </div>

                {/* CỬA SỔ XEM CHI TIẾT TÀI LIỆU (MODAL) */}
                {selectedDoc && (
                    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 backdrop-blur-sm p-4 animate-fadeIn">
                        <div className="w-full max-w-5xl h-[85vh] flex flex-col overflow-hidden rounded-2xl bg-white shadow-2xl border border-black/5 animate-in scale-in duration-150">

                            {/* Tiêu đề Modal */}
                            <div className="flex items-center justify-between border-b border-zinc-100 px-5 py-3.5 bg-zinc-50">
                                <div>
                                    <h3 className="text-xs font-bold text-zinc-900">Xem tài liệu ID bản ghi: #{selectedDoc.id}</h3>
                                    <p className="text-[10px] text-zinc-500 mt-0.5">Tên tệp gốc: {selectedDoc.fileName ?? selectedDoc.fileUrl} | Phân loại: {translateDocType(selectedDoc.documentType)}</p>
                                </div>
                                <button type="button" onClick={() => setSelectedDoc(null)} className="text-zinc-400 hover:text-zinc-600"><X size={16} /></button>
                            </div>

                            {/* Thân Modal phân chia 2 khu vực */}
                            <div className="flex-1 flex flex-col md:flex-row overflow-hidden">

                                {/* Khối xem trước File (Đọc liên kết từ trường fileUrl của DB) */}
                                <div className="flex-1 bg-zinc-100 p-4 flex items-center justify-center overflow-auto border-r border-zinc-200">
                                    {selectedDoc.mimeType?.startsWith("image/") ? (
                                        // eslint-disable-next-line @next/next/no-img-element
                                        <img
                                            src={resolveFileUrl(selectedDoc.fileUrl)}
                                            alt="Tài liệu đính kèm khách hàng"
                                            className="max-w-full max-h-full object-contain rounded-lg shadow animate-fadeIn"
                                        />
                                    ) : selectedDoc.mimeType === "application/pdf" ? (
                                        <iframe
                                            src={`${resolveFileUrl(selectedDoc.fileUrl)}#toolbar=0`}
                                            className="w-full h-full rounded-lg shadow bg-white"
                                            title="PDF Preview"
                                        />
                                    ) : (
                                        <div className="text-center p-6 bg-white rounded-xl shadow border max-w-sm">
                                            <FileText size={40} className="text-zinc-400 mx-auto mb-2" />
                                            <p className="text-xs font-bold text-zinc-700">Định dạng file không hỗ trợ hiển thị nhanh</p>
                                            <a href={resolveFileUrl(selectedDoc.fileUrl)} target="_blank" rel="noreferrer" className="text-[11px] text-blue-600 underline block mt-2 font-mono">Tải file gốc từ Storage</a>
                                        </div>
                                    )}
                                </div>

                                {/* Khối thông tin và tải về tài liệu */}
                                <div className="w-full md:w-80 p-5 flex flex-col justify-between bg-white overflow-y-auto">
                                    <div className="space-y-4">
                                        <div>
                                            <h4 className="text-[11px] font-bold text-zinc-400 uppercase tracking-wider">Thuộc tính dữ liệu gốc</h4>
                                            <div className="mt-2 space-y-2 text-[11px]">
                                                <div className="flex justify-between"><span className="text-zinc-500">Mã Số Tài Liệu:</span> <span className="font-mono font-medium">#{selectedDoc.id}</span></div>
                                                <div className="flex justify-between"><span className="text-zinc-500">Định dạng Mime:</span> <span className="font-mono text-zinc-600">{selectedDoc.mimeType}</span></div>
                                                <div className="flex justify-between"><span className="text-zinc-500">Trạng thái Database:</span> <span className="font-bold text-orange-600">{selectedDoc.verifiedStatus}</span></div>
                                            </div>
                                        </div>

                                        <div className="border-t border-zinc-100 pt-3">
                                            <label className="text-[11px] font-bold text-zinc-500 uppercase tracking-wider block">Ghi chú tài liệu</label>
                                            <textarea
                                                rows={5}
                                                placeholder="Không có ghi chú"
                                                value={reviewNote}
                                                readOnly
                                                className="mt-1.5 w-full rounded-xl border border-black/10 p-3 text-xs outline-none focus:border-orange-600 disabled:bg-zinc-50 disabled:text-zinc-500 bg-zinc-50/50 resize-none transition"
                                            />
                                        </div>
                                    </div>

                                    {/* Cụm nút xem/tải file */}
                                    <div className="border-t border-zinc-100 pt-4 mt-6">
                                        <div className="space-y-2">
                                            <a
                                                href={resolveFileUrl(selectedDoc.fileUrl)}
                                                target="_blank"
                                                rel="noreferrer"
                                                className="flex h-9 items-center justify-center gap-1 rounded-xl bg-blue-600 text-xs font-bold text-white transition hover:bg-blue-700 shadow-sm"
                                            >
                                                Xem file
                                            </a>
                                            <a
                                                href={resolveFileUrl(selectedDoc.fileUrl)}
                                                download={selectedDoc.fileName ?? true}
                                                className="flex h-9 items-center justify-center gap-1 rounded-xl border border-black/10 text-xs font-bold text-zinc-600 hover:bg-zinc-50 transition"
                                            >
                                                Tải về
                                            </a>
                                            <button
                                                type="button"
                                                onClick={() => setSelectedDoc(null)}
                                                className="w-full h-9 rounded-xl border border-black/10 text-xs font-bold text-zinc-600 hover:bg-zinc-50 transition"
                                            >
                                                Đóng cửa sổ chi tiết
                                            </button>
                                        </div>
                                    </div>

                                </div>

                            </div>

                        </div>
                    </div>
                )}

            </div>
        </AdminShell>
    );
}