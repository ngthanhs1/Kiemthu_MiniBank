"use client";

import { useCallback, useEffect, useMemo, useState } from "react";
import AdminShell from "../../components/admin-shell";

const API_BASE = (
  process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8080"
).replace(/\/+$/, "");

type ClassificationItem = {
  transactionId: number;
  transactionCode: string;
  createdAt: string;
  customerName: string | null;
  accountNumber: string | null;
  description: string | null;
  amount: number;
  categoryCode: string;
  categoryName: string;
  confidence: number;
  source: string;
  verificationStatus: "verified" | "pending";
};

type AiSettings = {
  classificationEnabled: boolean;
  classificationFrequencyMinutes: number;
  classificationStartTime: string | null;
  recommendationEnabled: boolean;
  recommendationFrequencyMinutes: number;
  recommendationStartTime: string | null;
  lastClassificationRun: string | null;
  lastRecommendationRun: string | null;
};

function formatCurrency(value: number) {
  return value
    .toFixed(0)
    .replace(/\B(?=(\d{3})+(?!\d))/g, ".");
}

function formatDate(value: string) {
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return value;
  return `${String(date.getDate()).padStart(2, "0")}/${String(
    date.getMonth() + 1
  ).padStart(2, "0")}/${date.getFullYear()} ${String(
    date.getHours()
  ).padStart(2, "0")}:${String(date.getMinutes()).padStart(2, "0")}`;
}

