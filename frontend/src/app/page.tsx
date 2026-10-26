"use client";

import Link from "next/link";
import { useEffect, useMemo, useState, useSyncExternalStore } from "react";
import AdminShell from "./components/admin-shell";


type DashboardMetric = {
  key: string;
  label: string;
  value: number | string;
  valueType: "number" | "money" | string;
  delta?: string | null;
  tone: string;
  href?: string | null;
};

type TimePoint = {
  label: string;
  value: number | string;
};

type ProductMetric = {
  label: string;
  value: number | string;
};

type RecentTransaction = {
  id: number;
  transactionCode: string;
  accountName?: string | null;
  description?: string | null;
  amount: number | string;
  direction: "in" | "out" | string;
  transactionType: string;
  status: string;
  createdAt: string;
};

type RequestMetric = {
  label: string;
  value: number;
  href: string;
};

type DashboardResponse = {
  updatedAt: string;
  metrics: DashboardMetric[];
  transactionCountSeries: TimePoint[];
  transactionAmountSeries: TimePoint[];
  productPerformance: ProductMetric[];
  recentTransactions: RecentTransaction[];
  pendingRequests: RequestMetric[];
};

const API_BASE = (
  process.env.NEXT_PUBLIC_API_BASE_URL ??
  process.env.NEXT_PUBLIC_API_URL ??
  "http://localhost:8080"
).replace(/\/+$/, "");

const toneStyles: Record<string, string> = {
  blue: "bg-blue-50 text-blue-600",
  amber: "bg-amber-50 text-amber-600",
  green: "bg-green-50 text-green-600",
  indigo: "bg-indigo-50 text-indigo-600",
  emerald: "bg-emerald-50 text-emerald-600",
  purple: "bg-purple-50 text-purple-600",
  orange: "bg-orange-50 text-orange-600",
  rose: "bg-rose-50 text-rose-600",
  teal: "bg-teal-50 text-teal-600",
  cyan: "bg-cyan-50 text-cyan-600",
};

function buildLinePath(values: number[], width: number, height: number) {
  const max = Math.max(...values, 1);
  const min = Math.min(...values, 0);
  const span = max - min || 1;
  return values
    .map((value, index) => {
      const x = values.length === 1 ? 0 : (index / (values.length - 1)) * width;
      const y = height - ((value - min) / span) * height;
      return `${index === 0 ? "M" : "L"}${x.toFixed(2)},${y.toFixed(2)}`;
    })
    .join(" ");
}

function toNumber(value: number | string | null | undefined) {
  const parsed = Number(value ?? 0);
  return Number.isFinite(parsed) ? parsed : 0;
}

function formatMetricValue(metric: DashboardMetric) {
  const value = toNumber(metric.value);
  if (metric.valueType === "money") {
    if (value >= 1_000_000_000) return `${(value / 1_000_000_000).toLocaleString("vi-VN", { maximumFractionDigits: 1 })}B VND`;
    if (value >= 1_000_000) return `${(value / 1_000_000).toLocaleString("vi-VN", { maximumFractionDigits: 1 })}M VND`;
    return formatVnd(value);
  }
  return new Intl.NumberFormat("vi-VN").format(value);
}

function formatVnd(value: number) {
  return new Intl.NumberFormat("vi-VN", {
    style: "currency",
    currency: "VND",
    maximumFractionDigits: 0,
  }).format(value);
}

function formatUpdatedAt(value?: string) {
  if (!value) return "";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return "";
  return `Cap nhat luc ${date.toLocaleString("vi-VN")}`;
}

function subscribeToken(onStoreChange: () => void) {
  if (typeof window === "undefined") return () => {};
  window.addEventListener("storage", onStoreChange);
  window.addEventListener("admin-token-change", onStoreChange);
  return () => {
    window.removeEventListener("storage", onStoreChange);
    window.removeEventListener("admin-token-change", onStoreChange);
  };
}

function getTokenSnapshot() {
  return typeof window === "undefined" ? null : localStorage.getItem("adminToken");
}

function getServerTokenSnapshot() {
  return null;
}

