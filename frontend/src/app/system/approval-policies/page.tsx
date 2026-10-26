"use client";

import { useEffect, useMemo, useState } from "react";
import { Edit2, Plus, Trash2 } from "lucide-react";

import AdminShell from "../../components/admin-shell";
import {
  createApprovalPolicy,
  deleteApprovalPolicy,
  listApprovalPolicies,
  updateApprovalPolicy,
  type ApprovalPolicy,
  type ApprovalPolicyPayload,
} from "../../../lib/api/approval-policies";

const emptyForm: ApprovalPolicyPayload = {
  serviceType: "saving",
  minAmount: "0",
  maxAmount: "",
  staffApprovalsRequired: 1,
  managerApprovalsRequired: 0,
  active: true,
  description: "",
};

function formatCurrency(value: string | number | null | undefined) {
  if (value == null || value === "") return "Không giới hạn";
  const n = typeof value === "number" ? value : Number(value);
  if (Number.isNaN(n)) return String(value);
  return new Intl.NumberFormat("vi-VN", { style: "currency", currency: "VND" }).format(n);
}

function serviceLabel(serviceType: string) {
  if (serviceType === "loan_application") return "Duyệt vay vốn";
  if (serviceType === "saving") return "Duyệt sổ tiết kiệm";
  return serviceType;
}

function toPayload(form: ApprovalPolicyPayload): ApprovalPolicyPayload {
  return {
    ...form,
    minAmount: form.minAmount || "0",
    maxAmount: form.maxAmount || null,
    staffApprovalsRequired: Number(form.staffApprovalsRequired),
    managerApprovalsRequired: Number(form.managerApprovalsRequired),
  };
}

