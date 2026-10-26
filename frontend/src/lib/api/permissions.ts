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

export interface PermissionDefinition {
	id: number;
	code: string;
	label: string;
	tabGroup: string;
	description: string;
	sortOrder: number;
	active: boolean;
	createdAt: string;
	updatedAt: string;
}

export interface PermissionPayload {
	code: string;
	label: string;
	tabGroup: string;
	description: string;
	sortOrder: number;
	active: boolean;
}

export function listPermissionDefinitions() {
	return requestJson<PermissionDefinition[]>("/api/admin/permissions");
}

export function createPermissionDefinition(payload: PermissionPayload) {
	return requestJson<PermissionDefinition>("/api/admin/permissions", {
		method: "POST",
		body: JSON.stringify(payload),
	});
}

export function updatePermissionDefinition(code: string, payload: PermissionPayload) {
	return requestJson<PermissionDefinition>(`/api/admin/permissions/${encodeURIComponent(code)}`, {
		method: "PUT",
		body: JSON.stringify(payload),
	});
}

export function deletePermissionDefinition(code: string) {
	return requestJson<void>(`/api/admin/permissions/${encodeURIComponent(code)}`, {
		method: "DELETE",
	});
}