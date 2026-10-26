"use client";

import {
    Search,
    Activity,
    Shield,
    AlertTriangle,
} from "lucide-react";

import { useEffect, useMemo, useState } from "react";
import AdminShell from "../../components/admin-shell";

const API_BASE = (
    process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8080"
).replace(/\/+$/, "");

type AuditType = "Giao dịch" | "Bảo mật" | "Cảnh báo";

type AuditLog = {
    id: number;
    createdAt: string;
    type: AuditType;
    actor: string;
    action: string;
    ip: string;
    detail: string;
};

export default function AuditPage() {

    const [auditLogs, setAuditLogs] = useState<AuditLog[]>([]);
    const [loading, setLoading] = useState(false);

    const [search, setSearch] = useState("");
    const [filterType, setFilterType] = useState("Tất cả");
    const [selectedDate, setSelectedDate] = useState("");
    const [currentPage, setCurrentPage] = useState(1);

    const itemsPerPage = 7;

    useEffect(() => {
        fetchLogs();
    }, []);

    async function fetchLogs() {

        try {

            setLoading(true);
            const token =
                localStorage.getItem("adminToken") ||
                localStorage.getItem("token");

            console.log("TOKEN =", token);
            if (!token) {
                console.log("Chưa đăng nhập admin");
                setAuditLogs([]);
                return;
            }

            const res = await fetch(`${API_BASE}/api/admin/system/logs`, {
                method: "GET",
                headers: {
                    Authorization: `Bearer ${token}`,
                    "Content-Type": "application/json",
                },
            });

            if (!res.ok) {

                console.log("API lỗi:", res.status);

                setAuditLogs([]);
                return;
            }

            const data = await res.json();

            console.log("AUDIT RESPONSE:", data);

            const logs = Array.isArray(data)
                ? data
                : data.items || [];

            const mappedData: AuditLog[] = logs.map((item: any) => ({

                id: item.id ?? 0,

                createdAt: item.createdAt ?? "",

                type: (
                    item.type === "SECURITY"
                        ? "Bảo mật"
                        : item.type === "WARNING"
                            ? "Cảnh báo"
                            : "Giao dịch"
                ) as AuditType,

                actor: item.actorType || "Hệ thống",

                action: item.action || "-",

                ip: item.ipAddress || "-",

                detail: (() => {
                    try {
                        const meta = JSON.parse(item.metadataJson || "{}");

                        return (
                            meta.path ||
                            meta.method ||
                            item.metadataJson ||
                            "-"
                        );
                    } catch {
                        return item.metadataJson || "-";
                    }
                })(),
            }));

            setAuditLogs(mappedData);

        } catch (err) {

            console.log("Fetch logs error:", err);

            setAuditLogs([]);

        } finally {

            setLoading(false);
        }
    }

    const filteredLogs = useMemo(() => {

        return auditLogs.filter((log) => {

            const matchSearch =
                log.actor?.toLowerCase().includes(search.toLowerCase()) ||
                log.action?.toLowerCase().includes(search.toLowerCase());

            const matchType =
                filterType === "Tất cả" || log.type === filterType;

            const matchDate =
                !selectedDate ||
                log.createdAt?.startsWith(selectedDate);

            return matchSearch && matchType && matchDate;
        });

    }, [auditLogs, search, filterType, selectedDate]);

    const totalPages = Math.ceil(filteredLogs.length / itemsPerPage);

    const paginatedLogs = filteredLogs.slice(
        (currentPage - 1) * itemsPerPage,
        currentPage * itemsPerPage
    );

    const getTypeStyle = (type: AuditType) => {

        switch (type) {

            case "Giao dịch":
                return "bg-blue-50 text-blue-600";

            case "Bảo mật":
                return "bg-green-50 text-green-600";

            case "Cảnh báo":
                return "bg-orange-50 text-orange-500";

            default:
                return "bg-gray-50 text-gray-600";
        }
    };

    const stats = {

        giaoDich: auditLogs.filter(
            (x) => x.type === "Giao dịch"
        ).length,

        baoMat: auditLogs.filter(
            (x) => x.type === "Bảo mật"
        ).length,

        canhBao: auditLogs.filter(
            (x) => x.type === "Cảnh báo"
        ).length,
    };

    return (

        <AdminShell
            title="Nhat ky he thong"
            subtitle="Audit trail - Theo doi moi hoat dong trong he thong"
        >

            <div className="mx-auto max-w-7xl">

                <div className="mb-6 rounded-2xl border border-gray-200 bg-white p-5">

                    <div className="grid grid-cols-1 gap-5 md:grid-cols-3">

                        <div>

                            <label className="mb-2 block text-[14px] font-semibold text-gray-700">
                                Tìm kiếm
                            </label>

                            <div className="flex items-center rounded-2xl border border-gray-300 px-4 py-3">

                                <Search
                                    size={18}
                                    className="text-gray-400"
                                />

                                <input
                                    type="text"
                                    placeholder="Tìm theo nhân viên hoặc hành động..."
                                    value={search}
                                    onChange={(e) => {
                                        setSearch(e.target.value);
                                        setCurrentPage(1);
                                    }}
                                    className="ml-3 w-full text-[14px] text-gray-700 placeholder:text-gray-400 outline-none"
                                />

                            </div>

                        </div>

                        <div>

                            <label className="mb-2 block text-[14px] font-semibold text-gray-700">
                                Loại hành động
                            </label>

                            <select
                                value={filterType}
                                onChange={(e) => {
                                    setFilterType(e.target.value);
                                    setCurrentPage(1);
                                }}
                                className="w-full rounded-2xl border border-gray-300 px-4 py-3 text-[14px] text-gray-700 outline-none"
                            >

                                <option>Tất cả</option>
                                <option>Giao dịch</option>
                                <option>Bảo mật</option>
                                <option>Cảnh báo</option>

                            </select>

                        </div>

                        <div>

                            <label className="mb-2 block text-[14px] font-semibold text-gray-700">
                                Ngày
                            </label>

                            <input
                                type="date"
                                value={selectedDate}
                                onChange={(e) => {
                                    setSelectedDate(e.target.value);
                                    setCurrentPage(1);
                                }}
                                className="w-full rounded-2xl border border-gray-300 px-4 py-3 text-[14px] text-gray-700 outline-none"
                            />

                        </div>

                    </div>

                </div>

                <div className="mb-6 grid grid-cols-1 gap-5 md:grid-cols-3">

                    <div className="rounded-2xl border border-gray-200 bg-white p-5">

                        <div className="flex items-center gap-4">

                            <div className="rounded-2xl bg-blue-50 p-3">

                                <Activity
                                    size={20}
                                    className="text-blue-600"
                                />

                            </div>

                            <div>

                                <p className="text-[14px] text-gray-600">
                                    Giao dịch
                                </p>

                                <h3 className="text-[26px] font-bold text-black">
                                    {stats.giaoDich}
                                </h3>

                            </div>

                        </div>

                    </div>

                    <div className="rounded-2xl border border-gray-200 bg-white p-5">

                        <div className="flex items-center gap-4">

                            <div className="rounded-2xl bg-green-50 p-3">

                                <Shield
                                    size={20}
                                    className="text-green-600"
                                />

                            </div>

                            <div>

                                <p className="text-[14px] text-gray-600">
                                    Bảo mật
                                </p>

                                <h3 className="text-[26px] font-bold text-black">
                                    {stats.baoMat}
                                </h3>

                            </div>

                        </div>

                    </div>

                    <div className="rounded-2xl border border-gray-200 bg-white p-5">

                        <div className="flex items-center gap-4">

                            <div className="rounded-2xl bg-orange-50 p-3">

                                <AlertTriangle
                                    size={20}
                                    className="text-orange-500"
                                />

                            </div>

                            <div>

                                <p className="text-[14px] text-gray-600">
                                    Cảnh báo
                                </p>

                                <h3 className="text-[26px] font-bold text-black">
                                    {stats.canhBao}
                                </h3>

                            </div>

                        </div>

                    </div>

                </div>

                <div className="overflow-hidden rounded-2xl border border-gray-200 bg-white">

                    <div className="overflow-x-auto">

                        <table className="w-full min-w-[1400px] border-collapse">

                            <thead>

                                <tr className="border-b border-gray-200 bg-white">

                                    <th className="px-6 py-4 text-left text-[14px] font-semibold text-gray-700">
                                        Thời gian
                                    </th>

                                    <th className="px-6 py-4 text-left text-[14px] font-semibold text-gray-700">
                                        Loại
                                    </th>

                                    <th className="px-6 py-4 text-left text-[14px] font-semibold text-gray-700">
                                        Tác nhân
                                    </th>

                                    <th className="px-6 py-4 text-left text-[14px] font-semibold text-gray-700">
                                        Hành động
                                    </th>

                                    <th className="px-6 py-4 text-left text-[14px] font-semibold text-gray-700">
                                        IP
                                    </th>

                                    <th className="px-6 py-4 text-left text-[14px] font-semibold text-gray-700">
                                        Chi tiết
                                    </th>

                                </tr>

                            </thead>

                            <tbody>

                                {loading ? (

                                    <tr>

                                        <td
                                            colSpan={6}
                                            className="px-6 py-10 text-center text-gray-500"
                                        >
                                            Đang tải dữ liệu...
                                        </td>

                                    </tr>

                                ) : paginatedLogs.length === 0 ? (

                                    <tr>

                                        <td
                                            colSpan={6}
                                            className="px-6 py-10 text-center text-gray-500"
                                        >
                                            Không có dữ liệu
                                        </td>

                                    </tr>

                                ) : (

                                    paginatedLogs.map((log) => (

                                        <tr
                                            key={log.id}
                                            className="border-b border-gray-200 align-top hover:bg-gray-50/40"
                                        >

                                            <td className="whitespace-nowrap px-6 py-5 text-[14px] font-medium text-gray-800">

                                                {log.createdAt
                                                    ? new Date(log.createdAt).toLocaleString()
                                                    : "-"}

                                            </td>

                                            <td className="px-6 py-5">

                                                <span
                                                    className={`inline-flex rounded-lg px-3 py-2 text-[13px] font-semibold ${getTypeStyle(
                                                        log.type
                                                    )}`}
                                                >
                                                    {log.type}
                                                </span>

                                            </td>

                                            <td className="px-6 py-5 text-[14px] font-semibold leading-7 text-gray-900">
                                                {log.actor}
                                            </td>

                                            <td className="max-w-[280px] px-6 py-5 text-[14px] font-medium leading-7 text-gray-900">
                                                {log.action}
                                            </td>

                                            <td className="px-6 py-5">

                                                <span className="inline-flex rounded-md bg-gray-100 px-3 py-1.5 font-mono text-[13px] text-gray-700">
                                                    {log.ip}
                                                </span>

                                            </td>

                                            <td className="max-w-[340px] px-6 py-5">

                                                <p className="truncate text-[14px] font-normal leading-7 text-gray-600">
                                                    {log.detail}
                                                </p>

                                            </td>

                                        </tr>

                                    ))

                                )}

                            </tbody>

                        </table>

                    </div>

                </div>

            </div>

        </AdminShell>
    );
}