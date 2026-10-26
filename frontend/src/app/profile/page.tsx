"use client";

import { useEffect, useState } from "react";
import AdminShell from "../components/admin-shell";

type AdminUser = {
  id: number;
  type: string;
  username?: string | null;
  roles: string[];
  permissions?: string[];
};

type ProfileForm = {
  fullName: string;
  phone: string;
  department: string;
  position: string;
  note: string;
};

const defaultForm: ProfileForm = {
  fullName: "",
  phone: "",
  department: "",
  position: "",
  note: "",
};

export default function ProfilePage() {
  const [user, setUser] = useState<AdminUser | null>(null);
  const [form, setForm] = useState<ProfileForm>(defaultForm);
  const [saved, setSaved] = useState(false);

  useEffect(() => {
    const u = localStorage.getItem("adminUser");
    if (u) {
      try {
        setUser(JSON.parse(u));
      } catch {
        setUser(null);
      }
    }
  }, []);

  function updateField<K extends keyof ProfileForm>(key: K, value: ProfileForm[K]) {
    setForm((prev) => ({ ...prev, [key]: value }));
  }

  function onSave(e: React.FormEvent) {
    e.preventDefault();
    setSaved(true);
    setTimeout(() => setSaved(false), 2000);
  }

  return (
    <AdminShell
      title="Thong tin ca nhan"
      subtitle="Cap nhat ho so tai khoan admin"
    >
      <main className="mx-auto flex w-full max-w-5xl flex-col gap-6">
        <section className="grid gap-6 lg:grid-cols-[1.1fr_1.9fr]">
          <div className="rounded-2xl border border-black/5 bg-white p-6 shadow-sm">
            <div className="flex items-center gap-4">
              <div className="flex h-14 w-14 items-center justify-center rounded-2xl bg-blue-600 text-lg font-semibold text-white">
                {user?.username?.[0]?.toUpperCase() ?? "A"}
              </div>
              <div>
                <div className="text-sm text-zinc-500">Tài khoản</div>
                <div className="text-lg font-semibold">{user?.username ?? "admin@gmail.com"}</div>
                <div className="text-xs text-zinc-400">{user?.type ?? "ADMIN"}</div>
              </div>
            </div>
            <div className="mt-4 rounded-xl border border-black/5 bg-zinc-50 px-4 py-3 text-xs text-zinc-600">
              Vai trò: {(user?.roles ?? ["ADMIN"]).join(", ")}
            </div>
            <div className="mt-4 text-xs text-zinc-500">
              Thông tin này dùng để xác thực quyền truy cập nội bộ.
            </div>
          </div>

          <div className="rounded-2xl border border-black/5 bg-white p-6 shadow-sm">
            <h2 className="text-sm font-semibold">Hồ sơ cá nhân</h2>
            <p className="mt-1 text-xs text-zinc-500">
              Cập nhật thông tin liên hệ và chức danh trong hệ thống.
            </p>

            <form className="mt-6 grid gap-4 md:grid-cols-2" onSubmit={onSave}>
              <label className="flex flex-col gap-1 text-sm">
                Họ và tên
                <input
                  className="h-11 rounded-lg border border-black/10 bg-white px-3 outline-none transition focus:border-blue-400 focus:ring-2 focus:ring-blue-100"
                  placeholder="Trần Văn B"
                  value={form.fullName}
                  onChange={(e) => updateField("fullName", e.target.value)}
                />
              </label>

              <label className="flex flex-col gap-1 text-sm">
                Số điện thoại
                <input
                  className="h-11 rounded-lg border border-black/10 bg-white px-3 outline-none transition focus:border-blue-400 focus:ring-2 focus:ring-blue-100"
                  placeholder="090xxxxxxx"
                  value={form.phone}
                  onChange={(e) => updateField("phone", e.target.value)}
                />
              </label>

              <label className="flex flex-col gap-1 text-sm">
                Phòng ban
                <input
                  className="h-11 rounded-lg border border-black/10 bg-white px-3 outline-none transition focus:border-blue-400 focus:ring-2 focus:ring-blue-100"
                  placeholder="Vận hành hệ thống"
                  value={form.department}
                  onChange={(e) => updateField("department", e.target.value)}
                />
              </label>

              <label className="flex flex-col gap-1 text-sm">
                Chức danh
                <input
                  className="h-11 rounded-lg border border-black/10 bg-white px-3 outline-none transition focus:border-blue-400 focus:ring-2 focus:ring-blue-100"
                  placeholder="Admin tổng"
                  value={form.position}
                  onChange={(e) => updateField("position", e.target.value)}
                />
              </label>

              <label className="md:col-span-2 flex flex-col gap-1 text-sm">
                Ghi chú
                <textarea
                  className="min-h-[120px] rounded-lg border border-black/10 bg-white px-3 py-2 outline-none transition focus:border-blue-400 focus:ring-2 focus:ring-blue-100"
                  placeholder="Ghi chú nội bộ"
                  value={form.note}
                  onChange={(e) => updateField("note", e.target.value)}
                />
              </label>

              <div className="md:col-span-2 flex items-center gap-3">
                <button
                  className="h-11 rounded-lg bg-blue-600 px-4 text-sm font-semibold text-white shadow-sm transition hover:bg-blue-700"
                  type="submit"
                >
                  Lưu thông tin
                </button>
                {saved ? (
                  <span className="text-xs text-emerald-600">Đã lưu thông tin tạm thời.</span>
                ) : null}
              </div>
            </form>
          </div>
        </section>
      </main>
    </AdminShell>
  );
}
