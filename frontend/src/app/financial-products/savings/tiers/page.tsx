"use client";

import { useCallback, useEffect, useMemo, useState } from "react";
import AdminShell from "../../../components/admin-shell";

const API_BASE = (
  process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8080"
).replace(/\/+$/, "");

type SavingProductItem = {
  id: number;
  code: string;
  name: string;
  termValue: number;
};

type SavingTierItem = {
  id: number;
  savingProductId: number;
  savingProductName: string | null;
  minAmount: number;
  maxAmount: number | null;
  interestRate: number;
  effectiveFrom: string;
  effectiveTo: string | null;
};

type TierFormState = {
  minAmount: string;
  maxAmount: string;
  interestRate: string;
  effectiveFrom: string;
  effectiveTo: string;
};

const defaultTierForm: TierFormState = {
  minAmount: "",
  maxAmount: "",
  interestRate: "",
  effectiveFrom: "",
  effectiveTo: "",
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

function toDateInput(value: string | null | undefined) {
  if (!value) return "";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return "";
  return date.toISOString().slice(0, 10);
}

function isActiveTier(tier: SavingTierItem) {
  const today = new Date();
  const from = new Date(tier.effectiveFrom);
  const to = tier.effectiveTo ? new Date(tier.effectiveTo) : null;
  if (Number.isNaN(from.getTime())) return false;
  if (to && Number.isNaN(to.getTime())) return false;
  return from <= today && (!to || to >= today);
}

export default function SavingInterestTierPage() {
  const [token, setToken] = useState<string | null>(null);
  const [products, setProducts] = useState<SavingProductItem[]>([]);
  const [selectedProductId, setSelectedProductId] = useState<number | null>(null);
  const [tiers, setTiers] = useState<SavingTierItem[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const [modalOpen, setModalOpen] = useState(false);
  const [form, setForm] = useState<TierFormState>(defaultTierForm);
  const [editing, setEditing] = useState<SavingTierItem | null>(null);
  const [saving, setSaving] = useState(false);
  const [deletingId, setDeletingId] = useState<number | null>(null);

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
      setProducts(data);
      if (!selectedProductId && data.length > 0) {
        setSelectedProductId(data[0].id);
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : "Load failed");
    }
  }, [authHeader, selectedProductId, token]);

  const fetchTiers = useCallback(async (productId: number) => {
    if (!token) return;
    setLoading(true);
    setError(null);
    try {
      const url = new URL(`${API_BASE}/api/admin/financial-products/saving-interest-tiers`);
      url.searchParams.set("savingProductId", String(productId));
      const res = await fetch(url.toString(), {
        headers: {
          "Content-Type": "application/json",
          ...authHeader,
        },
      });
      if (!res.ok) {
        const text = await res.text();
        throw new Error(text || "Load failed");
      }
      const data = (await res.json()) as SavingTierItem[];
      setTiers(data);
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

  useEffect(() => {
    if (token && selectedProductId) {
      fetchTiers(selectedProductId);
    }
  }, [fetchTiers, selectedProductId, token]);

  function updateForm<K extends keyof TierFormState>(key: K, value: TierFormState[K]) {
    setForm((prev) => ({ ...prev, [key]: value }));
  }

  function openCreate() {
    setEditing(null);
    setForm(defaultTierForm);
    setModalOpen(true);
  }

  function openEdit(tier: SavingTierItem) {
    setEditing(tier);
    setForm({
      minAmount: String(tier.minAmount ?? ""),
      maxAmount: tier.maxAmount ? String(tier.maxAmount) : "",
      interestRate: toPercentInput(tier.interestRate),
      effectiveFrom: toDateInput(tier.effectiveFrom),
      effectiveTo: toDateInput(tier.effectiveTo),
    });
    setModalOpen(true);
  }

  async function saveTier(e: React.FormEvent) {
    e.preventDefault();
    if (!token || !selectedProductId) return;
    setSaving(true);
    setError(null);

    const payload = {
      savingProductId: selectedProductId,
      minAmount: toNumber(form.minAmount),
      maxAmount: toNumber(form.maxAmount),
      interestRate: toDecimalPercent(form.interestRate),
      effectiveFrom: form.effectiveFrom || null,
      effectiveTo: form.effectiveTo || null,
    };

    try {
      const url = editing
        ? `${API_BASE}/api/admin/financial-products/saving-interest-tiers/${editing.id}`
        : `${API_BASE}/api/admin/financial-products/saving-interest-tiers`;
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
      await fetchTiers(selectedProductId);
      setModalOpen(false);
      setForm(defaultTierForm);
      setEditing(null);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Save failed");
    } finally {
      setSaving(false);
    }
  }

  async function deleteTier(tierId: number) {
    if (!token || !selectedProductId) return;
    setDeletingId(tierId);
    setError(null);
    try {
      const res = await fetch(
        `${API_BASE}/api/admin/financial-products/saving-interest-tiers/${tierId}`,
        {
          method: "DELETE",
          headers: {
            "Content-Type": "application/json",
            ...authHeader,
          },
        }
      );
      if (!res.ok) {
        const text = await res.text();
        throw new Error(text || "Delete failed");
      }
      await fetchTiers(selectedProductId);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Delete failed");
    } finally {
      setDeletingId(null);
    }
  }

  if (!token) {
    return (
      <div className="flex min-h-screen items-center justify-center bg-[#f6f7fb] px-6 text-[#111827]">
        <div className="w-full max-w-md rounded-2xl border border-black/5 bg-white p-6">
          <h1 className="text-xl font-semibold">Bac lai suat tiet kiem</h1>
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
      title="Bac lai suat tiet kiem"
      subtitle="Cau hinh lai suat theo so tien gui"
      actions={
        <button
          className="h-10 rounded-lg bg-emerald-600 px-4 text-sm font-semibold text-white shadow-sm transition hover:bg-emerald-700"
          onClick={openCreate}
          type="button"
          disabled={!selectedProductId}
        >
          Them bac lai suat
        </button>
      }
    >
      <section className="rounded-2xl border border-black/5 bg-white p-6 shadow-sm">
        <div className="flex flex-wrap items-center justify-between gap-3">
          <div>
            <div className="text-sm font-semibold">Chon san pham</div>
            <div className="text-xs text-zinc-500">Ap dung bac lai cho san pham tiet kiem</div>
          </div>
          <select
            className="h-10 min-w-[240px] rounded-lg border border-black/10 bg-white px-3 text-sm"
            value={selectedProductId ?? ""}
            onChange={(e) => setSelectedProductId(Number(e.target.value))}
          >
            {products.map((item) => (
              <option key={item.id} value={item.id}>
                {item.name} ({item.code})
              </option>
            ))}
          </select>
        </div>

        {error ? (
          <div className="mt-4 rounded-lg border border-red-100 bg-red-50 p-3 text-sm text-red-700">
            {error}
          </div>
        ) : null}

        <div className="mt-4 overflow-hidden rounded-xl border border-black/5">
          <table className="w-full text-left text-sm">
            <thead className="bg-zinc-50 text-xs text-zinc-500">
              <tr>
                <th className="px-4 py-3">So tien tu</th>
                <th className="px-4 py-3">So tien den</th>
                <th className="px-4 py-3">Lai suat</th>
                <th className="px-4 py-3">Hieu luc tu</th>
                <th className="px-4 py-3">Hieu luc den</th>
                <th className="px-4 py-3">Thao tac</th>
              </tr>
            </thead>
            <tbody>
              {loading ? (
                <tr>
                  <td className="px-4 py-6 text-center text-zinc-400" colSpan={6}>
                    Dang tai...
                  </td>
                </tr>
              ) : tiers.length === 0 ? (
                <tr>
                  <td className="px-4 py-6 text-center text-zinc-400" colSpan={6}>
                    Chua co bac lai suat.
                  </td>
                </tr>
              ) : (
                tiers.map((tier) => {
                  const active = isActiveTier(tier);
                  return (
                    <tr key={tier.id} className="border-t border-black/5">
                      <td className="px-4 py-3">{formatCurrency(tier.minAmount)} VND</td>
                      <td className="px-4 py-3">
                        {tier.maxAmount ? `${formatCurrency(tier.maxAmount)} VND` : "Khong gioi han"}
                      </td>
                      <td className="px-4 py-3">
                        <span className="rounded-full bg-emerald-50 px-2 py-1 text-xs text-emerald-700">
                          {formatPercent(tier.interestRate)}
                        </span>
                      </td>
                      <td className="px-4 py-3">{toDateInput(tier.effectiveFrom)}</td>
                      <td className="px-4 py-3">
                        {tier.effectiveTo ? (
                          toDateInput(tier.effectiveTo)
                        ) : (
                          <span className="rounded-full bg-emerald-50 px-2 py-1 text-xs text-emerald-700">
                            Dang ap dung
                          </span>
                        )}
                        {!tier.effectiveTo && !active ? (
                          <span className="ml-2 rounded-full bg-rose-50 px-2 py-1 text-xs text-rose-700">
                            Het hieu luc
                          </span>
                        ) : null}
                      </td>
                      <td className="px-4 py-3">
                        <div className="flex items-center gap-3">
                          <button
                            className="text-sm font-medium text-blue-600 hover:text-blue-700"
                            type="button"
                            onClick={() => openEdit(tier)}
                          >
                            Sua
                          </button>
                          <button
                            className="text-sm font-medium text-rose-600 hover:text-rose-700 disabled:opacity-60"
                            type="button"
                            onClick={() => deleteTier(tier.id)}
                            disabled={deletingId === tier.id}
                          >
                            {deletingId === tier.id ? "Dang xoa..." : "Xoa"}
                          </button>
                        </div>
                      </td>
                    </tr>
                  );
                })
              )}
            </tbody>
          </table>
        </div>

        <div className="mt-3 rounded-lg border border-blue-100 bg-blue-50 px-4 py-3 text-xs text-blue-700">
          Luu y: Khong sua truc tiep bac lai suat da ap dung. Tao bac moi va chon ngay
          hieu luc neu muon thay doi lai suat.
        </div>
      </section>

      {modalOpen ? (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40 px-4">
          <div className="w-full max-w-2xl rounded-2xl bg-white p-6 shadow-xl">
            <div className="flex items-start justify-between">
              <div>
                <div className="text-lg font-semibold">
                  {editing ? "Cap nhat bac lai suat" : "Them bac lai suat"}
                </div>
                <div className="text-xs text-zinc-500">Cau hinh muc lai suat theo so tien</div>
              </div>
              <button
                className="text-zinc-400"
                type="button"
                onClick={() => setModalOpen(false)}
              >
                ✕
              </button>
            </div>

            <form className="mt-5 grid gap-4 md:grid-cols-2" onSubmit={saveTier}>
              <label className="text-sm">
                So tien tu (VND)
                <input
                  className="mt-1 h-10 w-full rounded-lg border border-black/10 px-3 outline-none"
                  value={form.minAmount}
                  onChange={(e) => updateForm("minAmount", e.target.value)}
                  required
                />
              </label>

              <label className="text-sm">
                So tien den (VND)
                <input
                  className="mt-1 h-10 w-full rounded-lg border border-black/10 px-3 outline-none"
                  value={form.maxAmount}
                  onChange={(e) => updateForm("maxAmount", e.target.value)}
                />
              </label>

              <label className="text-sm">
                Lai suat (%/nam)
                <input
                  className="mt-1 h-10 w-full rounded-lg border border-black/10 px-3 outline-none"
                  value={form.interestRate}
                  onChange={(e) => updateForm("interestRate", e.target.value)}
                  required
                />
              </label>

              <label className="text-sm">
                Hieu luc tu
                <input
                  className="mt-1 h-10 w-full rounded-lg border border-black/10 px-3 outline-none"
                  type="date"
                  value={form.effectiveFrom}
                  onChange={(e) => updateForm("effectiveFrom", e.target.value)}
                />
              </label>

              <label className="text-sm">
                Hieu luc den
                <input
                  className="mt-1 h-10 w-full rounded-lg border border-black/10 px-3 outline-none"
                  type="date"
                  value={form.effectiveTo}
                  onChange={(e) => updateForm("effectiveTo", e.target.value)}
                />
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
                  className="h-10 rounded-lg bg-emerald-600 px-4 text-sm font-semibold text-white shadow-sm disabled:opacity-60"
                  type="submit"
                  disabled={saving}
                >
                  {saving ? "Dang luu..." : "Luu bac"}
                </button>
              </div>
            </form>
          </div>
        </div>
      ) : null}
    </AdminShell>
  );
}