export default function TransactionClassificationPage() {
  const [items, setItems] = useState<ClassificationItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [searchQuery, setSearchQuery] = useState("");
  const [categoryFilter, setCategoryFilter] = useState("ALL");
  const [settings, setSettings] = useState<AiSettings | null>(null);
  const [settingsLoading, setSettingsLoading] = useState(false);
  const [settingsSaving, setSettingsSaving] = useState(false);
  const [settingsError, setSettingsError] = useState<string | null>(null);
  const [actionMessage, setActionMessage] = useState<string | null>(null);

  const fetchClassifications = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const token = localStorage.getItem("adminToken");
      if (!token) {
        window.location.href = "/login";
        return;
      }

      const url = new URL(`${API_BASE}/api/admin/transactions/classifications`);
      if (searchQuery) url.searchParams.append("q", searchQuery);
      if (categoryFilter !== "ALL") {
        url.searchParams.append("category", categoryFilter);
      }

      const res = await fetch(url.toString(), {
        headers: {
          Authorization: `Bearer ${token}`,
          "Content-Type": "application/json",
        },
      });

      if (res.ok) {
        const data = (await res.json()) as ClassificationItem[];
        setItems(data);
      } else {
        const message = await res.text();
        setError(message || "Failed to load classification data.");
      }
    } catch (err) {
      const message = err instanceof Error ? err.message : "Failed to fetch data.";
      setError(message);
    } finally {
      setLoading(false);
    }
  }, [searchQuery, categoryFilter]);

  const fetchSettings = useCallback(async () => {
    setSettingsLoading(true);
    setSettingsError(null);
    try {
      const token = localStorage.getItem("adminToken");
      if (!token) {
        window.location.href = "/login";
        return;
      }

      const res = await fetch(`${API_BASE}/api/admin/ai/settings`, {
        headers: {
          Authorization: `Bearer ${token}`,
          "Content-Type": "application/json",
        },
      });

      if (res.ok) {
        const data = (await res.json()) as AiSettings;
        setSettings(data);
      } else {
        const message = await res.text();
        setSettingsError(message || "Failed to load AI settings.");
      }
    } catch (err) {
      const message = err instanceof Error ? err.message : "Failed to load AI settings.";
      setSettingsError(message);
    } finally {
      setSettingsLoading(false);
    }
  }, []);

  const saveSettings = useCallback(async () => {
    if (!settings) return;
    setSettingsSaving(true);
    setSettingsError(null);
    try {
      const token = localStorage.getItem("adminToken");
      if (!token) {
        window.location.href = "/login";
        return;
      }

      const res = await fetch(`${API_BASE}/api/admin/ai/settings`, {
        method: "PUT",
        headers: {
          Authorization: `Bearer ${token}`,
          "Content-Type": "application/json",
        },
        body: JSON.stringify(settings),
      });

      if (res.ok) {
        const data = (await res.json()) as AiSettings;
        setSettings(data);
        setActionMessage("AI settings saved.");
      } else {
        const message = await res.text();
        setSettingsError(message || "Failed to save AI settings.");
      }
    } catch (err) {
      const message = err instanceof Error ? err.message : "Failed to save AI settings.";
      setSettingsError(message);
    } finally {
      setSettingsSaving(false);
    }
  }, [settings]);

  const runAction = useCallback(async (endpoint: string, successMsg: string) => {
    setActionMessage(null);
    try {
      const token = localStorage.getItem("adminToken");
      if (!token) {
        window.location.href = "/login";
        return;
      }
      const res = await fetch(`${API_BASE}/api/admin/ai/${endpoint}`, {
        method: "POST",
        headers: {
          Authorization: `Bearer ${token}`,
          "Content-Type": "application/json",
        },
      });
      if (res.ok) {
        setActionMessage(successMsg);
        fetchSettings();
      } else {
        const message = await res.text();
        setActionMessage(message || "Action failed.");
      }
    } catch (err) {
      const message = err instanceof Error ? err.message : "Action failed.";
      setActionMessage(message);
    }
  }, [fetchSettings]);

  useEffect(() => {
    const timer = setTimeout(() => fetchClassifications(), 300);
    return () => clearTimeout(timer);
  }, [fetchClassifications]);

  useEffect(() => {
    fetchSettings();
  }, [fetchSettings]);

  const categoryOptions = useMemo(() => {
    const unique = new Map<string, string>();
    items.forEach((item) => {
      unique.set(item.categoryCode, item.categoryName);
    });
    return Array.from(unique.entries()).map(([code, name]) => ({ code, name }));
  }, [items]);

  const totalClassified = items.length;
  const averageConfidence = useMemo(() => {
    if (!items.length) return 0;
    const sum = items.reduce((acc, item) => acc + item.confidence, 0);
    return Math.round((sum / items.length) * 100);
  }, [items]);
  const verifiedCount = items.filter((item) => item.verificationStatus === "verified")
    .length;
  const categoryCount = new Set(items.map((item) => item.categoryCode)).size;

  const formatRunTime = (value: string | null) => {
    if (!value) return "-";
    const date = new Date(value);
    if (Number.isNaN(date.getTime())) return value;
    return `${String(date.getDate()).padStart(2, "0")}/${String(
      date.getMonth() + 1
    ).padStart(2, "0")}/${date.getFullYear()} ${String(
      date.getHours()
    ).padStart(2, "0")}:${String(date.getMinutes()).padStart(2, "0")}`;
  };

  return (
    <AdminShell
      title="Phan loai giao dich AI"
      subtitle="Xem va quan ly giao dich duoc phan loai tu dong."
      actions={
        <div className="flex items-center gap-3">
          <div className="relative">
            <input
              type="text"
              placeholder="Tim theo mo ta, ten KH, danh muc..."
              className="h-10 w-80 rounded-xl border border-black/10 bg-white px-4 text-sm outline-none transition focus:border-blue-400 focus:ring-4 focus:ring-blue-50"
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
            />
          </div>
          <select
            className="h-10 rounded-xl border border-black/10 bg-white px-3 text-sm"
            value={categoryFilter}
            onChange={(e) => setCategoryFilter(e.target.value)}
          >
            <option value="ALL">Tat ca danh muc</option>
            {categoryOptions.map((option) => (
              <option key={option.code} value={option.code}>
                {option.name}
              </option>
            ))}
          </select>
        </div>
      }
    >
      <div className="rounded-2xl border border-black/5 bg-white p-5">
        <div className="grid gap-4 md:grid-cols-4">
          <div>
            <div className="text-xs text-zinc-500">Tong so GD phan loai</div>
            <div className="text-xl font-bold text-zinc-900">{totalClassified}</div>
          </div>
          <div>
            <div className="text-xs text-zinc-500">Do tin cay trung binh</div>
            <div className="text-xl font-bold text-indigo-600">{averageConfidence}%</div>
          </div>
          <div>
            <div className="text-xs text-zinc-500">Da xac minh</div>
            <div className="text-xl font-bold text-emerald-600">{verifiedCount}</div>
          </div>
          <div>
            <div className="text-xs text-zinc-500">Danh muc khac nhau</div>
            <div className="text-xl font-bold text-blue-600">{categoryCount}</div>
          </div>
        </div>
      </div>

      <div className="grid gap-4 lg:grid-cols-3">
        <div className="rounded-2xl border border-black/5 bg-white p-5 lg:col-span-2">
          <div className="flex items-center justify-between">
            <div>
              <div className="text-sm font-semibold text-zinc-900">AI settings</div>
              <div className="text-xs text-zinc-500">Quan ly lich phan loai va goi y.</div>
            </div>
            <button
              className="h-9 rounded-lg bg-blue-600 px-4 text-xs font-semibold text-white disabled:opacity-60"
              onClick={saveSettings}
              disabled={settingsSaving || settingsLoading || !settings}
            >
              {settingsSaving ? "Saving..." : "Save"}
            </button>
          </div>

          {settingsLoading ? (
            <div className="mt-4 text-sm text-zinc-500">Loading settings...</div>
          ) : settingsError ? (
            <div className="mt-4 text-sm text-rose-500">{settingsError}</div>
          ) : settings ? (
            <div className="mt-4 grid gap-4 md:grid-cols-2">
              <div className="space-y-3">
                <div className="flex items-center gap-2">
                  <input
                    type="checkbox"
                    checked={settings.classificationEnabled}
                    onChange={(e) =>
                      setSettings({
                        ...settings,
                        classificationEnabled: e.target.checked,
                      })
                    }
                  />
                  <span className="text-sm font-medium">Enable classification</span>
                </div>
                <label className="block text-xs text-zinc-500">Classification frequency (minutes)</label>
                <input
                  type="number"
                  className="h-10 w-full rounded-xl border border-black/10 bg-white px-3 text-sm"
                  value={settings.classificationFrequencyMinutes}
                  min={1}
                  onChange={(e) =>
                    setSettings({
                      ...settings,
                      classificationFrequencyMinutes: Number(e.target.value),
                    })
                  }
                />
                <label className="block text-xs text-zinc-500">Classification start time</label>
                <input
                  type="time"
                  className="h-10 w-full rounded-xl border border-black/10 bg-white px-3 text-sm"
                  value={settings.classificationStartTime ?? ""}
                  onChange={(e) =>
                    setSettings({
                      ...settings,
                      classificationStartTime: e.target.value,
                    })
                  }
                />
                <div className="text-xs text-zinc-500">
                  Last run: {formatRunTime(settings.lastClassificationRun)}
                </div>
              </div>

              <div className="space-y-3">
                <div className="flex items-center gap-2">
                  <input
                    type="checkbox"
                    checked={settings.recommendationEnabled}
                    onChange={(e) =>
                      setSettings({
                        ...settings,
                        recommendationEnabled: e.target.checked,
                      })
                    }
                  />
                  <span className="text-sm font-medium">Enable recommendations</span>
                </div>
                <label className="block text-xs text-zinc-500">Recommendation frequency (minutes)</label>
                <input
                  type="number"
                  className="h-10 w-full rounded-xl border border-black/10 bg-white px-3 text-sm"
                  value={settings.recommendationFrequencyMinutes}
                  min={1}
                  onChange={(e) =>
                    setSettings({
                      ...settings,
                      recommendationFrequencyMinutes: Number(e.target.value),
                    })
                  }
                />
                <label className="block text-xs text-zinc-500">Recommendation start time</label>
                <input
                  type="time"
                  className="h-10 w-full rounded-xl border border-black/10 bg-white px-3 text-sm"
                  value={settings.recommendationStartTime ?? ""}
                  onChange={(e) =>
                    setSettings({
                      ...settings,
                      recommendationStartTime: e.target.value,
                    })
                  }
                />
                <div className="text-xs text-zinc-500">
                  Last run: {formatRunTime(settings.lastRecommendationRun)}
                </div>
              </div>
            </div>
          ) : null}
        </div>

        <div className="rounded-2xl border border-black/5 bg-white p-5">
          <div className="text-sm font-semibold text-zinc-900">AI actions</div>
          <div className="mt-2 text-xs text-zinc-500">
            Chay thu cong khi can kiem tra du lieu.
          </div>
          <div className="mt-4 flex flex-col gap-2">
            <button
              className="h-9 rounded-lg border border-blue-100 bg-blue-50 text-xs font-semibold text-blue-700"
              onClick={() => runAction("run-classification", "Classification triggered.")}
            >
              Run classification now
            </button>
            <button
              className="h-9 rounded-lg border border-emerald-100 bg-emerald-50 text-xs font-semibold text-emerald-700"
              onClick={() => runAction("run-recommendations", "Recommendations triggered.")}
            >
              Run recommendations now
            </button>
            <button
              className="h-9 rounded-lg border border-zinc-200 bg-zinc-50 text-xs font-semibold text-zinc-700"
              onClick={() => runAction("train", "Training queued.")}
            >
              Train AI
            </button>
          </div>
          {actionMessage ? (
            <div className="mt-3 text-xs text-zinc-500">{actionMessage}</div>
          ) : null}
        </div>
      </div>

      <div className="overflow-hidden rounded-2xl border border-black/5 bg-white">
        <table className="w-full text-left text-sm">
          <thead className="border-b border-black/5 bg-slate-50 text-xs uppercase text-zinc-500">
            <tr>
              <th className="px-4 py-3">Thoi gian</th>
              <th className="px-4 py-3">Khach hang</th>
              <th className="px-4 py-3">Mo ta GD</th>
              <th className="px-4 py-3">So tien</th>
              <th className="px-4 py-3">Danh muc AI</th>
              <th className="px-4 py-3">Do tin cay</th>
              <th className="px-4 py-3">Trang thai</th>
            </tr>
          </thead>
          <tbody>
            {loading ? (
              <tr>
                <td className="px-4 py-6 text-center text-zinc-500" colSpan={7}>
                  Dang tai du lieu...
                </td>
              </tr>
            ) : error ? (
              <tr>
                <td className="px-4 py-6 text-center text-rose-500" colSpan={7}>
                  {error}
                </td>
              </tr>
            ) : items.length === 0 ? (
              <tr>
                <td className="px-4 py-6 text-center text-zinc-500" colSpan={7}>
                  Chua co du lieu phan loai.
                </td>
              </tr>
            ) : (
              items.map((item) => (
                <tr key={item.transactionId} className="border-b border-black/5">
                  <td className="px-4 py-4 text-xs text-zinc-500">
                    {formatDate(item.createdAt)}
                  </td>
                  <td className="px-4 py-4">
                    <div className="font-semibold text-zinc-900">
                      {item.customerName ?? "Khach hang"}
                    </div>
                    <div className="text-xs text-zinc-500">{item.accountNumber ?? ""}</div>
                  </td>
                  <td className="px-4 py-4">
                    <div className="font-medium text-zinc-900">
                      {item.description ?? item.transactionCode}
                    </div>
                  </td>
                  <td className="px-4 py-4 font-semibold text-zinc-900">
                    {formatCurrency(item.amount)} d
                  </td>
                  <td className="px-4 py-4">
                    <span className="rounded-full bg-indigo-50 px-3 py-1 text-xs font-semibold text-indigo-700">
                      {item.categoryName}
                    </span>
                  </td>
                  <td className="px-4 py-4">
                    <span className="rounded-full bg-emerald-50 px-3 py-1 text-xs font-semibold text-emerald-700">
                      {Math.round(item.confidence * 100)}%
                    </span>
                  </td>
                  <td className="px-4 py-4">
                    {item.verificationStatus === "verified" ? (
                      <span className="rounded-full bg-emerald-50 px-3 py-1 text-xs font-semibold text-emerald-700">
                        Da xac minh
                      </span>
                    ) : (
                      <span className="rounded-full bg-amber-50 px-3 py-1 text-xs font-semibold text-amber-700">
                        Cho xac minh
                      </span>
                    )}
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>
    </AdminShell>
  );
}
