const BASE_URL = (process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8080").replace(/\/+$/, "");

function getAdminAuthHeader(): HeadersInit {
  if (typeof window === "undefined") return {};
  const token = localStorage.getItem("adminToken");
  return token ? { Authorization: `Bearer ${token}` } : {};
}

async function requestJson<T>(url: string, init?: RequestInit): Promise<T> {
  const res = await fetch(`${BASE_URL}${url}`, {
    ...init,
    headers: {
      "Content-Type": "application/json",
      ...getAdminAuthHeader(),
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

export type TemplateSummary = {
  id: number;
  name: string;
  code: string;
  description?: string | null;
  services?: string | null;
  status?: string | null;
  templateBody?: string | null;
  templateFileUrl?: string | null;
  placeholderCount?: number | null;
  createdAt?: string | null;
  updatedAt?: string | null;
};

export type TemplatePlaceholder = {
  id?: number | null;
  fieldCode: string;
  fieldLabel?: string | null;
  dataSource?: string | null;
  sortOrder?: number | null;
};

export type TemplateDetail = TemplateSummary & {
  templateBody?: string | null;
  placeholders?: TemplatePlaceholder[];
};

export type GeneratedContract = {
  id: number;
  ownerType: string;
  ownerId: number;
  contractNumber?: string | null;
  status?: string | null;
  fileUrl?: string | null;
  createdAt?: string | null;
};

export type ContractAcceptanceSummary = {
  agreementType: string;
  referenceType: string;
  referenceId: number;
  userId: number | null;
  userFullName: string | null;
  userPhone: string | null;
  templateId: number | null;
  templateCode: string | null;
  templateName: string | null;
  templateVersion: string | null;
  contractNumber: string | null;
  acceptanceStatus: string | null;
  acceptedAt: string | null;
};

export async function listContractTemplates(service?: string): Promise<TemplateSummary[]> {
  const query = service ? `?service=${encodeURIComponent(service)}` : "";
  return requestJson<TemplateSummary[]>(`/api/admin/contract-templates${query}`);
}

export async function getContractTemplate(id: number): Promise<TemplateDetail> {
  return requestJson<TemplateDetail>(`/api/admin/contract-templates/${id}`);
}

export async function listGeneratedContracts(): Promise<GeneratedContract[]> {
  return requestJson<GeneratedContract[]>("/api/admin/contracts");
}

export async function listContractsByOwner(ownerType: string, ownerId: number): Promise<GeneratedContract[]> {
  const query = `?ownerType=${encodeURIComponent(ownerType)}&ownerId=${encodeURIComponent(ownerId)}`;
  return requestJson<GeneratedContract[]>(`/api/admin/contracts${query}`);
}

export async function createContractTemplate(payload: {
  name: string;
  code: string;
  description?: string | null;
  services?: string | null;
  status?: string | null;
  templateBody?: string | null;
  templateFileUrl?: string | null;
  placeholders?: TemplatePlaceholder[];
}): Promise<TemplateDetail> {
  return requestJson<TemplateDetail>("/api/admin/contract-templates", {
    method: "POST",
    body: JSON.stringify(payload),
  });
}

export async function updateContractTemplate(id: number, payload: {
  name: string;
  code: string;
  description?: string | null;
  services?: string | null;
  status?: string | null;
  templateBody?: string | null;
  templateFileUrl?: string | null;
  placeholders?: TemplatePlaceholder[];
}): Promise<TemplateDetail> {
  return requestJson<TemplateDetail>(`/api/admin/contract-templates/${id}`, {
    method: "PUT",
    body: JSON.stringify(payload),
  });
}

export async function activateContractTemplate(id: number): Promise<TemplateDetail> {
  return requestJson<TemplateDetail>(`/api/admin/contract-templates/${id}/activate`, {
    method: "PATCH",
  });
}

export async function archiveContractTemplate(id: number): Promise<TemplateDetail> {
  return requestJson<TemplateDetail>(`/api/admin/contract-templates/${id}/archive`, {
    method: "PATCH",
  });
}

export async function deleteContractTemplate(id: number): Promise<void> {
  await requestJson<void>(`/api/admin/contract-templates/${id}`, {
    method: "DELETE",
  });
}

export async function generateContractDocument(payload: {
  ownerType: string;
  ownerId: number;
  templateId: number;
  contractNumber?: string | null;
}): Promise<GeneratedContract> {
  return requestJson<GeneratedContract>("/api/admin/contracts/generate", {
    method: "POST",
    body: JSON.stringify(payload),
  });
}

export async function listContractAcceptances(type: string = "all"): Promise<ContractAcceptanceSummary[]> {
  const query = type ? `?type=${encodeURIComponent(type)}` : "";
  return requestJson<ContractAcceptanceSummary[]>(`/api/admin/contracts/acceptances${query}`);
}

export async function uploadContractTemplate(payload: {
  file: File;
  name: string;
  code: string;
  description?: string | null;
  services?: string | null;
}): Promise<TemplateDetail> {
  const body = new FormData();
  body.append("file", payload.file);
  body.append("name", payload.name);
  body.append("code", payload.code);
  if (payload.description) body.append("description", payload.description);
  if (payload.services) body.append("services", payload.services);

  const res = await fetch(`${BASE_URL}/api/admin/contract-templates/upload`, {
    method: "POST",
    headers: getAdminAuthHeader(),
    body,
  });

  if (!res.ok) {
    const text = await res.text().catch(() => "");
    throw new Error(text || "Không thể tải template lên");
  }

  return res.json() as Promise<TemplateDetail>;
}
