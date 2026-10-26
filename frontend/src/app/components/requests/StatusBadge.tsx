import React from "react";

interface Props {
    status: string;
}

const statusMap: Record<string, { label: string; className: string }> = {
    submitted: {
        label: "Chờ xử lý",
        className: "bg-yellow-100 text-yellow-700 border border-yellow-300 px-3 py-1 rounded-full text-xs font-medium",
    },
    processing: {
        label: "Đang xử lý",
        className: "bg-blue-100 text-blue-700 border border-blue-300 px-3 py-1 rounded-full text-xs font-medium",
    },
    approved: {
        label: "Đã duyệt",
        className: "bg-green-100 text-green-700 border border-green-300 px-3 py-1 rounded-full text-xs font-medium",
    },
    rejected: {
        label: "Từ chối",
        className: "bg-red-100 text-red-700 border border-red-300 px-3 py-1 rounded-full text-xs font-medium",
    },
};

export function StatusBadge({ status }: Props) {
    const config = statusMap[status?.toLowerCase()] ?? {
        label: status,
        className: "bg-gray-100 text-gray-600 border border-gray-300 px-3 py-1 rounded-full text-xs font-medium",
    };
    return <span className={config.className}>{config.label}</span>;
}