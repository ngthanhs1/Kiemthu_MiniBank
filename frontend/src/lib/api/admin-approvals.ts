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

export interface SavingApprovalListItem {
  id: number;
  code: string;
  status: string;
  userFullName: string | null;
  userPhone: string | null;
  principalAmount: string;
  actualInterestRate: string;
  productName: string | null;
  termUnit: string;
  termValue: number;
  sourceAccountNumber: string | null;
  sourceAccountName: string | null;
  autoRenew: boolean;
  openDate: string | null;
  maturityDate: string | null;
  createdAt: string;
  approvalProgress?: ApprovalProgress | null;
}

export interface SavingDocumentItem {
  id: number;
  documentType: string;
  fileName: string | null;
  fileUrl: string | null;
  mimeType: string | null;
  verifiedStatus: string | null;
  uploadedAt: string | null;
  note: string | null;
}

export interface SavingApprovalDetail {
  id: number;
  code: string;
  status: string;
  principalAmount: string;
  actualInterestRate: string;
  termUnit: string;
  termValue: number;
  autoRenew: boolean;
  openDate: string | null;
  maturityDate: string | null;
  agreementAcceptedAt: string | null;
  agreementVersion: string | null;
  sourceAccountId: number | null;
  sourceAccountNumber: string | null;
  sourceAccountName: string | null;
  settlementAccountId: number | null;
  settlementAccountNumber: string | null;
  settlementAccountName: string | null;
  productId: number | null;
  productCode: string | null;
  productName: string | null;
  userId: number | null;
  userFullName: string | null;
  userPhone: string | null;
  userEmail: string | null;
  userDob: string | null;
  userAddress: string | null;
  userCitizenId: string | null;
  customerRank: string | null;
  creditScoreLevel: string | null;
  rejectionReason: string | null;
  documents: SavingDocumentItem[];
  contractId: number | null;
  contractNumber: string | null;
  contractStatus: string | null;
  approvalProgress?: ApprovalProgress | null;
}

export interface ApprovalActionItem {
  id: number;
  adminUserId: number | null;
  adminFullName: string | null;
  approverRole: string;
  action: string;
  note: string | null;
  actedAt: string | null;
}

export interface ApprovalProgress {
  instanceId: number;
  status: string;
  currentStage: string;
  staffApprovalsRequired: number;
  staffApprovedCount: number;
  managerApprovalsRequired: number;
  managerApprovedCount: number;
  finalApproved: boolean;
  rejected: boolean;
  actions: ApprovalActionItem[];
}

export interface LoanApprovalSummary {
  id: number;
  userId: number | null;
  userFullName: string | null;
  userPhone: string | null;
  userEmail: string | null;
  userDob: string | null;
  userAddress: string | null;
  userCitizenId: string | null;
  customerRank: string | null;
  creditScoreLevel: string | null;
  loanProductId: number | null;
  productCode: string | null;
  productName: string | null;
  loanType: string | null;
  requestedAmount: string;
  termMonths: number;
  monthlyIncome: string | null;
  purpose: string | null;
  collateralDescription: string | null;
  incomeProofUrl: string | null;
  collateralProofUrl: string | null;
  cccdFrontUrl: string | null;
  cccdBackUrl: string | null;
  selfieUrl: string | null;
  priorityTag: string | null;
  status: string;
  submittedAt: string;
  reviewedAt: string | null;
  reviewNote: string | null;
  hasCollateral: boolean;
  contractId: number | null;
  contractNumber: string | null;
  contractStatus: string | null;
  approvalProgress?: ApprovalProgress | null;
}

export async function listSavingsApprovals(status: string): Promise<SavingApprovalListItem[]> {
  const params = new URLSearchParams();
  if (status) params.set("status", status);
  const query = params.toString() ? `?${params.toString()}` : "";
  return requestJson<SavingApprovalListItem[]>(`/api/admin/savings${query}`);
}

export async function getSavingApproval(id: number): Promise<SavingApprovalDetail> {
  return requestJson<SavingApprovalDetail>(`/api/admin/savings/${id}`);
}

export async function approveSaving(id: number, note?: string): Promise<void> {
  await requestJson<void>(`/api/admin/savings/${id}/approve`, {
    method: "POST",
    body: JSON.stringify({ note }),
  });
}

export async function rejectSaving(id: number, reason: string): Promise<void> {
  await requestJson<void>(`/api/admin/savings/${id}/reject`, {
    method: "POST",
    body: JSON.stringify({ reason }),
  });
}

export async function listLoanApprovals(status: string): Promise<LoanApprovalSummary[]> {
  const params = new URLSearchParams();
  if (status) params.set("status", status);
  const query = params.toString() ? `?${params.toString()}` : "";
  return requestJson<LoanApprovalSummary[]>(`/api/admin/approvals/loan-applications${query}`);
}

export async function getLoanApproval(id: number): Promise<LoanApprovalSummary> {
  return requestJson<LoanApprovalSummary>(`/api/admin/approvals/loan-applications/${id}`);
}

export async function approveLoanApplication(id: number, note?: string, templateId?: number | null): Promise<void> {
  await requestJson<void>(`/api/admin/approvals/loan-applications/${id}/approve`, {
    method: "POST",
    body: JSON.stringify({ note, templateId: templateId ?? null }),
  });
}

export async function rejectLoanApplication(id: number, reason: string): Promise<void> {
  await requestJson<void>(`/api/admin/approvals/loan-applications/${id}/reject`, {
    method: "POST",
    body: JSON.stringify({ reason }),
  });
}