export default function ApprovalPoliciesPage() {
  const [items, setItems] = useState<ApprovalPolicy[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [editing, setEditing] = useState<ApprovalPolicy | null>(null);
  const [form, setForm] = useState<ApprovalPolicyPayload>(emptyForm);
  const [toast, setToast] = useState("");

  const grouped = useMemo(() => {
    return items.reduce<Record<string, ApprovalPolicy[]>>((acc, item) => {
      acc[item.serviceType] = [...(acc[item.serviceType] ?? []), item];
      return acc;
    }, {});
  }, [items]);

  async function load() {
    setLoading(true);
    setError("");
    try {
      setItems(await listApprovalPolicies());
    } catch (err) {
      setError(err instanceof Error ? err.message : "Không thể tải cấu hình");
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    load();
  }, []);

  function edit(item: ApprovalPolicy) {
    setEditing(item);
    setForm({
      serviceType: item.serviceType,
      minAmount: item.minAmount,
      maxAmount: item.maxAmount ?? "",
      staffApprovalsRequired: item.staffApprovalsRequired,
      managerApprovalsRequired: item.managerApprovalsRequired,
      active: item.active,
      description: item.description ?? "",
    });
  }

  function reset() {
    setEditing(null);
    setForm(emptyForm);
  }

  async function submit() {
    try {
      if (editing) {
        await updateApprovalPolicy(editing.id, toPayload(form));
        setToast("Đã cập nhật mức duyệt");
      } else {
        await createApprovalPolicy(toPayload(form));
        setToast("Đã tạo mức duyệt");
      }
      reset();
      await load();
    } catch (err) {
      setToast(err instanceof Error ? err.message : "Không thể lưu cấu hình");
    }
  }

  async function remove(id: number) {
    if (!window.confirm("Xóa mức duyệt này?")) return;
    try {
      await deleteApprovalPolicy(id);
      setToast("Đã xóa mức duyệt");
      await load();
    } catch (err) {
      setToast(err instanceof Error ? err.message : "Không thể xóa cấu hình");
    }
  }

  return (
    <AdminShell title="Mức duyệt nghiệp vụ" subtitle="Quản lý mức tiền vay vốn / tiết kiệm và số người cần duyệt">
      {toast ? (
        <div className="fixed right-6 top-6 z-50 rounded-xl border bg-white px-4 py-3 text-sm shadow-lg">{toast}</div>
      ) : null}

      <div className="grid gap-6 xl:grid-cols-[minmax(0,1fr)_420px]">
        <section className="rounded-2xl border border-neutral-200 bg-white">
          <div className="border-b border-neutral-200 px-6 py-4">
            <h2 className="text-base font-semibold text-neutral-900">Danh sách mức duyệt</h2>
            <p className="text-sm text-neutral-500">Hệ thống chọn rule active có khoảng tiền khớp với hồ sơ.</p>
          </div>

          <div className="space-y-6 p-6">
            {loading ? <div className="text-sm text-neutral-500">Đang tải...</div> : null}
            {error ? <div className="rounded-xl border border-red-200 bg-red-50 p-3 text-sm text-red-700">{error}</div> : null}
            {!loading && !error && Object.entries(grouped).map(([serviceType, policies]) => (
              <div key={serviceType}>
                <h3 className="mb-3 text-sm font-semibold text-neutral-700">{serviceLabel(serviceType)}</h3>
                <div className="overflow-hidden rounded-xl border border-neutral-200">
                  <table className="w-full text-sm">
                    <thead className="bg-neutral-50 text-left text-xs text-neutral-500">
                      <tr>
                        <th className="px-4 py-3">Khoảng tiền</th>
                        <th className="px-4 py-3">Nhân viên nghiệp vụ</th>
                        <th className="px-4 py-3">Quản lý</th>
                        <th className="px-4 py-3">Trạng thái</th>
                        <th className="px-4 py-3 text-right">Thao tác</th>
                      </tr>
                    </thead>
                    <tbody>
                      {policies.map((item) => (
                        <tr key={item.id} className="border-t border-neutral-100">
                          <td className="px-4 py-3">
                            <div className="font-medium text-neutral-900">{formatCurrency(item.minAmount)}</div>
                            <div className="text-xs text-neutral-500">đến {formatCurrency(item.maxAmount)}</div>
                            {item.description ? <div className="mt-1 text-xs text-neutral-400">{item.description}</div> : null}
                          </td>
                          <td className="px-4 py-3">{item.staffApprovalsRequired}</td>
                          <td className="px-4 py-3">{item.managerApprovalsRequired}</td>
                          <td className="px-4 py-3">
                            <span className={`rounded-full px-2 py-1 text-xs font-medium ${item.active ? "bg-green-100 text-green-700" : "bg-neutral-100 text-neutral-500"}`}>
                              {item.active ? "Đang áp dụng" : "Tắt"}
                            </span>
                          </td>
                          <td className="px-4 py-3">
                            <div className="flex justify-end gap-2">
                              <button onClick={() => edit(item)} className="rounded-lg border border-neutral-200 p-2 text-blue-600 hover:bg-blue-50">
                                <Edit2 size={15} />
                              </button>
                              <button onClick={() => remove(item.id)} className="rounded-lg border border-neutral-200 p-2 text-red-600 hover:bg-red-50">
                                <Trash2 size={15} />
                              </button>
                            </div>
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              </div>
            ))}
          </div>
        </section>

        <section className="rounded-2xl border border-neutral-200 bg-white p-6">
          <div className="mb-5 flex items-center justify-between">
            <div>
              <h2 className="text-base font-semibold text-neutral-900">{editing ? "Sửa mức duyệt" : "Tạo mức duyệt"}</h2>
              <p className="text-sm text-neutral-500">Ví dụ: từ 100 triệu cần 2 nhân viên nghiệp vụ và 1 quản lý.</p>
            </div>
            <button onClick={reset} className="rounded-xl border border-neutral-200 px-3 py-2 text-sm hover:bg-neutral-50">
              Mới
            </button>
          </div>

          <div className="space-y-4">
            <label className="block text-sm">
              <span className="mb-1 block text-neutral-600">Loại nghiệp vụ</span>
              <select
                className="w-full rounded-xl border border-neutral-200 px-3 py-2 outline-none focus:ring-2 focus:ring-blue-500"
                value={form.serviceType}
                onChange={(e) => setForm((prev) => ({ ...prev, serviceType: e.target.value }))}
              >
                <option value="saving">Duyệt sổ tiết kiệm</option>
                <option value="loan_application">Duyệt vay vốn</option>
              </select>
            </label>

            <div className="grid grid-cols-2 gap-3">
              <label className="block text-sm">
                <span className="mb-1 block text-neutral-600">Từ số tiền</span>
                <input
                  className="w-full rounded-xl border border-neutral-200 px-3 py-2 outline-none focus:ring-2 focus:ring-blue-500"
                  value={form.minAmount}
                  onChange={(e) => setForm((prev) => ({ ...prev, minAmount: e.target.value }))}
                  type="number"
                  min={0}
                />
              </label>
              <label className="block text-sm">
                <span className="mb-1 block text-neutral-600">Đến số tiền</span>
                <input
                  className="w-full rounded-xl border border-neutral-200 px-3 py-2 outline-none focus:ring-2 focus:ring-blue-500"
                  value={form.maxAmount ?? ""}
                  onChange={(e) => setForm((prev) => ({ ...prev, maxAmount: e.target.value }))}
                  type="number"
                  min={0}
                  placeholder="Trống = không giới hạn"
                />
              </label>
            </div>

            <div className="grid grid-cols-2 gap-3">
              <label className="block text-sm">
                <span className="mb-1 block text-neutral-600">Số nhân viên nghiệp vụ duyệt</span>
                <input
                  className="w-full rounded-xl border border-neutral-200 px-3 py-2 outline-none focus:ring-2 focus:ring-blue-500"
                  value={form.staffApprovalsRequired}
                  onChange={(e) => setForm((prev) => ({ ...prev, staffApprovalsRequired: Number(e.target.value) }))}
                  type="number"
                  min={0}
                />
              </label>
              <label className="block text-sm">
                <span className="mb-1 block text-neutral-600">Số quản lý duyệt</span>
                <input
                  className="w-full rounded-xl border border-neutral-200 px-3 py-2 outline-none focus:ring-2 focus:ring-blue-500"
                  value={form.managerApprovalsRequired}
                  onChange={(e) => setForm((prev) => ({ ...prev, managerApprovalsRequired: Number(e.target.value) }))}
                  type="number"
                  min={0}
                />
              </label>
            </div>

            <label className="block text-sm">
              <span className="mb-1 block text-neutral-600">Ghi chú</span>
              <textarea
                className="min-h-24 w-full rounded-xl border border-neutral-200 px-3 py-2 outline-none focus:ring-2 focus:ring-blue-500"
                value={form.description}
                onChange={(e) => setForm((prev) => ({ ...prev, description: e.target.value }))}
              />
            </label>

            <label className="flex items-center gap-2 text-sm text-neutral-700">
              <input
                type="checkbox"
                checked={form.active}
                onChange={(e) => setForm((prev) => ({ ...prev, active: e.target.checked }))}
              />
              Đang áp dụng
            </label>

            <button onClick={submit} className="inline-flex items-center gap-2 rounded-xl bg-blue-600 px-4 py-2 text-sm font-medium text-white hover:bg-blue-700">
              <Plus size={16} /> {editing ? "Cập nhật" : "Tạo mức duyệt"}
            </button>
          </div>
        </section>
      </div>
    </AdminShell>
  );
}
