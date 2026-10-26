"use client";

import Link from "next/link";
import { useEffect, useMemo, useState } from "react";
import AdminShell from "../components/admin-shell";

type StaffItem = {
  id: number;
  username: string;
  email: string;
  fullName: string;
  status: string;
  roles: string[];
  createdAt: string;
};

type StaffForm = {
  fullName: string;
  email: string;
  username: string;
  password: string;
  roles: string[];
};

type RoleOption = {
  code: string;
  label: string;
  desc: string;
};

const API_BASE = (process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8080").replace(/\/+$/, "");

const DEFAULT_ROLE_OPTIONS: RoleOption[] = [
  { code: "SUPER_ADMIN", label: "Super Admin", desc: "Toàn quyền hệ thống" },
  { code: "CUSTOMER_SUPPORT", label: "Nhân viên CSKH", desc: "Nhận và trả lời chat chuyển tiếp" },
  { code: "KYC_OFFICER", label: "Nhân viên KYC", desc: "Duyệt và từ chối hồ sơ KYC" },
  { code: "SERVICE_OFFICER", label: "Nhân viên Thủ tục", desc: "Yêu cầu dịch vụ, hạn mức, tiết kiệm" },
  { code: "LOAN_OFFICER", label: "Nhân viên Tín dụng", desc: "Thẩm định và duyệt vay vốn" },
];

const defaultForm: StaffForm = {
  fullName: "",
  email: "",
  username: "",
  password: "",
  roles: ["CUSTOMER_SUPPORT"],
};

export default function StaffPage() {
  const [token, setToken] = useState<string | null>(null);
  const [staff, setStaff] = useState<StaffItem[]>([]);
  const [query, setQuery] = useState("");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [modalOpen, setModalOpen] = useState(false);
  const [form, setForm] = useState<StaffForm>(defaultForm);
  const [saving, setSaving] = useState(false);
  const [roleModalOpen, setRoleModalOpen] = useState(false);
  const [selectedStaff, setSelectedStaff] = useState<StaffItem | null>(null);
  const [editRoles, setEditRoles] = useState<string[]>([]);
  const [updatingRoles, setUpdatingRoles] = useState(false);
  const [statusUpdatingId, setStatusUpdatingId] = useState<number | null>(null);
  const [roleOptions, setRoleOptions] = useState<RoleOption[]>(DEFAULT_ROLE_OPTIONS);

  useEffect(() => {
    setToken(localStorage.getItem("adminToken"));
  }, []);

  const authHeader = useMemo<HeadersInit>(() => {
    if (!token) return {} as HeadersInit;
    return { Authorization: `Bearer ${token}` } as HeadersInit;
  }, [token]);

  const roleLabel = useMemo(() => new Map(roleOptions.map((item) => [item.code, item.label])), [roleOptions]);

  async function fetchRoles() {
    if (!token) return;
    try {
      const res = await fetch(`${API_BASE}/api/admin/roles`, { headers: authHeader });
      if (!res.ok) throw new Error(await res.text());
      const data = (await res.json()) as Array<{ code: string; name?: string; description?: string }>;
      const nextRoles = data
        .filter((role) => role.code)
        .map((role) => ({
          code: role.code,
          label: role.name || role.code,
          desc: role.description || "Vai trò tùy chỉnh từ tab Quản lý vai trò",
        }));
      if (nextRoles.length > 0) setRoleOptions(nextRoles);
    } catch {
      setRoleOptions(DEFAULT_ROLE_OPTIONS);
    }
  }

  async function fetchStaff(search?: string) {
    if (!token) return;
    setLoading(true);
    setError(null);
    try {
      const qs = search ? `?q=${encodeURIComponent(search)}` : "";
      const res = await fetch(`${API_BASE}/api/admin/staff${qs}`, { headers: authHeader });
      if (!res.ok) throw new Error((await res.text()) || "Không thể tải danh sách nhân viên");
      setStaff((await res.json()) as StaffItem[]);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Không thể tải danh sách nhân viên");
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    if (token) {
      fetchRoles();
      fetchStaff();
    }
  }, [token]);

  function updateForm<K extends keyof StaffForm>(key: K, value: StaffForm[K]) {
    setForm((prev) => ({ ...prev, [key]: value }));
  }

  function toggleRole(code: string, target: "create" | "edit") {
    if (target === "create") {
      setForm((prev) => ({
        ...prev,
        roles: prev.roles.includes(code) ? prev.roles.filter((item) => item !== code) : [...prev.roles, code],
      }));
      return;
    }
    setEditRoles((prev) => (prev.includes(code) ? prev.filter((item) => item !== code) : [...prev, code]));
  }

  function openRoles(staffItem: StaffItem) {
    setSelectedStaff(staffItem);
    setEditRoles(staffItem.roles?.filter((role) => roleOptions.some((item) => item.code === role)) ?? []);
    setRoleModalOpen(true);
  }

  async function createStaff(e: React.FormEvent) {
    e.preventDefault();
    if (!token || form.roles.length === 0) return;
    setSaving(true);
    setError(null);
    try {
      const res = await fetch(`${API_BASE}/api/admin/staff`, {
        method: "POST",
        headers: { "Content-Type": "application/json", ...authHeader },
        body: JSON.stringify(form),
      });
      if (!res.ok) throw new Error((await res.text()) || "Không thể tạo nhân viên");
      await fetchStaff(query);
      setModalOpen(false);
      setForm(defaultForm);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Không thể tạo nhân viên");
    } finally {
      setSaving(false);
    }
  }

  async function saveRoles() {
    if (!token || !selectedStaff || editRoles.length === 0) return;
    setUpdatingRoles(true);
    setError(null);
    try {
      const res = await fetch(`${API_BASE}/api/admin/staff/${selectedStaff.id}/roles`, {
        method: "PUT",
        headers: { "Content-Type": "application/json", ...authHeader },
        body: JSON.stringify({ roles: editRoles }),
      });
      if (!res.ok) throw new Error((await res.text()) || "Không thể cập nhật vai trò");
      await fetchStaff(query);
      setRoleModalOpen(false);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Không thể cập nhật vai trò");
    } finally {
      setUpdatingRoles(false);
    }
  }

  async function toggleStatus(staffItem: StaffItem) {
    if (!token) return;
    const nextStatus = staffItem.status?.toLowerCase() === "active" ? "blocked" : "active";
    setStatusUpdatingId(staffItem.id);
    setError(null);
    try {
      const res = await fetch(`${API_BASE}/api/admin/staff/${staffItem.id}/status`, {
        method: "PUT",
        headers: { "Content-Type": "application/json", ...authHeader },
        body: JSON.stringify({ status: nextStatus }),
      });
      if (!res.ok) throw new Error((await res.text()) || "Không thể cập nhật trạng thái");
      await fetchStaff(query);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Không thể cập nhật trạng thái");
    } finally {
      setStatusUpdatingId(null);
    }
  }

  if (!token) {
    return (
      <div className="flex min-h-screen items-center justify-center bg-[#f6f7fb] px-6 text-[#111827]">
        <div className="w-full max-w-md rounded-2xl border border-black/5 bg-white p-6">
          <h1 className="text-xl font-semibold">Quản lý nhân viên</h1>
          <p className="mt-2 text-sm text-zinc-500">Vui lòng đăng nhập để tiếp tục.</p>
          <Link className="mt-4 inline-flex h-10 items-center justify-center rounded-lg bg-blue-600 px-4 text-sm font-semibold text-white" href="/login">
            Đi tới đăng nhập
          </Link>
        </div>
      </div>
    );
  }

  return (
    <AdminShell
      title="Quản lý nhân viên"
      subtitle="Thêm nhân viên, khóa/mở khóa tài khoản và gán vai trò RBAC"
      actions={
        <button className="h-10 rounded-lg bg-blue-600 px-4 text-sm font-semibold text-white shadow-sm transition hover:bg-blue-700" onClick={() => setModalOpen(true)} type="button">
          Thêm nhân viên
        </button>
      }
    >
      <section className="rounded-2xl border border-black/5 bg-white p-6 shadow-sm">
        <div className="flex flex-wrap items-center justify-between gap-3">
          <div>
            <div className="text-sm font-semibold">Danh sách nhân viên</div>
            <div className="text-xs text-zinc-500">Tab này chỉ gán vai trò; quyền chi tiết nằm ở Quản trị hệ thống / Vai trò.</div>
          </div>
          <div className="flex items-center gap-2">
            <input className="h-10 w-64 rounded-lg border border-black/10 px-3 text-sm outline-none" placeholder="Tìm theo tên, email..." value={query} onChange={(e) => setQuery(e.target.value)} />
            <button className="h-10 rounded-lg border border-black/10 px-3 text-sm" onClick={() => fetchStaff(query)} type="button">
              Tìm
            </button>
          </div>
        </div>

        {error ? <div className="mt-4 rounded-lg border border-red-100 bg-red-50 p-3 text-sm text-red-700">{error}</div> : null}

        <div className="mt-4 overflow-hidden rounded-xl border border-black/5">
          <table className="w-full text-left text-sm">
            <thead className="bg-zinc-50 text-xs text-zinc-500">
              <tr>
                <th className="px-4 py-3">ID</th>
                <th className="px-4 py-3">Họ tên</th>
                <th className="px-4 py-3">Email</th>
                <th className="px-4 py-3">Vai trò</th>
                <th className="px-4 py-3">Trạng thái</th>
                <th className="px-4 py-3">Ngày tạo</th>
                <th className="px-4 py-3">Hành động</th>
              </tr>
            </thead>
            <tbody>
              {loading ? (
                <tr><td className="px-4 py-6 text-center text-zinc-400" colSpan={7}>Đang tải...</td></tr>
              ) : staff.length === 0 ? (
                <tr><td className="px-4 py-6 text-center text-zinc-400" colSpan={7}>Chưa có tài khoản nào.</td></tr>
              ) : (
                staff.map((item) => (
                  <tr key={item.id} className="border-t border-black/5 hover:bg-zinc-50/70">
                    <td className="px-4 py-3 text-zinc-500">#{item.id}</td>
                    <td className="px-4 py-3">
                      <div className="font-medium">{item.fullName}</div>
                      <div className="text-xs text-zinc-400">{item.username}</div>
                    </td>
                    <td className="px-4 py-3 text-zinc-600">{item.email}</td>
                    <td className="px-4 py-3">
                      <div className="flex flex-wrap gap-2">
                        {item.roles?.length ? item.roles.map((role) => (
                          <span key={role} className="rounded-full bg-blue-50 px-2.5 py-1 text-xs text-blue-700">
                            {roleLabel.get(role) ?? role}
                          </span>
                        )) : <span className="text-xs text-zinc-400">Không có</span>}
                      </div>
                    </td>
                    <td className="px-4 py-3">
                      <span className={`rounded-full px-2 py-1 text-xs ${item.status?.toLowerCase() === "active" ? "bg-emerald-50 text-emerald-600" : "bg-rose-50 text-rose-600"}`}>
                        {item.status?.toLowerCase() === "active" ? "Hoạt động" : "Đã khóa"}
                      </span>
                    </td>
                    <td className="px-4 py-3 text-zinc-500">{item.createdAt ? new Date(item.createdAt).toLocaleDateString("vi-VN") : "-"}</td>
                    <td className="px-4 py-3">
                      <div className="flex flex-wrap items-center gap-3">
                        <button className="text-sm font-medium text-blue-600 hover:text-blue-700" type="button" onClick={() => openRoles(item)}>
                          Gán vai trò
                        </button>
                        <button className="text-sm font-medium text-zinc-600 hover:text-zinc-800 disabled:opacity-50" type="button" onClick={() => toggleStatus(item)} disabled={statusUpdatingId === item.id}>
                          {statusUpdatingId === item.id ? "Đang cập nhật..." : item.status?.toLowerCase() === "active" ? "Khóa" : "Mở khóa"}
                        </button>
                      </div>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </section>

      {modalOpen ? (
        <StaffModal
          title="Thêm nhân viên mới"
          roles={form.roles}
          roleOptions={roleOptions}
          saving={saving}
          onClose={() => setModalOpen(false)}
          onSubmit={createStaff}
          onToggleRole={(code) => toggleRole(code, "create")}
        >
          <div className="grid gap-4 md:grid-cols-2">
            <label className="text-sm">Họ tên<input className="mt-1 h-10 w-full rounded-lg border border-black/10 px-3 outline-none" value={form.fullName} onChange={(e) => updateForm("fullName", e.target.value)} required /></label>
            <label className="text-sm">Email<input className="mt-1 h-10 w-full rounded-lg border border-black/10 px-3 outline-none" type="email" value={form.email} onChange={(e) => updateForm("email", e.target.value)} required /></label>
            <label className="text-sm">Tên đăng nhập<input className="mt-1 h-10 w-full rounded-lg border border-black/10 px-3 outline-none" value={form.username} onChange={(e) => updateForm("username", e.target.value)} required /></label>
            <label className="text-sm">Mật khẩu<input className="mt-1 h-10 w-full rounded-lg border border-black/10 px-3 outline-none" type="password" value={form.password} onChange={(e) => updateForm("password", e.target.value)} required /></label>
          </div>
        </StaffModal>
      ) : null}

      {roleModalOpen && selectedStaff ? (
        <StaffModal
          title={`Gán vai trò cho ${selectedStaff.fullName}`}
          roles={editRoles}
          roleOptions={roleOptions}
          saving={updatingRoles}
          onClose={() => setRoleModalOpen(false)}
          onSubmit={(e) => {
            e.preventDefault();
            saveRoles();
          }}
          onToggleRole={(code) => toggleRole(code, "edit")}
        />
      ) : null}
    </AdminShell>
  );
}

function StaffModal({
  title,
  roles,
  roleOptions,
  saving,
  children,
  onClose,
  onSubmit,
  onToggleRole,
}: {
  title: string;
  roles: string[];
  roleOptions: RoleOption[];
  saving: boolean;
  children?: React.ReactNode;
  onClose: () => void;
  onSubmit: (e: React.FormEvent) => void;
  onToggleRole: (code: string) => void;
}) {
  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40 px-4">
      <form className="w-full max-w-2xl rounded-2xl bg-white p-6 shadow-xl" onSubmit={onSubmit}>
        <div className="flex items-start justify-between">
          <div>
            <div className="text-lg font-semibold">{title}</div>
            <div className="text-xs text-zinc-500">Chọn một hoặc nhiều vai trò nghiệp vụ.</div>
          </div>
          <button className="text-zinc-400" type="button" onClick={onClose}>x</button>
        </div>

        <div className="mt-5 space-y-5">
          {children}
          <div>
            <div className="text-sm font-medium">Vai trò</div>
            <div className="mt-2 grid gap-2 md:grid-cols-2">
              {roleOptions.map((role) => (
                <label key={role.code} className="flex cursor-pointer items-start gap-3 rounded-xl border border-zinc-200 p-3 text-sm hover:bg-zinc-50">
                  <input className="mt-1" type="checkbox" checked={roles.includes(role.code)} onChange={() => onToggleRole(role.code)} />
                  <span>
                    <span className="block font-semibold text-zinc-900">{role.label}</span>
                    <span className="block font-mono text-[11px] text-zinc-500">{role.code}</span>
                    <span className="mt-1 block text-xs text-zinc-500">{role.desc}</span>
                  </span>
                </label>
              ))}
            </div>
            {roles.length === 0 ? <div className="mt-2 text-xs text-red-600">Cần chọn ít nhất một vai trò.</div> : null}
          </div>
        </div>

        <div className="mt-5 flex items-center justify-end gap-2">
          <button className="h-10 rounded-lg border border-black/10 px-4 text-sm" type="button" onClick={onClose}>Hủy</button>
          <button className="h-10 rounded-lg bg-blue-600 px-4 text-sm font-semibold text-white shadow-sm disabled:opacity-60" type="submit" disabled={saving || roles.length === 0}>
            {saving ? "Đang lưu..." : "Lưu vai trò"}
          </button>
        </div>
      </form>
    </div>
  );
}
