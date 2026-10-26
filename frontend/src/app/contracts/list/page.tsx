"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import AdminShell from "../../components/admin-shell";
import { listGeneratedContracts, type GeneratedContract } from "../../../lib/api/admin-contracts";

const formatDateTime = (value?: string | null) => {
  if (!value) return "—";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return value;
  return new Intl.DateTimeFormat("vi-VN", { dateStyle: "short", timeStyle: "short" }).format(date);
};

const ownerTypeLabel = (value?: string | null) => {
  switch ((value ?? "").toLowerCase()) {
    case "loan_application":
      return "Vay vốn";
    case "saving":
      return "Tiết kiệm";
    default:
      return value ?? "—";
  }
};

export default function ContractDocumentsPage() {
  const [contracts, setContracts] = useState<GeneratedContract[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    let alive = true;
    setLoading(true);
    listGeneratedContracts()
      .then((data) => {
        if (!alive) return;
        setContracts(data);
      })
      .catch((err) => {
        if (!alive) return;
        setError(err instanceof Error ? err.message : "Không thể tải danh sách tài liệu");
      })
      .finally(() => {
        if (alive) setLoading(false);
      });

    return () => {
      alive = false;
    };
  }, []);

  return (
    <AdminShell
      title="Tài liệu đã sinh"
      subtitle="Danh sách hợp đồng và tài liệu được hệ thống tạo ra"
      actions={
        <Link
          href="/contracts"
          className="rounded bg-blue-600 px-3 py-2 text-xs font-semibold text-white"
        >
          Quản lý template
        </Link>
      }
    >
      {error ? (
        <div className="rounded-lg border border-rose-100 bg-rose-50 px-4 py-3 text-sm text-rose-700">
          {error}
        </div>
      ) : null}

      <div className="rounded-lg bg-white p-4 shadow-sm">
        <div className="mb-3 flex items-center justify-between">
          <h3 className="text-sm font-bold">Danh sách hợp đồng đã tạo</h3>
          <span className="text-xs text-zinc-400">{contracts.length} bản ghi</span>
        </div>

        {loading ? (
          <div className="text-sm text-zinc-500">Đang tải...</div>
        ) : contracts.length === 0 ? (
          <div className="text-sm text-zinc-500">Chưa có tài liệu hợp đồng nào.</div>
        ) : (
          <div className="space-y-3">
            {contracts.map((contract) => (
              <div key={contract.id} className="rounded-lg border border-zinc-100 p-4">
                <div className="flex flex-wrap items-center justify-between gap-3">
                  <div>
                    <div className="text-sm font-semibold text-zinc-900">
                      {contract.contractNumber ?? "Chưa có số"}
                    </div>
                    <div className="text-xs text-zinc-500">
                      {ownerTypeLabel(contract.ownerType)} · #{contract.ownerId}
                    </div>
                  </div>
                  <div className="text-xs text-zinc-500">{contract.status ?? "—"}</div>
                </div>
                <div className="mt-2 text-xs text-zinc-500">
                  Tạo lúc: {formatDateTime(contract.createdAt)}
                </div>
                <div className="mt-3">
                  {contract.fileUrl ? (
                    <a
                      className="inline-flex rounded bg-blue-600 px-3 py-1.5 text-xs font-medium text-white"
                      href={contract.fileUrl}
                      target="_blank"
                      rel="noreferrer"
                    >
                      Mở tài liệu
                    </a>
                  ) : (
                    <span className="text-xs text-zinc-400">Chưa có file</span>
                  )}
                </div>
              </div>
            ))}
          </div>
        )}
      </div>
    </AdminShell>
  );
}