export default function Home() {
  const token = useSyncExternalStore(subscribeToken, getTokenSnapshot, getServerTokenSnapshot);
  const [data, setData] = useState<DashboardResponse | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!token) return;
    let cancelled = false;
    const loadDashboard = async () => {
      setLoading(true);
      setError(null);
      try {
        const res = await fetch(`${API_BASE}/api/admin/dashboard`, {
          headers: { Authorization: `Bearer ${token}` },
        });
        if (!res.ok) throw new Error((await res.text()) || "Khong the tai dashboard");
        const json = (await res.json()) as DashboardResponse;
        if (!cancelled) setData(json);
      } catch (err) {
        if (!cancelled) setError(err instanceof Error ? err.message : "Khong the tai dashboard");
      } finally {
        if (!cancelled) setLoading(false);
      }
    };
    loadDashboard();
    return () => {
      cancelled = true;
    };
  }, [token]);

  const countSeries = useMemo(() => data?.transactionCountSeries ?? [], [data]);
  const amountSeries = useMemo(() => data?.transactionAmountSeries ?? [], [data]);
  const countValues = countSeries.map((item) => toNumber(item.value));
  const amountValues = amountSeries.map((item) => toNumber(item.value));
  const linePath = buildLinePath(countValues.length ? countValues : [0], 520, 160);
  const lineValuePath = buildLinePath(amountValues.length ? amountValues : [0], 520, 160);
  const countLabels = countSeries.filter((_, index) => index % 4 === 0 || index === countSeries.length - 1);
  const amountLabels = amountSeries.filter((_, index) => index % 4 === 0 || index === amountSeries.length - 1);
  const maxProduct = Math.max(...(data?.productPerformance ?? []).map((item) => toNumber(item.value)), 1);

  if (!token) {
    return (
      <div className="flex flex-1 items-center justify-center bg-background px-6">
        <main className="w-full max-w-md rounded-2xl border border-black/[.08] bg-background p-6 dark:border-white/[.145]">
          <h1 className="text-2xl font-semibold tracking-tight">MiniBank Admin</h1>
          <p className="mt-2 text-sm text-zinc-600 dark:text-zinc-400">Vui long dang nhap de tiep tuc.</p>
          <div className="mt-6 flex flex-col gap-3">
            <Link className="flex h-11 items-center justify-center rounded-lg bg-foreground text-background" href="/login">
              Di toi dang nhap
            </Link>
          </div>
        </main>
      </div>
    );
  }

  return (
    <AdminShell
      title="Dashboard Tong quan"
      subtitle={data ? formatUpdatedAt(data.updatedAt) : loading ? "Dang cap nhat du lieu..." : "Du lieu tu database"}
      onLogout={() => {
        window.dispatchEvent(new Event("admin-token-change"));
      }}
    >
      {error ? <div className="rounded-xl border border-red-100 bg-red-50 p-4 text-sm text-red-700">{error}</div> : null}

      <section className="grid gap-4 md:grid-cols-2 xl:grid-cols-5">
        {(data?.metrics ?? []).map((item) => {
          const cardClassName = `rounded-2xl border border-black/5 bg-white p-4 text-left shadow-sm ${item.href ? "cursor-pointer transition-all hover:-translate-y-0.5 hover:shadow-md" : ""}`;
          const content = (
            <>
              <div className="flex items-center justify-between">
                <div className="text-xs text-zinc-500">{item.label}</div>
                <span className={`flex h-8 w-8 items-center justify-center rounded-xl text-xs ${toneStyles[item.tone] ?? "bg-zinc-100 text-zinc-600"}`}>●</span>
              </div>
              <div className="mt-3 text-2xl font-semibold">{formatMetricValue(item)}</div>
              <div className="mt-1 text-xs text-zinc-500">{item.delta || "Du lieu hien tai"}</div>
            </>
          );
          return item.href ? (
            <Link key={item.key} href={item.href} className={cardClassName}>{content}</Link>
          ) : (
            <div key={item.key} className={cardClassName}>{content}</div>
          );
        })}
        {loading && !data ? <div className="rounded-2xl border border-black/5 bg-white p-4 text-sm text-zinc-500 shadow-sm">Dang tai KPI...</div> : null}
      </section>

      <section className="mt-6 grid gap-6 xl:grid-cols-2">
        <div className="rounded-2xl border border-black/5 bg-white p-5 shadow-sm">
          <div className="text-sm font-semibold">Giao dich theo thoi gian</div>
          <div className="text-xs text-zinc-500">So luong giao dich trong ngay</div>
          <div className="mt-4 h-48">
            <svg viewBox="0 0 520 180" className="h-full w-full">
              <defs>
                <linearGradient id="lineFill" x1="0" x2="0" y1="0" y2="1">
                  <stop offset="0%" stopColor="#60a5fa" stopOpacity="0.3" />
                  <stop offset="100%" stopColor="#60a5fa" stopOpacity="0" />
                </linearGradient>
              </defs>
              <path d={`${linePath} L520,180 L0,180 Z`} fill="url(#lineFill)" stroke="none" />
              <path d={linePath} fill="none" stroke="#3b82f6" strokeWidth="3" />
            </svg>
            <div className="mt-2 flex justify-between text-xs text-zinc-400">
              {countLabels.map((item) => <span key={item.label}>{item.label}</span>)}
            </div>
          </div>
        </div>

        <div className="rounded-2xl border border-black/5 bg-white p-5 shadow-sm">
          <div className="text-sm font-semibold">Gia tri giao dich theo thoi gian</div>
          <div className="text-xs text-zinc-500">Tong tien giao dich (trieu VND)</div>
          <div className="mt-4 h-48">
            <svg viewBox="0 0 520 180" className="h-full w-full">
              <defs>
                <linearGradient id="lineFill2" x1="0" x2="0" y1="0" y2="1">
                  <stop offset="0%" stopColor="#34d399" stopOpacity="0.3" />
                  <stop offset="100%" stopColor="#34d399" stopOpacity="0" />
                </linearGradient>
              </defs>
              <path d={`${lineValuePath} L520,180 L0,180 Z`} fill="url(#lineFill2)" stroke="none" />
              <path d={lineValuePath} fill="none" stroke="#10b981" strokeWidth="3" />
            </svg>
            <div className="mt-2 flex justify-between text-xs text-zinc-400">
              {amountLabels.map((item) => <span key={item.label}>{item.label}</span>)}
            </div>
          </div>
        </div>
      </section>

      <section className="mt-6 rounded-2xl border border-black/5 bg-white p-5 shadow-sm">
        <div className="text-sm font-semibold">Hieu suat san pham</div>
        <div className="text-xs text-zinc-500">Gia tri theo san pham (ty VND)</div>
        <div className="mt-6 grid gap-6 md:grid-cols-3">
          {(data?.productPerformance ?? []).map((item) => {
            const value = toNumber(item.value);
            return (
              <div key={item.label} className="flex flex-col items-center gap-3">
                <div className="flex h-28 w-full items-end rounded-xl bg-zinc-100">
                  <div className="w-full rounded-xl bg-blue-500" style={{ height: `${Math.max(6, (value / maxProduct) * 100)}%` }} />
                </div>
                <div className="text-xs text-zinc-500">{item.label}: {value.toLocaleString("vi-VN")}</div>
              </div>
            );
          })}
        </div>
      </section>

      <section className="mt-6 grid gap-6 lg:grid-cols-3">
        <div className="rounded-2xl border border-black/5 bg-white p-5 shadow-sm">
          <div className="text-sm font-semibold">Yeu cau can xu ly</div>
          <div className="mt-4 space-y-3 text-xs text-zinc-600">
            {(data?.pendingRequests ?? []).map((item) => (
              <Link key={item.label} href={item.href} className="flex items-center justify-between rounded-lg px-2 py-1 hover:bg-zinc-50">
                <span>{item.label}</span>
                <span className="font-semibold text-zinc-800">{item.value}</span>
              </Link>
            ))}
          </div>
        </div>

        <div className="rounded-2xl border border-black/5 bg-white p-5 shadow-sm lg:col-span-2">
          <div className="flex items-center justify-between">
            <div className="text-sm font-semibold">Giao dich gan day</div>
            <Link href="/transactions/list" className="text-xs font-medium text-blue-600">Xem tat ca</Link>
          </div>
          <div className="mt-4 divide-y divide-black/5 text-sm">
            {(data?.recentTransactions ?? []).map((item) => {
              const incoming = item.direction === "in";
              return (
                <div key={item.id} className="flex items-center justify-between gap-4 py-3">
                  <div>
                    <div className="font-semibold text-zinc-800">{item.accountName || item.transactionCode}</div>
                    <div className="text-xs text-zinc-500">{item.transactionType} · {item.status}</div>
                  </div>
                  <div className={`text-sm font-semibold ${incoming ? "text-emerald-600" : "text-red-500"}`}>
                    {incoming ? "+" : "-"}{formatVnd(toNumber(item.amount))}
                  </div>
                </div>
              );
            })}
            {!loading && data?.recentTransactions?.length === 0 ? <div className="py-6 text-center text-xs text-zinc-400">Chua co giao dich.</div> : null}
          </div>
        </div>
      </section>
    </AdminShell>
  );
}
