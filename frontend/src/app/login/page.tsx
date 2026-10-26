"use client";

import { useRouter } from "next/navigation";
import { useState } from "react";

type AuthResponse = {
  tokenType: string;
  accessToken: string;
  expiresInSeconds: number;
  user: {
    id: number;
    type: string;
    username?: string | null;
    phone?: string | null;
    roles: string[];
    permissions?: string[];
  };
};

const API_BASE = (
  process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8080"
).replace(/\/+$/, "");

export default function AdminLoginPage() {
  const router = useRouter();
  const [identifier, setIdentifier] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  async function onSubmit(e: React.FormEvent) {
    e.preventDefault();
    setError(null);
    setLoading(true);
    try {
      const res = await fetch(`${API_BASE}/api/admin/auth/login`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ identifier, password }),
      });

      if (!res.ok) {
        const text = await res.text();
        throw new Error(text || "Login failed");
      }

      const data = (await res.json()) as AuthResponse;
      localStorage.setItem("adminToken", data.accessToken);
      localStorage.setItem("adminUser", JSON.stringify(data.user));
      router.push("/");
    } catch (err) {
      setError(err instanceof Error ? err.message : "Login failed");
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="flex min-h-screen items-center justify-center bg-gradient-to-br from-blue-50 via-white to-indigo-50 px-6 py-10 text-[#111827]">
      <div className="grid w-full max-w-4xl overflow-hidden rounded-3xl border border-black/5 bg-white shadow-xl md:grid-cols-[1.1fr_0.9fr]">
        <div className="flex flex-col gap-6 border-b border-black/5 p-8 md:border-b-0 md:border-r">
          <div className="inline-flex w-fit items-center gap-2 rounded-full bg-blue-50 px-3 py-1 text-xs font-semibold text-blue-700">
            MiniBank Admin
          </div>
          <div>
            <h1 className="text-3xl font-semibold tracking-tight">Đăng nhập quản trị</h1>
            <p className="mt-2 text-sm text-zinc-500">
              Vui lòng đăng nhập bằng tài khoản quản trị được cấp.
            </p>
          </div>

          <form className="flex flex-col gap-4" onSubmit={onSubmit}>
            <label className="flex flex-col gap-1 text-sm">
              Tên đăng nhập
              <input
                className="h-11 rounded-lg border border-black/10 bg-white px-3 outline-none transition focus:border-blue-400 focus:ring-2 focus:ring-blue-100"
                value={identifier}
                onChange={(e) => setIdentifier(e.target.value)}
                autoComplete="username"
                placeholder="admin@gmail.com"
                required
              />
            </label>

            <label className="flex flex-col gap-1 text-sm">
              Mật khẩu
              <input
                className="h-11 rounded-lg border border-black/10 bg-white px-3 outline-none transition focus:border-blue-400 focus:ring-2 focus:ring-blue-100"
                type="password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                autoComplete="current-password"
                placeholder="••••••••"
                required
              />
            </label>

            {error ? (
              <div className="rounded-lg border border-red-100 bg-red-50 p-3 text-sm text-red-700">
                {error}
              </div>
            ) : null}

            <button
              className="h-11 rounded-lg bg-blue-600 text-sm font-semibold text-white shadow-sm transition hover:bg-blue-700 disabled:opacity-60"
              type="submit"
              disabled={loading}
            >
              {loading ? "Đang đăng nhập..." : "Đăng nhập"}
            </button>
          </form>
        </div>

        <div className="flex flex-col justify-between gap-6 bg-gradient-to-br from-blue-600 to-indigo-600 p-8 text-white">
          <div>
            <div className="text-sm font-semibold uppercase tracking-[0.2em] text-white/70">
              Quy định nội bộ
            </div>
            <h2 className="mt-3 text-2xl font-semibold">Bảo mật truy cập hệ thống</h2>
            <p className="mt-3 text-sm text-white/80">
              Tài khoản admin chỉ được tạo bởi quản trị viên và chia sẻ nội bộ.
              Vui lòng không cung cấp thông tin đăng nhập ra bên ngoài.
            </p>
          </div>
          <div className="rounded-2xl bg-white/10 p-4 text-xs text-white/80">
            <div className="font-semibold text-white">Hỗ trợ nhanh</div>
            <div className="mt-2">Nếu gặp lỗi đăng nhập, liên hệ phòng IT nội bộ.</div>
          </div>
        </div>
      </div>
    </div>
  );
}
