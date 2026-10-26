"use client";

import Link from "next/link";
import AdminShell from "../components/admin-shell";

export default function SystemPage() {
  return (
    <AdminShell title="Quản trị hệ thống" subtitle="Cấu hình vai trò, FAQ, mức duyệt và nhật ký">
      <main className="mx-auto max-w-6xl space-y-6">
        <section className="rounded-2xl border border-black/5 bg-white p-6 shadow-sm">
          <div className="flex flex-wrap items-start justify-between gap-4">
            <div>
              <p className="text-xs font-semibold uppercase tracking-[0.2em] text-blue-600">System hub</p>
              <h2 className="mt-2 text-2xl font-semibold text-[#111827]">Quản trị hệ thống</h2>
              <p className="mt-2 max-w-2xl text-sm text-zinc-500">
                Đi nhanh tới quản lý vai trò, FAQ chatbot, mức duyệt nghiệp vụ và nhật ký hệ thống.
              </p>
            </div>
            <div className="rounded-2xl bg-blue-50 px-4 py-3 text-sm text-blue-800">
              Mức duyệt áp dụng theo loại nghiệp vụ và ngưỡng tiền cho vay / tiết kiệm.
            </div>
          </div>

          <div className="mt-6 grid gap-4 md:grid-cols-4">
            <SystemLink href="/system/roles" title="Vai trò" text="Chuẩn hóa quyền theo 5 role MVP của MiniBank." />
            <SystemLink href="/staff" title="Nhân viên" text="Tạo tài khoản, khóa/mở khóa và gán vai trò." />
            <SystemLink href="/system/chatbot" title="FAQ / Chat CSKH" text="Quản lý cây hỏi đáp và luồng chat được chuyển cho CSKH." />
            <SystemLink href="/system/approval-policies" title="Mức duyệt nghiệp vụ" text="Cấu hình số người duyệt theo ngưỡng tiền." accent />
          </div>
        </section>

        <section className="rounded-2xl border border-black/5 bg-white p-6 shadow-sm">
          <div className="flex items-center justify-between gap-4">
            <div>
              <h3 className="text-sm font-semibold text-[#111827]">Lối tắt cấu hình</h3>
              <p className="mt-1 text-sm text-zinc-500">RBAC nằm ở tab Vai trò; nhân viên chỉ được gán role để tránh lệch quyền.</p>
            </div>
            <div className="flex flex-wrap gap-2 text-xs font-semibold">
              <Link href="/system/approval-policies" className="rounded-full bg-blue-600 px-3 py-2 text-white">Mức duyệt</Link>
              <Link href="/system/roles" className="rounded-full border border-black/10 px-3 py-2 text-zinc-700">Vai trò</Link>
              <Link href="/system/chatbot" className="rounded-full border border-black/10 px-3 py-2 text-zinc-700">FAQ</Link>
              <Link href="/system/audit" className="rounded-full border border-black/10 px-3 py-2 text-zinc-700">Nhật ký</Link>
            </div>
          </div>
        </section>
      </main>
    </AdminShell>
  );
}

function SystemLink({ href, title, text, accent = false }: { href: string; title: string; text: string; accent?: boolean }) {
  return (
    <Link
      href={href}
      className={`rounded-2xl border p-4 transition hover:-translate-y-0.5 hover:shadow-md ${
        accent ? "border-blue-200 bg-blue-50" : "border-black/5 bg-[#fcfcfd]"
      }`}
    >
      <div className={`text-sm font-semibold ${accent ? "text-blue-800" : "text-[#111827]"}`}>{title}</div>
      <div className={`mt-1 text-sm ${accent ? "text-blue-700/80" : "text-zinc-500"}`}>{text}</div>
    </Link>
  );
}
