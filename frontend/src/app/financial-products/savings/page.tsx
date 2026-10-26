"use client";

import { useCallback, useEffect, useMemo, useState } from "react";
import AdminShell from "../../components/admin-shell";

const API_BASE = (
  process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8080"
).replace(/\/+$/, "");

type SavingProductItem = {
  id: number;
  code: string;
  name: string;
  currency: string;
  termUnit: string;
  termValue: number;
  interestRateType: string;
  baseInterestRate: number;
  interestAccrualFrequency: string;
  interestPostingFrequency: string;
  minOpenAmount: number;
  maxOpenAmount: number | null;
  closeFeeRate?: number | null;
  status: string;
};

type ProductFormState = {
  code: string;
  name: string;
  termValue: string;
  interestRateType: string;
  baseInterestRate: string;
  interestAccrualFrequency: string;
  interestPostingFrequency: string;
  minOpenAmount: string;
  maxOpenAmount: string;
  closeFeeRate: string;
  status: string;
};

const defaultProductForm: ProductFormState = {
  code: "",
  name: "",
  termValue: "",
  interestRateType: "FIXED",
  baseInterestRate: "",
  interestAccrualFrequency: "DAILY",
  interestPostingFrequency: "END_OF_TERM",
  minOpenAmount: "",
  maxOpenAmount: "",
  closeFeeRate: "",
  status: "active",
};

function formatCurrency(value: number | null | undefined) {
  if (value === null || value === undefined) return "-";
  return value
    .toFixed(0)
    .replace(/\B(?=(\d{3})+(?!\d))/g, ".");
}

function formatPercent(value: number | null | undefined) {
  if (value === null || value === undefined) return "-";
  return `${(value * 100).toFixed(2).replace(/\.00$/, "")}%`;
}

function toDecimalPercent(input: string) {
  const normalized = input.replace(/,/g, ".").trim();
  if (!normalized) return null;
  const parsed = Number(normalized);
  if (Number.isNaN(parsed)) return null;
  return parsed / 100;
}

function toNumber(input: string) {
  const normalized = input.replace(/,/g, "").trim();
  if (!normalized) return null;
  const parsed = Number(normalized);
  if (Number.isNaN(parsed)) return null;
  return parsed;
}

function toPercentInput(value: number | null | undefined) {
  if (value === null || value === undefined) return "";
  return (value * 100).toFixed(2).replace(/\.00$/, "");
}

function formatFrequency(value: string | null | undefined) {
  const normalized = value?.toUpperCase();
  switch (normalized) {
    case "DAILY":
      return "Hang ngay";
    case "MONTHLY":
      return "Hang thang";
    case "QUARTERLY":
      return "Hang quy";
    case "END_OF_TERM":
      return "Cuoi ky";
    default:
      return value ?? "-";
  }
}

