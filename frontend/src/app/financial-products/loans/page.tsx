"use client";

import { useCallback, useEffect, useMemo, useState } from "react";
import { CheckCircle2, ChevronDown, Lock, PlusCircle, Search, Unlock, Landmark, X, Edit } from "lucide-react";
import AdminShell from "../../components/admin-shell";

const API_BASE = (
  process.env.NEXT_PUBLIC_API_BASE_URL ?? process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080"
).replace(/\/+$/, "");

type LoanProductItem = {
  id: number;
  code: string;
  name: string;
  loanType: string;
  currency: string;
  minAmount: number;
  maxAmount: number;
  minTermMonths: number;
  maxTermMonths: number;
  baseInterestRate: number;
  penaltyRate?: number;
  processingFee?: number;
  earlyRepaymentFee?: number;
  interestCalculationMethod?: string;
  repaymentFrequency?: string;
  status: string;
};

type ProductFormState = {
  code: string;
  name: string;
  loanType: string;
  minAmount: string;
  maxAmount: string;
  minTermMonths: string;
  maxTermMonths: string;
  baseInterestRate: string;
  penaltyRate: string;
  processingFee: string;
  earlyRepaymentFee: string;
  interestCalculationMethod: string;
  repaymentFrequency: string;
};

const defaultForm: ProductFormState = {
  code: "",
  name: "",
  loanType: "personal",
  minAmount: "",
  maxAmount: "",
  minTermMonths: "",
  maxTermMonths: "",
  baseInterestRate: "",
  penaltyRate: "",
  processingFee: "",
  earlyRepaymentFee: "",
  interestCalculationMethod: "reducing",
  repaymentFrequency: "monthly",
};

const statusOptions = [
  { value: "ALL", label: "Tất cả trạng thái" },
  { value: "active", label: "Đang hoạt động" },
  { value: "inactive", label: "Tạm ngưng" },
];

function formatVND(value: number) {
  return new Intl.NumberFormat("vi-VN", {
    style: "currency",
    currency: "VND",
    maximumFractionDigits: 0,
  }).format(value ?? 0);
}

function getLoanTypeLabel(type: string) {
  switch ((type ?? "").toLowerCase()) {
    case "personal": return "Tiêu dùng cá nhân";
    case "business": return "Kinh doanh";
    case "mortgage": return "Mua nhà";
    case "auto": return "Mua xe";
    case "unsecured": return "Tín chấp";
    case "secured": return "Thế chấp";
    default: return type || "Chưa phân loại";
  }
}

function getCalcMethodLabel(method: string) {
  switch ((method ?? "").toLowerCase()) {
    case "reducing": return "Lãi giảm dần";
    case "flat": return "Lãi cố định";
    default: return method || "-";
  }
}

