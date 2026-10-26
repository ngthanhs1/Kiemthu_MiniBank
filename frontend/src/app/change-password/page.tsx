"use client";

import { useState } from "react";
import AdminShell from "../components/admin-shell";

type PasswordForm = {
  currentPassword: string;
  newPassword: string;
  confirmPassword: string;
};

const defaultForm: PasswordForm = {
  currentPassword: "",
  newPassword: "",
  confirmPassword: "",
};

export default function ChangePasswordPage() {
  const [form, setForm] = useState<PasswordForm>(defaultForm);
  const [message, setMessage] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);

  function updateField<K extends keyof PasswordForm>(key: K, value: PasswordForm[K]) {
    setForm((prev) => ({ ...prev, [key]: value }));
  }

  function onSubmit(e: React.FormEvent) {
    e.preventDefault();
    setMessage(null);
    setError(null);

    if (!form.currentPassword || !form.newPassword || !form.confirmPassword) {
      setError("Vui long nhap day du thong tin.");
      return;
    }

    if (form.newPassword.length < 6) {
      setError("Mat khau moi toi thieu 6 ky tu.");
      return;
    }

    if (form.newPassword !== form.confirmPassword) {
      setError("Mat khau xac nhan khong trung khop.");
      return;
    }

    setMessage("Da cap nhat mat khau tam thoi. Can ket noi backend de luu chinh thuc.");
    setForm(defaultForm);
  }

  return (
    <AdminShell
      title="Doi mat khau"
      subtitle="Cap nhat mat khau dang nhap"
    >
      <main className="mx-auto flex w-full max-w-4xl flex-col gap-6">
        <section className="grid gap-6 lg:grid-cols-[1fr_1.2fr]">
          <div className="rounded-2xl border border-black/5 bg-white p-6 shadow-sm">
            <h2 className="text-sm font-semibold">Lưu ý bảo mật</h2>
            <ul className="mt-3 space-y-2 text-xs text-zinc-500">
              <li>Mat khau moi toi thieu 6 ky tu.</li>
              <li>Khong su dung mat khau trung voi mat khau cu.</li>
              <li>Khong chia se thong tin dang nhap ra ben ngoai.</li>
            </ul>
            <div className="mt-4 rounded-xl border border-blue-100 bg-blue-50 p-3 text-xs text-blue-700">
              Doi mat khau dinh ky de tang cuong bao mat tai khoan.
            </div>
          </div>

          <div className="rounded-2xl border border-black/5 bg-white p-6 shadow-sm">
            <form className="flex flex-col gap-4" onSubmit={onSubmit}>
              <label className="flex flex-col gap-1 text-sm">
                Mật khẩu hiện tại
                <input
                  className="h-11 rounded-lg border border-black/10 bg-white px-3 outline-none transition focus:border-blue-400 focus:ring-2 focus:ring-blue-100"
                  type="password"
                  value={form.currentPassword}
                  onChange={(e) => updateField("currentPassword", e.target.value)}
                  autoComplete="current-password"
                />
              </label>

              <label className="flex flex-col gap-1 text-sm">
                Mật khẩu mới
                <input
                  className="h-11 rounded-lg border border-black/10 bg-white px-3 outline-none transition focus:border-blue-400 focus:ring-2 focus:ring-blue-100"
                  type="password"
                  value={form.newPassword}
                  onChange={(e) => updateField("newPassword", e.target.value)}
                  autoComplete="new-password"
                />
              </label>

              <label className="flex flex-col gap-1 text-sm">
                Xác nhận mật khẩu mới
                <input
                  className="h-11 rounded-lg border border-black/10 bg-white px-3 outline-none transition focus:border-blue-400 focus:ring-2 focus:ring-blue-100"
                  type="password"
                  value={form.confirmPassword}
                  onChange={(e) => updateField("confirmPassword", e.target.value)}
                  autoComplete="new-password"
                />
              </label>

              {error ? (
                <div className="rounded-lg border border-red-100 bg-red-50 p-3 text-xs text-red-700">
                  {error}
                </div>
              ) : null}

              {message ? (
                <div className="rounded-lg border border-emerald-100 bg-emerald-50 p-3 text-xs text-emerald-700">
                  {message}
                </div>
              ) : null}

              <button
                className="h-11 rounded-lg bg-blue-600 text-sm font-semibold text-white shadow-sm transition hover:bg-blue-700"
                type="submit"
              >
                Cập nhật mật khẩu
              </button>
            </form>
          </div>
        </section>
      </main>
    </AdminShell>
  );
}