export default function SavingProductsPage() {
  const [token, setToken] = useState<string | null>(null);
  const [items, setItems] = useState<SavingProductItem[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [query, setQuery] = useState("");

  const [modalOpen, setModalOpen] = useState(false);
  const [editing, setEditing] = useState<SavingProductItem | null>(null);
  const [form, setForm] = useState<ProductFormState>(defaultProductForm);
  const [saving, setSaving] = useState(false);
  const [statusUpdatingId, setStatusUpdatingId] = useState<number | null>(null);

  useEffect(() => {
    const t = localStorage.getItem("adminToken");
    setToken(t);
  }, []);

  const authHeader = useMemo<HeadersInit>(() => {
    if (!token) return {} as HeadersInit;
    return { Authorization: `Bearer ${token}` } as HeadersInit;
  }, [token]);

  const fetchProducts = useCallback(async () => {
    if (!token) return;
    setLoading(true);
    setError(null);
    try {
      const res = await fetch(`${API_BASE}/api/admin/financial-products/saving-products`, {
        headers: {
          "Content-Type": "application/json",
          ...authHeader,
        },
      });
      if (!res.ok) {
        const text = await res.text();
        throw new Error(text || "Load failed");
      }
      const data = (await res.json()) as SavingProductItem[];
      setItems(data);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Load failed");
    } finally {
      setLoading(false);
    }
  }, [authHeader, token]);

  useEffect(() => {
    if (token) {
      fetchProducts();
    }
  }, [fetchProducts, token]);

  const filtered = useMemo(() => {
    if (!query.trim()) return items;
    const q = query.trim().toLowerCase();
    return items.filter((item) =>
      [item.code, item.name].some((value) => value?.toLowerCase().includes(q))
    );
  }, [items, query]);

  function openCreate() {
    setEditing(null);
    setForm(defaultProductForm);
    setModalOpen(true);
  }

  function openEdit(item: SavingProductItem) {
    setEditing(item);
    setForm({
      code: item.code ?? "",
      name: item.name ?? "",
      termValue: String(item.termValue ?? ""),
      interestRateType: item.interestRateType ?? "FIXED",
      baseInterestRate: toPercentInput(item.baseInterestRate),
      interestAccrualFrequency: item.interestAccrualFrequency ?? "DAILY",
      interestPostingFrequency: item.interestPostingFrequency ?? "END_OF_TERM",
      minOpenAmount: item.minOpenAmount ? String(item.minOpenAmount) : "",
      maxOpenAmount: item.maxOpenAmount ? String(item.maxOpenAmount) : "",
      closeFeeRate: toPercentInput(item.closeFeeRate ?? null),
      status: item.status ?? "active",
    });
    setModalOpen(true);
  }

  function updateForm<K extends keyof ProductFormState>(key: K, value: ProductFormState[K]) {
    setForm((prev) => ({ ...prev, [key]: value }));
  }

  async function saveProduct(e: React.FormEvent) {
    e.preventDefault();
    if (!token) return;
    setSaving(true);
    setError(null);

    const payload = {
      code: form.code.trim().toUpperCase(),
      name: form.name.trim(),
      currency: "VND",
      termUnit: "MONTH",
      termValue: Number(form.termValue),
      interestRateType: form.interestRateType,
      baseInterestRate: toDecimalPercent(form.baseInterestRate),
      interestAccrualFrequency: form.interestAccrualFrequency,
      interestPostingFrequency: form.interestPostingFrequency,
      minOpenAmount: toNumber(form.minOpenAmount),
      maxOpenAmount: toNumber(form.maxOpenAmount),
      closeFeeRate: toDecimalPercent(form.closeFeeRate),
      status: form.status,
    };

    try {
      const url = editing
        ? `${API_BASE}/api/admin/financial-products/saving-products/${editing.id}`
        : `${API_BASE}/api/admin/financial-products/saving-products`;
      const method = editing ? "PUT" : "POST";
      const res = await fetch(url, {
        method,
        headers: {
          "Content-Type": "application/json",
          ...authHeader,
        },
        body: JSON.stringify(payload),
      });
      if (!res.ok) {
        const text = await res.text();
        throw new Error(text || "Save failed");
      }
      await fetchProducts();
      setModalOpen(false);
      setForm(defaultProductForm);
      setEditing(null);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Save failed");
    } finally {
      setSaving(false);
    }
  }

  async function toggleStatus(item: SavingProductItem) {
    if (!token) return;
    const nextStatus = item.status?.toLowerCase() === "active" ? "inactive" : "active";
    setStatusUpdatingId(item.id);
    setError(null);
    try {
      const res = await fetch(
        `${API_BASE}/api/admin/financial-products/saving-products/${item.id}/status`,
        {
          method: "PATCH",
          headers: {
            "Content-Type": "application/json",
            ...authHeader,
          },
          body: JSON.stringify({ status: nextStatus }),
        }
      );
      if (!res.ok) {
        const text = await res.text();
        throw new Error(text || "Update failed");
      }
      await fetchProducts();
    } catch (err) {
      setError(err instanceof Error ? err.message : "Update failed");
    } finally {
      setStatusUpdatingId(null);
    }
  }

  if (!token) {
    return (
      <div className="flex min-h-screen items-center justify-center bg-[#f6f7fb] px-6 text-[#111827]">
        <div className="w-full max-w-md rounded-2xl border border-black/5 bg-white p-6">
          <h1 className="text-xl font-semibold">San pham tiet kiem</h1>
          <p className="mt-2 text-sm text-zinc-500">Vui long dang nhap de tiep tuc.</p>
          <a
            className="mt-4 inline-flex h-10 items-center justify-center rounded-lg bg-blue-600 px-4 text-sm font-semibold text-white"
            href="/login"
          >
            Di toi dang nhap
          </a>
        </div>
      </div>
    );
  }

  return (
    <AdminShell
      title="San pham tiet kiem"
      subtitle="Quan ly danh muc san pham tiet kiem"
      actions={
        <button
          className="h-10 rounded-lg bg-blue-600 px-4 text-sm font-semibold text-white shadow-sm transition hover:bg-blue-700"
          onClick={openCreate}
          type="button"
        >
          Tao san pham moi
        </button>
      }
    >
      <section className="rounded-2xl border border-black/5 bg-white p-6 shadow-sm">
        <div className="flex flex-wrap items-center justify-between gap-3">
          <div>
            <div className="text-sm font-semibold">Danh sach san pham</div>
            <div className="text-xs text-zinc-500">Cap nhat lai suat va dieu kien mo so</div>
          </div>
          <div className="flex items-center gap-2">
            <div className="flex h-10 items-center gap-2 rounded-lg border border-black/10 bg-white px-3">
              <span className="text-xs text-zinc-400">🔎</span>
              <input
                className="h-8 w-52 bg-transparent text-sm outline-none"
                placeholder="Tim theo ma, ten..."
                value={query}
                onChange={(e) => setQuery(e.target.value)}
              />
            </div>
            <button
              className="h-10 rounded-lg border border-black/10 px-3 text-sm"
              onClick={fetchProducts}
              type="button"
            >
              Lam moi
            </button>
          </div>
        </div>

        {error ? (
          <div className="mt-4 rounded-lg border border-red-100 bg-red-50 p-3 text-sm text-red-700">
            {error}
          </div>
        ) : null}

        <div className="mt-5 grid gap-4 md:grid-cols-2 xl:grid-cols-3">
          {loading ? (
            <div className="col-span-full rounded-xl border border-dashed border-zinc-200 p-8 text-center text-sm text-zinc-400">
              Dang tai du lieu...
            </div>
          ) : filtered.length === 0 ? (
            <div className="col-span-full rounded-xl border border-dashed border-zinc-200 p-8 text-center text-sm text-zinc-400">
              Chua co san pham nao.
            </div>
          ) : (
            filtered.map((item) => (
              <div key={item.id} className="rounded-2xl border border-black/5 bg-white shadow-sm">
                <div className="rounded-t-2xl bg-blue-600 px-4 py-3 text-white">
                  <div className="text-xs text-white/70">Ma san pham</div>
                  <div className="text-lg font-semibold">{item.code}</div>
                  <div className="text-xs text-white/80">{item.name}</div>
                </div>
                <div className="p-4 text-sm text-zinc-700">
                  <div className="flex items-center justify-between">
                    <span className="text-xs text-zinc-400">Ky han</span>
                    <span>{item.termValue} thang</span>
                  </div>
                  <div className="mt-2 flex items-center justify-between">
                    <span className="text-xs text-zinc-400">Lai suat co ban</span>
                    <span className="font-semibold text-blue-600">
                      {formatPercent(item.baseInterestRate)} / nam
                    </span>
                  </div>
                  <div className="mt-2 flex items-center justify-between">
                    <span className="text-xs text-zinc-400">Loai lai suat</span>
                    <span>{item.interestRateType}</span>
                  </div>
                  <div className="mt-2 flex items-center justify-between">
                    <span className="text-xs text-zinc-400">So tien toi thieu</span>
                    <span>{formatCurrency(item.minOpenAmount)} d</span>
                  </div>
                  <div className="mt-2 flex items-center justify-between">
                    <span className="text-xs text-zinc-400">So tien toi da</span>
                    <span>{item.maxOpenAmount ? `${formatCurrency(item.maxOpenAmount)} d` : "Khong"}</span>
                  </div>
                  <div className="mt-2 flex items-center justify-between">
                    <span className="text-xs text-zinc-400">Phi tat toan som</span>
                    <span>{formatPercent(item.closeFeeRate ?? null)}</span>
                  </div>
                  <div className="mt-2 flex items-center justify-between">
                    <span className="text-xs text-zinc-400">Tinh lai theo</span>
                    <span>{formatFrequency(item.interestAccrualFrequency)}</span>
                  </div>
                  <div className="mt-2 flex items-center justify-between">
                    <span className="text-xs text-zinc-400">Tra lai</span>
                    <span>{formatFrequency(item.interestPostingFrequency)}</span>
                  </div>
                  <div className="mt-3 flex items-center justify-between">
                    <span className="text-xs text-zinc-400">Trang thai</span>
                    <span
                      className={`rounded-full px-2 py-1 text-xs ${item.status?.toLowerCase() === "active"
                        ? "bg-emerald-50 text-emerald-600"
                        : "bg-rose-50 text-rose-600"
                        }`}
                    >
                      {item.status?.toLowerCase() === "active" ? "Hoat dong" : "Da khoa"}
                    </span>
                  </div>
                </div>
                <div className="flex items-center gap-2 border-t border-black/5 px-4 py-3 text-sm">
                  <button
                    className="flex-1 rounded-lg bg-blue-50 py-2 text-blue-600 hover:bg-blue-100"
                    type="button"
                    onClick={() => openEdit(item)}
                  >
                    Sua
                  </button>
                  <button
                    className="flex-1 rounded-lg bg-rose-50 py-2 text-rose-600 hover:bg-rose-100 disabled:opacity-60"
                    type="button"
                    onClick={() => toggleStatus(item)}
                    disabled={statusUpdatingId === item.id}
                  >
                    {statusUpdatingId === item.id
                      ? "Dang xu ly..."
                      : item.status?.toLowerCase() === "active"
                        ? "Khoa"
                        : "Mo"}
                  </button>
                </div>
              </div>
            ))
          )}
        </div>
      </section>

      {modalOpen ? (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40 px-4">
          <div className="w-full max-w-2xl rounded-2xl bg-white p-6 shadow-xl">
            <div className="flex items-start justify-between">
              <div>
                <div className="text-lg font-semibold">
                  {editing ? "Cap nhat san pham" : "Tao san pham tiet kiem"}
                </div>
                <div className="text-xs text-zinc-500">Cap nhat thong tin san pham</div>
              </div>
              <button
                className="text-zinc-400"
                type="button"
                onClick={() => setModalOpen(false)}
              >
                ✕
              </button>
            </div>

            <form className="mt-5 grid gap-4 md:grid-cols-2" onSubmit={saveProduct}>
              <label className="text-sm">
                Ma san pham
                <input
                  className="mt-1 h-10 w-full rounded-lg border border-black/10 px-3 outline-none transition focus:border-blue-400 focus:ring-2 focus:ring-blue-100"
                  placeholder="VD: TK12T"
                  value={form.code}
                  onChange={(e) => updateForm("code", e.target.value)}
                  required
                />
              </label>

              <label className="text-sm">
                Ten san pham
                <input
                  className="mt-1 h-10 w-full rounded-lg border border-black/10 px-3 outline-none transition focus:border-blue-400 focus:ring-2 focus:ring-blue-100"
                  placeholder="Tiet kiem 12 thang"
                  value={form.name}
                  onChange={(e) => updateForm("name", e.target.value)}
                  required
                />
              </label>

              <label className="text-sm">
                Ky han (thang)
                <input
                  className="mt-1 h-10 w-full rounded-lg border border-black/10 px-3 outline-none transition focus:border-blue-400 focus:ring-2 focus:ring-blue-100"
                  placeholder="12"
                  type="number"
                  min={1}
                  value={form.termValue}
                  onChange={(e) => updateForm("termValue", e.target.value)}
                  required
                />
              </label>

              <label className="text-sm">
                Loai lai suat
                <select
                  className="mt-1 h-10 w-full rounded-lg border border-black/10 px-3 outline-none"
                  value={form.interestRateType}
                  onChange={(e) => updateForm("interestRateType", e.target.value)}
                >
                  <option value="FIXED">Co dinh</option>
                  <option value="FLOATING">Thay doi</option>
                </select>
              </label>

              <label className="text-sm">
                Lai suat co ban (%/nam)
                <input
                  className="mt-1 h-10 w-full rounded-lg border border-black/10 px-3 outline-none transition focus:border-blue-400 focus:ring-2 focus:ring-blue-100"
                  placeholder="5.2"
                  value={form.baseInterestRate}
                  onChange={(e) => updateForm("baseInterestRate", e.target.value)}
                  required
                />
              </label>

              <label className="text-sm">
                Tinh lai theo thoi han
                <select
                  className="mt-1 h-10 w-full rounded-lg border border-black/10 px-3 outline-none"
                  value={form.interestAccrualFrequency}
                  onChange={(e) => updateForm("interestAccrualFrequency", e.target.value)}
                >
                  <option value="DAILY">Hang ngay</option>
                  <option value="MONTHLY">Hang thang</option>
                  <option value="QUARTERLY">Hang quy</option>
                </select>
              </label>

              <label className="text-sm">
                Thoi han tra lai
                <select
                  className="mt-1 h-10 w-full rounded-lg border border-black/10 px-3 outline-none"
                  value={form.interestPostingFrequency}
                  onChange={(e) => updateForm("interestPostingFrequency", e.target.value)}
                >
                  <option value="END_OF_TERM">Cuoi ky</option>
                  <option value="MONTHLY">Hang thang</option>
                  <option value="QUARTERLY">Hang quy</option>
                </select>
              </label>

              <label className="text-sm">
                So tien toi thieu (VND)
                <input
                  className="mt-1 h-10 w-full rounded-lg border border-black/10 px-3 outline-none transition focus:border-blue-400 focus:ring-2 focus:ring-blue-100"
                  placeholder="1000000"
                  value={form.minOpenAmount}
                  onChange={(e) => updateForm("minOpenAmount", e.target.value)}
                  required
                />
              </label>

              <label className="text-sm">
                So tien toi da (VND)
                <input
                  className="mt-1 h-10 w-full rounded-lg border border-black/10 px-3 outline-none transition focus:border-blue-400 focus:ring-2 focus:ring-blue-100"
                  placeholder="Khong bat buoc"
                  value={form.maxOpenAmount}
                  onChange={(e) => updateForm("maxOpenAmount", e.target.value)}
                />
              </label>

              <label className="text-sm">
                Phi tat toan som (%)
                <input
                  className="mt-1 h-10 w-full rounded-lg border border-black/10 px-3 outline-none transition focus:border-blue-400 focus:ring-2 focus:ring-blue-100"
                  placeholder="0.5"
                  value={form.closeFeeRate}
                  onChange={(e) => updateForm("closeFeeRate", e.target.value)}
                />
              </label>

              <label className="text-sm">
                Trang thai
                <select
                  className="mt-1 h-10 w-full rounded-lg border border-black/10 px-3 outline-none"
                  value={form.status}
                  onChange={(e) => updateForm("status", e.target.value)}
                >
                  <option value="active">Hoat dong</option>
                  <option value="inactive">Tam dung</option>
                </select>
              </label>

              <div className="mt-2 flex items-center justify-end gap-2 md:col-span-2">
                <button
                  className="h-10 rounded-lg border border-black/10 px-4 text-sm"
                  type="button"
                  onClick={() => setModalOpen(false)}
                >
                  Huy
                </button>
                <button
                  className="h-10 rounded-lg bg-blue-600 px-4 text-sm font-semibold text-white shadow-sm disabled:opacity-60"
                  type="submit"
                  disabled={saving}
                >
                  {saving ? "Dang luu..." : "Luu thay doi"}
                </button>
              </div>
            </form>
          </div>
        </div>
      ) : null}

    </AdminShell>
  );
}