export default function LoanProductsPage() {
  const [token, setToken] = useState<string | null>(null);
  const [products, setProducts] = useState<LoanProductItem[]>([]);
  const [searchQuery, setSearchQuery] = useState("");
  const [statusFilter, setStatusFilter] = useState("ALL");
  const [isDropdownOpen, setIsDropdownOpen] = useState(false);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [modalOpen, setModalOpen] = useState(false);
  const [editing, setEditing] = useState<LoanProductItem | null>(null);
  const [form, setForm] = useState<ProductFormState>(defaultForm);
  const [formSubmitting, setFormSubmitting] = useState(false);
  const [statusUpdatingId, setStatusUpdatingId] = useState<number | null>(null);

  useEffect(() => {
    setToken(localStorage.getItem("adminToken"));
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
      const res = await fetch(`${API_BASE}/api/admin/financial-products/loan-products`, {
        headers: { "Content-Type": "application/json", ...authHeader },
      });
      if (!res.ok) throw new Error((await res.text()) || "Không thể tải danh sách sản phẩm vay");
      setProducts((await res.json()) as LoanProductItem[]);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Có lỗi khi kết nối API");
    } finally {
      setLoading(false);
    }
  }, [authHeader, token]);

  useEffect(() => {
    if (token) fetchProducts();
  }, [fetchProducts, token]);

  const filteredProducts = useMemo(() => {
    const q = searchQuery.trim().toLowerCase();
    return products.filter(
      (p) =>
        (!q ||
          p.name.toLowerCase().includes(q) ||
          p.code.toLowerCase().includes(q) ||
          getLoanTypeLabel(p.loanType).toLowerCase().includes(q)) &&
        (statusFilter === "ALL" || p.status.toLowerCase() === statusFilter.toLowerCase())
    );
  }, [products, searchQuery, statusFilter]);

  function openCreate() {
    setEditing(null);
    setForm(defaultForm);
    setModalOpen(true);
  }

  function openEdit(item: LoanProductItem) {
    setEditing(item);
    setForm({
      code: item.code ?? "",
      name: item.name ?? "",
      loanType: item.loanType ?? "personal",
      minAmount: String(item.minAmount ?? ""),
      maxAmount: String(item.maxAmount ?? ""),
      minTermMonths: String(item.minTermMonths ?? ""),
      maxTermMonths: String(item.maxTermMonths ?? ""),
      baseInterestRate: String(item.baseInterestRate ?? ""),
      penaltyRate: String(item.penaltyRate ?? ""),
      processingFee: String(item.processingFee ?? ""),
      earlyRepaymentFee: String(item.earlyRepaymentFee ?? ""),
      interestCalculationMethod: item.interestCalculationMethod ?? "reducing",
      repaymentFrequency: item.repaymentFrequency ?? "monthly",
    });
    setModalOpen(true);
  }

  function updateForm<K extends keyof ProductFormState>(key: K, value: ProductFormState[K]) {
    setForm((prev) => ({ ...prev, [key]: value }));
  }

  async function saveProduct(e: React.FormEvent) {
    e.preventDefault();
    if (!token) return;
    setFormSubmitting(true);
    setError(null);
    const payload = {
      code: form.code.trim().toUpperCase(),
      name: form.name.trim(),
      loanType: form.loanType,
      currency: "VND",
      minAmount: Number(form.minAmount),
      maxAmount: Number(form.maxAmount),
      minTermMonths: Number(form.minTermMonths),
      maxTermMonths: Number(form.maxTermMonths),
      interestRateType: "FIXED",
      baseInterestRate: Number(form.baseInterestRate),
      penaltyRate: form.penaltyRate ? Number(form.penaltyRate) : undefined,
      processingFee: form.processingFee ? Number(form.processingFee) : undefined,
      earlyRepaymentFee: form.earlyRepaymentFee ? Number(form.earlyRepaymentFee) : undefined,
      interestCalculationMethod: form.interestCalculationMethod,
      repaymentFrequency: form.repaymentFrequency,
      status: "active",
    };
    try {
      const url = editing
        ? `${API_BASE}/api/admin/financial-products/loan-products/${editing.id}`
        : `${API_BASE}/api/admin/financial-products/loan-products`;
      const method = editing ? "PUT" : "POST";
      const res = await fetch(url, {
        method,
        headers: { "Content-Type": "application/json", ...authHeader },
        body: JSON.stringify(payload),
      });
      if (!res.ok) throw new Error((await res.text()) || "Không thể lưu sản phẩm");
      await fetchProducts();
      setModalOpen(false);
      setForm(defaultForm);
      setEditing(null);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Lưu thất bại");
    } finally {
      setFormSubmitting(false);
    }
  }

  async function toggleStatus(item: LoanProductItem) {
    if (!token) return;
    const nextStatus = item.status?.toLowerCase() === "active" ? "inactive" : "active";
    setStatusUpdatingId(item.id);
    setError(null);
    try {
      const res = await fetch(
        `${API_BASE}/api/admin/financial-products/loan-products/${item.id}/status`,
        {
          method: "PATCH",
          headers: { "Content-Type": "application/json", ...authHeader },
          body: JSON.stringify({ status: nextStatus }),
        }
      );
      if (!res.ok) throw new Error((await res.text()) || "Cập nhật thất bại");
      await fetchProducts();
    } catch (err) {
      setError(err instanceof Error ? err.message : "Cập nhật thất bại");
    } finally {
      setStatusUpdatingId(null);
    }
  }

  if (!token) {
    return (
      <div className="flex min-h-screen items-center justify-center bg-neutral-50 px-6">
        <div className="w-full max-w-md rounded-2xl border border-black/5 bg-white p-6 shadow-sm">
          <h1 className="text-xl font-semibold text-neutral-900">Sản phẩm vay</h1>
          <p className="mt-2 text-sm text-neutral-500">Vui lòng đăng nhập để tiếp tục.</p>
          <a
            className="mt-4 inline-flex h-10 items-center justify-center rounded-lg bg-orange-600 px-4 text-sm font-semibold text-white"
            href="/login"
          >
            Đi tới đăng nhập
          </a>
        </div>
      </div>
    );
  }

  return (
    <AdminShell
      title="Sản phẩm vay"
      subtitle="Quản lý các sản phẩm vay của ngân hàng"
      actions={
        <button
          onClick={openCreate}
          type="button"
          className="flex items-center gap-2 rounded-lg bg-orange-600 px-4 py-2 text-sm font-semibold text-white shadow-sm transition hover:bg-orange-700"
        >
          <PlusCircle size={16} />
          Tạo sản phẩm mới
        </button>
      }
    >
      {/* Header icon row */}
      <div className="mb-2 flex items-center gap-2">
        <Landmark size={22} className="text-orange-600" />
        <span className="text-base font-semibold text-neutral-800">Sản phẩm vay</span>
      </div>

      {/* Filter bar */}
      <div className="mb-6 flex flex-col items-end justify-between gap-3 rounded-xl border border-neutral-200 bg-white p-4 shadow-sm sm:flex-row sm:items-center">
        <div className="relative w-full sm:w-2/3">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 text-neutral-400" size={16} />
          <input
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            placeholder="Tìm theo mã, tên, loại vay..."
            className="w-full rounded-lg border border-neutral-200 bg-neutral-50 py-2 pl-9 pr-4 text-sm outline-none transition focus:border-orange-400 focus:ring-2 focus:ring-orange-100"
          />
        </div>
        <div className="flex w-full items-center gap-2 sm:w-auto">
          <div className="relative w-full sm:w-52">
            <button
              onClick={() => setIsDropdownOpen(!isDropdownOpen)}
              className="flex w-full items-center justify-between rounded-lg border border-neutral-200 bg-white px-3 py-2 text-left text-sm text-neutral-700 transition hover:border-neutral-300"
            >
              <span>{statusOptions.find((o) => o.value === statusFilter)?.label}</span>
              <ChevronDown size={16} />
            </button>
            {isDropdownOpen && (
              <div className="absolute right-0 top-full z-50 mt-1 w-full rounded-lg border border-neutral-200 bg-white py-1 shadow-xl">
                {statusOptions.map((o) => (
                  <button
                    key={o.value}
                    onClick={() => { setStatusFilter(o.value); setIsDropdownOpen(false); }}
                    className="w-full px-4 py-2 text-left text-sm text-neutral-700 hover:bg-neutral-50"
                  >
                    {o.label}
                  </button>
                ))}
              </div>
            )}
          </div>
          <button
            onClick={fetchProducts}
            type="button"
            className="h-9 rounded-lg border border-neutral-200 px-3 text-sm text-neutral-600 transition hover:bg-neutral-50"
          >
            Làm mới
          </button>
        </div>
      </div>

      {error && (
        <div className="mb-4 rounded-lg border border-red-100 bg-red-50 p-3 text-sm text-red-700">
          {error}
        </div>
      )}

      {/* Cards grid */}
      <div className="grid grid-cols-1 gap-6 md:grid-cols-2 lg:grid-cols-3">
        {loading ? (
          <div className="col-span-full rounded-xl border border-dashed border-neutral-200 p-10 text-center text-sm text-neutral-400">
            Đang tải dữ liệu...
          </div>
        ) : filteredProducts.length === 0 ? (
          <div className="col-span-full rounded-xl border border-dashed border-neutral-200 p-10 text-center text-sm text-neutral-400">
            Chưa có sản phẩm vay nào.
          </div>
        ) : (
          filteredProducts.map((product) => (
            <div
              key={product.id}
              className="overflow-hidden rounded-xl border border-neutral-200 bg-white shadow-sm transition hover:shadow-md"
            >
              {/* Card header */}
              <div className="bg-gradient-to-r from-orange-500 to-orange-600 px-4 py-4 text-white">
                <div className="flex items-start justify-between">
                  <div>
                    <p className="text-xs text-orange-100">Mã sản phẩm</p>
                    <h3 className="text-xl font-semibold tracking-wide">{product.code}</h3>
                  </div>
                  <span
                    className={`rounded px-2 py-1 text-xs font-medium ${
                      product.status?.toLowerCase() === "active"
                        ? "bg-green-500 text-white"
                        : "bg-neutral-400 text-white"
                    }`}
                  >
                    {product.status?.toLowerCase() === "active" ? "Hoạt động" : "Tạm khóa"}
                  </span>
                </div>
                <p className="mt-1 text-sm font-medium text-orange-50">{product.name}</p>
              </div>

              {/* Card body */}
              <div className="space-y-2.5 p-4 text-sm">
                <Row label="Loại vay" value={getLoanTypeLabel(product.loanType)} />
                <Row
                  label="Hạn mức"
                  value={`${formatVND(product.minAmount)} - ${formatVND(product.maxAmount)}`}
                  valueClass="font-medium text-neutral-900"
                />
                <Row
                  label="Kỳ hạn"
                  value={`${product.minTermMonths} - ${product.maxTermMonths} tháng`}
                />
                <Row
                  label="Lãi suất cơ bản"
                  value={`${product.baseInterestRate}%/năm`}
                  valueClass="text-lg font-semibold text-orange-600"
                />
                {product.penaltyRate != null && (
                  <Row
                    label="Lãi phạt"
                    value={`${product.penaltyRate}%/tháng`}
                    valueClass="font-medium text-red-600"
                  />
                )}
                {product.processingFee != null && (
                  <Row label="Phí xử lý" value={`${product.processingFee}%`} />
                )}
                {product.earlyRepaymentFee != null && (
                  <Row label="Phí trả nợ sớm" value={`${product.earlyRepaymentFee}%`} />
                )}
                {product.interestCalculationMethod && (
                  <Row
                    label="Cách tính lãi"
                    value={getCalcMethodLabel(product.interestCalculationMethod)}
                    valueClass="font-medium text-neutral-900"
                  />
                )}
              </div>

              {/* Card actions */}
              <div className="flex gap-2 border-t border-neutral-100 px-4 py-3">
                <button
                  onClick={() => openEdit(product)}
                  className="flex flex-1 items-center justify-center gap-1.5 rounded-lg bg-orange-50 py-2 text-sm text-orange-600 transition hover:bg-orange-100"
                >
                  <Edit size={14} />
                  Sửa
                </button>
                <button
                  onClick={() => toggleStatus(product)}
                  disabled={statusUpdatingId === product.id}
                  className={`flex flex-1 items-center justify-center gap-1.5 rounded-lg py-2 text-sm transition disabled:opacity-60 ${
                    product.status?.toLowerCase() === "active"
                      ? "bg-red-50 text-red-600 hover:bg-red-100"
                      : "bg-green-50 text-green-600 hover:bg-green-100"
                  }`}
                >
                  {statusUpdatingId === product.id ? (
                    "Đang xử lý..."
                  ) : product.status?.toLowerCase() === "active" ? (
                    <><Lock size={14} /> Khóa</>
                  ) : (
                    <><Unlock size={14} /> Mở</>
                  )}
                </button>
              </div>
            </div>
          ))
        )}
      </div>

      {/* Modal Create / Edit */}
      {modalOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 px-4">
          <div className="flex max-h-[90vh] w-full max-w-3xl flex-col overflow-hidden rounded-xl bg-white shadow-2xl">
            {/* Modal header */}
            <div className="flex items-center justify-between border-b border-neutral-200 px-6 py-4">
              <div>
                <h2 className="font-semibold text-neutral-900">
                  {editing ? "Sửa sản phẩm vay" : "Tạo sản phẩm vay mới"}
                </h2>
                <p className="text-xs text-neutral-500">
                  {editing ? "Cập nhật thông tin sản phẩm vay" : "Điền thông tin để tạo sản phẩm mới"}
                </p>
              </div>
              <button
                onClick={() => { setModalOpen(false); setEditing(null); }}
                className="rounded-lg p-1 text-neutral-400 transition hover:bg-neutral-100 hover:text-neutral-600"
              >
                <X size={20} />
              </button>
            </div>

            {/* Modal form */}
            <form onSubmit={saveProduct} className="flex-1 overflow-auto">
              <div className="grid grid-cols-1 gap-4 p-6 sm:grid-cols-2">
                <FormField label="Mã sản phẩm">
                  <input
                    required
                    placeholder="VD: VAYCN"
                    value={form.code}
                    onChange={(e) => updateForm("code", e.target.value)}
                    className="input-field"
                  />
                </FormField>

                <FormField label="Tên sản phẩm">
                  <input
                    required
                    placeholder="VD: Vay tiêu dùng cá nhân"
                    value={form.name}
                    onChange={(e) => updateForm("name", e.target.value)}
                    className="input-field"
                  />
                </FormField>

                <FormField label="Loại vay">
                  <select
                    value={form.loanType}
                    onChange={(e) => updateForm("loanType", e.target.value)}
                    className="input-field"
                  >
                    <option value="personal">Tiêu dùng cá nhân</option>
                    <option value="business">Kinh doanh</option>
                    <option value="mortgage">Mua nhà</option>
                    <option value="auto">Mua xe</option>
                    <option value="UNSECURED">Tín chấp</option>
                    <option value="SECURED">Thế chấp</option>
                  </select>
                </FormField>

                <FormField label="Lãi suất cơ bản (%/năm)">
                  <input
                    required
                    type="number"
                    step="0.1"
                    placeholder="VD: 12.5"
                    value={form.baseInterestRate}
                    onChange={(e) => updateForm("baseInterestRate", e.target.value)}
                    className="input-field"
                  />
                </FormField>

                <FormField label="Hạn mức tối thiểu (VND)">
                  <input
                    required
                    type="number"
                    placeholder="VD: 10000000"
                    value={form.minAmount}
                    onChange={(e) => updateForm("minAmount", e.target.value)}
                    className="input-field"
                  />
                </FormField>

                <FormField label="Hạn mức tối đa (VND)">
                  <input
                    required
                    type="number"
                    placeholder="VD: 500000000"
                    value={form.maxAmount}
                    onChange={(e) => updateForm("maxAmount", e.target.value)}
                    className="input-field"
                  />
                </FormField>

                <FormField label="Kỳ hạn tối thiểu (tháng)">
                  <input
                    required
                    type="number"
                    min={1}
                    placeholder="VD: 6"
                    value={form.minTermMonths}
                    onChange={(e) => updateForm("minTermMonths", e.target.value)}
                    className="input-field"
                  />
                </FormField>

                <FormField label="Kỳ hạn tối đa (tháng)">
                  <input
                    required
                    type="number"
                    min={1}
                    placeholder="VD: 60"
                    value={form.maxTermMonths}
                    onChange={(e) => updateForm("maxTermMonths", e.target.value)}
                    className="input-field"
                  />
                </FormField>

                <FormField label="Lãi phạt (%/tháng)">
                  <input
                    type="number"
                    step="0.1"
                    placeholder="VD: 2.0"
                    value={form.penaltyRate}
                    onChange={(e) => updateForm("penaltyRate", e.target.value)}
                    className="input-field"
                  />
                </FormField>

                <FormField label="Phí xử lý (%)">
                  <input
                    type="number"
                    step="0.1"
                    placeholder="VD: 1.0"
                    value={form.processingFee}
                    onChange={(e) => updateForm("processingFee", e.target.value)}
                    className="input-field"
                  />
                </FormField>

                <FormField label="Phí trả nợ sớm (%)">
                  <input
                    type="number"
                    step="0.1"
                    placeholder="VD: 2.0"
                    value={form.earlyRepaymentFee}
                    onChange={(e) => updateForm("earlyRepaymentFee", e.target.value)}
                    className="input-field"
                  />
                </FormField>

                <FormField label="Cách tính lãi">
                  <select
                    value={form.interestCalculationMethod}
                    onChange={(e) => updateForm("interestCalculationMethod", e.target.value)}
                    className="input-field"
                  >
                    <option value="reducing">Lãi giảm dần</option>
                    <option value="flat">Lãi cố định</option>
                  </select>
                </FormField>

                <FormField label="Tần suất trả nợ">
                  <select
                    value={form.repaymentFrequency}
                    onChange={(e) => updateForm("repaymentFrequency", e.target.value)}
                    className="input-field"
                  >
                    <option value="monthly">Hàng tháng</option>
                    <option value="quarterly">Hàng quý</option>
                  </select>
                </FormField>
              </div>

              {error && (
                <div className="mx-6 mb-4 rounded-lg border border-red-100 bg-red-50 p-3 text-sm text-red-700">
                  {error}
                </div>
              )}

              <div className="flex items-center justify-end gap-3 border-t border-neutral-200 px-6 py-4">
                <button
                  type="button"
                  onClick={() => { setModalOpen(false); setEditing(null); }}
                  className="rounded-lg border border-neutral-300 px-4 py-2 text-sm text-neutral-700 transition hover:bg-neutral-50"
                >
                  Hủy
                </button>
                <button
                  type="submit"
                  disabled={formSubmitting}
                  className="rounded-lg bg-orange-600 px-4 py-2 text-sm font-semibold text-white shadow-sm transition hover:bg-orange-700 disabled:opacity-60"
                >
                  {formSubmitting ? "Đang lưu..." : editing ? "Cập nhật" : "Tạo mới"}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      <style jsx>{`
        .input-field {
          width: 100%;
          height: 40px;
          border-radius: 8px;
          border: 1px solid #e5e7eb;
          padding: 0 12px;
          font-size: 14px;
          outline: none;
          transition: border-color 0.15s, box-shadow 0.15s;
          background: #fff;
        }
        .input-field:focus {
          border-color: #f97316;
          box-shadow: 0 0 0 3px rgba(249, 115, 22, 0.1);
        }
        select.input-field {
          padding: 0 8px;
        }
      `}</style>
    </AdminShell>
  );
}

function Row({
  label,
  value,
  valueClass = "font-medium text-neutral-700",
}: {
  label: string;
  value: string;
  valueClass?: string;
}) {
  return (
    <div className="flex items-center justify-between">
      <span className="text-neutral-500">{label}:</span>
      <span className={valueClass}>{value}</span>
    </div>
  );
}

function FormField({ label, children }: { label: string; children: React.ReactNode }) {
  return (
    <label className="flex flex-col gap-1 text-sm font-medium text-neutral-700">
      {label}
      {children}
    </label>
  );
}