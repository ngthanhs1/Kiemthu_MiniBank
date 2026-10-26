const BASE_URL = (process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8080").replace(/\/+$/, "");

function getAdminHeaders(): HeadersInit {
  if (typeof window === "undefined") return { "Content-Type": "application/json" };
  const token = localStorage.getItem("adminToken");
  return {
    "Content-Type": "application/json",
    ...(token ? { Authorization: `Bearer ${token}` } : {}),
  };
}

async function requestJson<T>(url: string, init?: RequestInit): Promise<T> {
  const res = await fetch(`${BASE_URL}${url}`, {
    ...init,
    headers: {
      ...getAdminHeaders(),
      ...(init?.headers || {}),
    },
  });
  if (!res.ok) {
    const text = await res.text().catch(() => "");
    throw new Error(text || "Không thể tải dữ liệu từ máy chủ");
  }
  if (res.status === 204) return undefined as T;
  return res.json() as Promise<T>;
}

export interface ApprovalPolicy {
  id: number;
  serviceType: string;
  minAmount: string;
  maxAmount: string | null;
  staffApprovalsRequired: number;
  managerApprovalsRequired: number;
  active: boolean;
  description: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface ApprovalPolicyPayload {
  serviceType: string;
  minAmount: string;
  maxAmount: string | null;
  staffApprovalsRequired: number;
  managerApprovalsRequired: number;
  active: boolean;
  description: string;
}

export function listApprovalPolicies() {
  return requestJson<ApprovalPolicy[]>("/api/admin/system/approval-policies");
}

export function createApprovalPolicy(payload: ApprovalPolicyPayload) {
  return requestJson<ApprovalPolicy>("/api/admin/system/approval-policies", {
    method: "POST",
    body: JSON.stringify(payload),
  });
}

export function updateApprovalPolicy(id: number, payload: ApprovalPolicyPayload) {
  return requestJson<ApprovalPolicy>(`/api/admin/system/approval-policies/${id}`, {
    method: "PUT",
    body: JSON.stringify(payload),
  });
}

export function deleteApprovalPolicy(id: number) {
  return requestJson<void>(`/api/admin/system/approval-policies/${id}`, { method: "DELETE" });
}
