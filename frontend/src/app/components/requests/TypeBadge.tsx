import React from "react";

interface Props {
    type: string;
}

const typeMap: Record<string, { label: string; className: string }> = {
    limit_change: {
        label: "Nâng hạn mức",
        className: "bg-purple-100 text-purple-700 px-2 py-0.5 rounded text-xs font-medium",
    },
    profile_change: {
        label: "Đổi thông tin",
        className: "bg-blue-100 text-blue-700 px-2 py-0.5 rounded text-xs font-medium",
    },
    close_account: {
        label: "Tất toán số",
        className: "bg-orange-100 text-orange-700 px-2 py-0.5 rounded text-xs font-medium",
    },
    complaint: {
        label: "Khiếu nại GD",
        className: "bg-red-100 text-red-700 px-2 py-0.5 rounded text-xs font-medium",
    },
};

export function TypeBadge({ type }: Props) {
    const config = typeMap[type?.toLowerCase()] ?? {
        label: type,
        className: "bg-gray-100 text-gray-600 px-2 py-0.5 rounded text-xs font-medium",
    };
    return <span className={config.className}>{config.label}</span>;
}