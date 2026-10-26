"use client";

import { Plus, Save, Shield, Trash2 } from "lucide-react";
import { useEffect, useMemo, useState } from "react";

import AdminShell from "../../components/admin-shell";
import {
	createPermissionDefinition,
	deletePermissionDefinition,
	listPermissionDefinitions,
	type PermissionDefinition,
	type PermissionPayload,
	updatePermissionDefinition,
} from "../../../lib/api/permissions";

type RoleItem = {
	code: string;
	name: string;
	description: string;
	totalUsers: number;
	color: string;
	permissions: string[];
};

type RoleForm = {
	code: string;
	name: string;
	description: string;
	color: string;
	permissionCodes: string[];
};

type ViewTab = "roles" | "permissions";

const API_BASE = (process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8080").replace(/\/+$/, "");

const colorClass: Record<string, string> = {
	red: "border-red-200 bg-red-50 text-red-700",
	blue: "border-blue-200 bg-blue-50 text-blue-700",
	emerald: "border-emerald-200 bg-emerald-50 text-emerald-700",
	amber: "border-amber-200 bg-amber-50 text-amber-700",
	violet: "border-violet-200 bg-violet-50 text-violet-700",
};

const emptyRoleForm: RoleForm = {
	code: "",
	name: "",
	description: "",
	color: "blue",
	permissionCodes: [],
};

const emptyPermissionForm: PermissionPayload = {
	code: "",
	label: "",
	tabGroup: "",
	description: "",
	sortOrder: 100,
	active: true,
};

const LEGACY_PERMISSION_ALIASES: Record<string, string> = {
	"quản lý nhân viên": "STAFF_MANAGE",
	"gán vai trò": "ROLE_MANAGE",
	"khóa/mở khóa nhân viên": "STAFF_MANAGE",
	"xem dashboard": "SYSTEM_AUDIT_VIEW",
	"xem log hệ thống": "SYSTEM_AUDIT_VIEW",
	"cấu hình ai": "PERMISSION_MANAGE",
	"cấu hình sản phẩm vay và tiết kiệm": "LOAN_PRODUCT_MANAGE",
	"nhận cuộc chat chuyển tiếp": "CHAT_CONVERSATION_MANAGE",
	"trả lời khách hàng": "CHAT_CONVERSATION_MANAGE",
	"xem thông tin khách khi chat": "CHAT_CONVERSATION_MANAGE",
	"ghi chú phiên chat": "CHAT_CONVERSATION_MANAGE",
	"ưu tiên khách vip": "CHAT_CONVERSATION_MANAGE",
	"xem hồ sơ kyc": "CUSTOMER_KYC_VIEW",
	"phê duyệt kyc": "CUSTOMER_KYC_APPROVE",
	"từ chối kyc": "CUSTOMER_KYC_REJECT",
	"xem tài liệu cccd": "CUSTOMER_DOCUMENT_VIEW",
	"đổi thông tin cá nhân": "PROFILE_REQUEST_APPROVAL",
	"đổi thông tin tài khoản": "PROFILE_REQUEST_APPROVAL",
	"nâng hạn mức": "LIMIT_REQUEST_APPROVAL",
	"mở/tất toán tiết kiệm": "SAVING_APPROVAL",
	"xử lý yêu cầu dịch vụ": "SAVING_APPROVAL",
	"xem hồ sơ vay": "LOAN_APPLICATION_APPROVAL",
	"thẩm định hồ sơ": "LOAN_APPLICATION_APPROVAL",
	"duyệt/từ chối vay": "LOAN_APPLICATION_APPROVAL",
	"theo dõi nợ": "LOAN_APPLICATION_APPROVAL",
	"nhắc nợ": "LOAN_APPLICATION_APPROVAL",
};

function normalizeText(value: string) {
	return value.trim().toLowerCase().replace(/\s+/g, " ");
}

function authHeaders(): HeadersInit {
	const token = typeof window !== "undefined" ? localStorage.getItem("adminToken") : null;
	return {
		"Content-Type": "application/json",
		...(token ? { Authorization: `Bearer ${token}` } : {}),
	};
}

function resolvePermissionCode(value: string, permissions: PermissionDefinition[]) {
	const normalized = normalizeText(value);
	const exact = permissions.find(
		(permission) => normalizeText(permission.code) === normalized || normalizeText(permission.label) === normalized
	);
	if (exact) return exact.code;
	return LEGACY_PERMISSION_ALIASES[normalized] ?? value.trim().toUpperCase();
}

function resolvePermissionLabel(code: string, permissions: PermissionDefinition[]) {
	const normalized = normalizeText(code);
	const exact = permissions.find(
		(permission) => normalizeText(permission.code) === normalized || normalizeText(permission.label) === normalized
	);
	return exact?.label ?? code;
}

function toRoleForm(role: RoleItem, permissions: PermissionDefinition[]): RoleForm {
	return {
		code: role.code,
		name: role.name,
		description: role.description,
		color: role.color || "blue",
		permissionCodes: role.permissions.map((item) => resolvePermissionCode(item, permissions)),
	};
}

export default function RolesPage() {
	const [viewTab, setViewTab] = useState<ViewTab>("roles");
	const [roles, setRoles] = useState<RoleItem[]>([]);
	const [permissions, setPermissions] = useState<PermissionDefinition[]>([]);
	const [selectedCode, setSelectedCode] = useState("");
	const [roleForm, setRoleForm] = useState<RoleForm>(emptyRoleForm);
	const [isCreatingRole, setIsCreatingRole] = useState(false);
	const [selectedPermissionCode, setSelectedPermissionCode] = useState("");
	const [permissionForm, setPermissionForm] = useState<PermissionPayload>(emptyPermissionForm);
	const [isCreatingPermission, setIsCreatingPermission] = useState(false);
	const [loading, setLoading] = useState(true);
	const [saving, setSaving] = useState(false);
	const [error, setError] = useState("");
	const [toast, setToast] = useState("");

	const selectedRole = useMemo(
		() => roles.find((role) => role.code === selectedCode) ?? null,
		[roles, selectedCode]
	);

	const selectedPermission = useMemo(
		() => permissions.find((item) => item.code === selectedPermissionCode) ?? null,
		[permissions, selectedPermissionCode]
	);

	const groupedPermissions = useMemo(() => {
		return permissions.reduce<Record<string, PermissionDefinition[]>>((acc, item) => {
			const key = item.tabGroup || "Khác";
			acc[key] = [...(acc[key] ?? []), item];
			return acc;
		}, {});
	}, [permissions]);

	const sortedTabGroups = useMemo(
		() => Object.keys(groupedPermissions).sort((a, b) => a.localeCompare(b, "vi")),
		[groupedPermissions]
	);

	async function loadAll(nextRoleCode?: string, nextPermissionCode?: string) {
		await Promise.resolve();
		setLoading(true);
		setError("");
		try {
			const [permissionData, roleRes] = await Promise.all([
				listPermissionDefinitions(),
				fetch(`${API_BASE}/api/admin/roles`, { headers: authHeaders() }),
			]);

			if (!roleRes.ok) throw new Error(await roleRes.text());
			const roleData = (await roleRes.json()) as RoleItem[];
			setPermissions(permissionData);
			setRoles(roleData);

			const roleCode = nextRoleCode ?? selectedCode ?? roleData[0]?.code ?? "";
			const nextRole = roleData.find((item) => item.code === roleCode) ?? roleData[0] ?? null;
			if (nextRole) {
				setSelectedCode(nextRole.code);
				setRoleForm(toRoleForm(nextRole, permissionData));
				setIsCreatingRole(false);
			}

			const permissionCode = nextPermissionCode ?? selectedPermissionCode ?? permissionData[0]?.code ?? "";
			const nextPermission = permissionData.find((item) => item.code === permissionCode) ?? permissionData[0] ?? null;
			if (nextPermission) {
				setSelectedPermissionCode(nextPermission.code);
				setPermissionForm({
					code: nextPermission.code,
					label: nextPermission.label,
					tabGroup: nextPermission.tabGroup,
					description: nextPermission.description,
					sortOrder: nextPermission.sortOrder,
					active: nextPermission.active,
				});
				setIsCreatingPermission(false);
			}
		} catch (err) {
			setError(err instanceof Error ? err.message : "Không thể tải dữ liệu cấu hình");
		} finally {
			setLoading(false);
		}
	}

	useEffect(() => {
		const timer = window.setTimeout(() => {
			void loadAll();
		}, 0);
		return () => window.clearTimeout(timer);
		// eslint-disable-next-line react-hooks/exhaustive-deps
	}, []);

	function selectRole(role: RoleItem) {
		setViewTab("roles");
		setSelectedCode(role.code);
		setRoleForm(toRoleForm(role, permissions));
		setIsCreatingRole(false);
		setToast("");
	}

	function newRole() {
		setViewTab("roles");
		setSelectedCode("");
		setRoleForm(emptyRoleForm);
		setIsCreatingRole(true);
		setToast("");
	}

	function selectPermission(permission: PermissionDefinition) {
		setViewTab("permissions");
		setSelectedPermissionCode(permission.code);
		setPermissionForm({
			code: permission.code,
			label: permission.label,
			tabGroup: permission.tabGroup,
			description: permission.description,
			sortOrder: permission.sortOrder,
			active: permission.active,
		});
		setIsCreatingPermission(false);
		setToast("");
	}

	function newPermission() {
		setViewTab("permissions");
		setSelectedPermissionCode("");
		setPermissionForm(emptyPermissionForm);
		setIsCreatingPermission(true);
		setToast("");
	}

	function toggleRolePermission(permissionCode: string) {
		setRoleForm((prev) => {
			const exists = prev.permissionCodes.includes(permissionCode);
			return {
				...prev,
				permissionCodes: exists
					? prev.permissionCodes.filter((item) => item !== permissionCode)
					: [...prev.permissionCodes, permissionCode],
			};
		});
	}

	async function saveRole() {
		if (!roleForm.code.trim() || !roleForm.name.trim()) {
			setError("Mã vai trò và tên vai trò là bắt buộc");
			return;
		}
		setSaving(true);
		setError("");
		try {
			const payload = {
				code: roleForm.code.trim().toUpperCase(),
				name: roleForm.name.trim(),
				description: roleForm.description.trim(),
				color: roleForm.color,
				permissions: roleForm.permissionCodes,
			};
			const url = isCreatingRole
				? `${API_BASE}/api/admin/roles`
				: `${API_BASE}/api/admin/roles/${encodeURIComponent(selectedCode)}`;
			const res = await fetch(url, {
				method: isCreatingRole ? "POST" : "PUT",
				headers: authHeaders(),
				body: JSON.stringify(payload),
			});
			if (!res.ok) throw new Error(await res.text());
			setToast(isCreatingRole ? "Đã tạo vai trò" : "Đã cập nhật vai trò");
			await loadAll(payload.code, selectedPermissionCode || undefined);
		} catch (err) {
			setError(err instanceof Error ? err.message : "Không thể lưu vai trò");
		} finally {
			setSaving(false);
		}
	}

	async function deleteRole() {
		if (!selectedRole) return;
		if (!window.confirm(`Xóa vai trò ${selectedRole.code}?`)) return;
		setSaving(true);
		setError("");
		try {
			const res = await fetch(`${API_BASE}/api/admin/roles/${encodeURIComponent(selectedRole.code)}`, {
				method: "DELETE",
				headers: authHeaders(),
			});
			if (!res.ok) throw new Error(await res.text());
			setToast("Đã xóa vai trò");
			setSelectedCode("");
			setRoleForm(emptyRoleForm);
			await loadAll(undefined, selectedPermissionCode || undefined);
		} catch (err) {
			setError(err instanceof Error ? err.message : "Không thể xóa vai trò");
		} finally {
			setSaving(false);
		}
	}

	async function savePermission() {
		if (!permissionForm.code.trim() || !permissionForm.label.trim() || !permissionForm.tabGroup.trim()) {
			setError("Mã, tên quyền và tab là bắt buộc");
			return;
		}
		setSaving(true);
		setError("");
		try {
			const payload = {
				code: permissionForm.code.trim().toUpperCase(),
				label: permissionForm.label.trim(),
				tabGroup: permissionForm.tabGroup.trim(),
				description: permissionForm.description.trim(),
				sortOrder: Number(permissionForm.sortOrder),
				active: permissionForm.active,
			};
			if (isCreatingPermission) {
				await createPermissionDefinition(payload);
				setToast("Đã tạo quyền");
			} else {
				await updatePermissionDefinition(selectedPermissionCode, payload);
				setToast("Đã cập nhật quyền");
			}
			await loadAll(selectedCode || undefined, payload.code);
		} catch (err) {
			setError(err instanceof Error ? err.message : "Không thể lưu quyền");
		} finally {
			setSaving(false);
		}
	}

	async function deletePermission() {
		if (!selectedPermission) return;
		if (!window.confirm(`Xóa quyền ${selectedPermission.code}?`)) return;
		setSaving(true);
		setError("");
		try {
			await deletePermissionDefinition(selectedPermission.code);
			setToast("Đã xóa quyền");
			setSelectedPermissionCode("");
			setPermissionForm(emptyPermissionForm);
			await loadAll(selectedCode || undefined);
		} catch (err) {
			setError(err instanceof Error ? err.message : "Không thể xóa quyền");
		} finally {
			setSaving(false);
		}
	}

	return (
		<AdminShell
			title="Quản lý vai trò"
			subtitle="Định nghĩa vai trò, catalog quyền và tab quyền theo nhóm chức năng"
			actions={
				<div className="flex gap-2">
					<button type="button" onClick={newRole} className="inline-flex h-10 items-center gap-2 rounded-xl bg-blue-600 px-4 text-sm font-semibold text-white">
						<Plus size={16} /> Thêm vai trò
					</button>
					<button type="button" onClick={newPermission} className="inline-flex h-10 items-center gap-2 rounded-xl border border-blue-200 bg-white px-4 text-sm font-semibold text-blue-700">
						<Plus size={16} /> Thêm quyền
					</button>
				</div>
			}
		>
			<div className="space-y-4">
				<div className="flex gap-2 rounded-2xl border border-neutral-200 bg-white p-2 shadow-sm">
					<button
						type="button"
						onClick={() => setViewTab("roles")}
						className={`rounded-xl px-4 py-2 text-sm font-semibold ${viewTab === "roles" ? "bg-blue-600 text-white" : "bg-neutral-50 text-neutral-700"}`}
					>
						Vai trò
					</button>
					<button
						type="button"
						onClick={() => setViewTab("permissions")}
						className={`rounded-xl px-4 py-2 text-sm font-semibold ${viewTab === "permissions" ? "bg-blue-600 text-white" : "bg-neutral-50 text-neutral-700"}`}
					>
						Quyền
					</button>
				</div>

				{loading ? <div className="text-sm text-neutral-500">Đang tải...</div> : null}
				{error ? <div className="rounded-xl border border-red-200 bg-red-50 p-3 text-sm text-red-700">{error}</div> : null}
				{toast ? <div className="rounded-xl border border-green-200 bg-green-50 p-3 text-sm text-green-700">{toast}</div> : null}

				{viewTab === "roles" ? (
					<div className="grid gap-6 xl:grid-cols-[360px_minmax(0,1fr)]">
						<section className="rounded-2xl border border-neutral-200 bg-white p-4 shadow-sm">
							<div className="mb-3">
								<h2 className="text-base font-semibold text-neutral-900">Vai trò MiniBank</h2>
								<p className="text-sm text-neutral-500">Chọn một vai trò để xem và chỉnh quyền.</p>
							</div>

							<div className="space-y-2">
								{roles.map((role) => {
									const preview = role.permissions
										.map((item) => resolvePermissionLabel(resolvePermissionCode(item, permissions), permissions))
										.slice(0, 3);
									return (
										<button
											key={role.code}
											type="button"
											onClick={() => selectRole(role)}
											className={`w-full rounded-xl border p-4 text-left transition ${
												selectedCode === role.code && !isCreatingRole
													? colorClass[role.color] ?? "border-blue-200 bg-blue-50 text-blue-700"
													: "border-neutral-200 bg-white text-neutral-700 hover:bg-neutral-50"
											}`}
										>
											<div className="flex items-start justify-between gap-3">
												<div>
													<div className="font-semibold">{role.name}</div>
													<div className="mt-0.5 font-mono text-xs">{role.code}</div>
												</div>
												<Shield size={18} />
											</div>
											<p className="mt-2 text-sm opacity-80">{role.description}</p>
											<div className="mt-3 text-xs opacity-75">{role.totalUsers} nhân viên đang được gán</div>
											{preview.length ? (
												<div className="mt-3 flex flex-wrap gap-2">
													{preview.map((item) => (
														<span key={item} className="rounded-full bg-white/80 px-2 py-1 text-[11px] text-neutral-600">
															{item}
														</span>
													))}
												</div>
											) : null}
										</button>
									);
								})}
							</div>
						</section>

						<section className="rounded-2xl border border-neutral-200 bg-white shadow-sm">
							<div className="border-b border-neutral-200 px-6 py-5">
								<div className="flex flex-wrap items-center justify-between gap-3">
									<div>
										<h2 className="text-lg font-semibold text-neutral-900">{isCreatingRole ? "Thêm vai trò" : selectedRole?.name ?? "Vai trò"}</h2>
										<p className="mt-1 text-sm text-neutral-500">Chọn quyền theo từng tab chức năng, không cần nhập tay.</p>
									</div>
									<div className="flex gap-2">
										{!isCreatingRole && selectedRole ? (
											<button
												type="button"
												onClick={deleteRole}
												disabled={saving || selectedRole.totalUsers > 0}
												className="inline-flex h-10 items-center gap-2 rounded-xl border border-red-200 px-4 text-sm font-semibold text-red-600 disabled:cursor-not-allowed disabled:opacity-50"
											>
												<Trash2 size={16} /> Xóa
											</button>
										) : null}
										<button
											type="button"
											onClick={saveRole}
											disabled={saving}
											className="inline-flex h-10 items-center gap-2 rounded-xl bg-blue-600 px-4 text-sm font-semibold text-white disabled:opacity-60"
										>
											<Save size={16} /> {saving ? "Đang lưu..." : "Lưu"}
										</button>
									</div>
								</div>
							</div>

							<div className="grid gap-5 p-6 lg:grid-cols-[1fr_1.15fr]">
								<div className="space-y-4">
									<div className="grid gap-4 md:grid-cols-2">
										<label className="text-sm">
											<span className="mb-1 block text-neutral-600">Mã vai trò</span>
											<input
												className="h-10 w-full rounded-xl border border-neutral-200 px-3 font-mono text-sm outline-none focus:ring-2 focus:ring-blue-500"
												value={roleForm.code}
												onChange={(e) => setRoleForm((prev) => ({ ...prev, code: e.target.value.toUpperCase() }))}
												disabled={!isCreatingRole}
												placeholder="CUSTOM_ROLE"
											/>
										</label>
										<label className="text-sm">
											<span className="mb-1 block text-neutral-600">Tên vai trò</span>
											<input
												className="h-10 w-full rounded-xl border border-neutral-200 px-3 text-sm outline-none focus:ring-2 focus:ring-blue-500"
												value={roleForm.name}
												onChange={(e) => setRoleForm((prev) => ({ ...prev, name: e.target.value }))}
												placeholder="Nhân viên nghiệp vụ"
											/>
										</label>
									</div>
									<div className="grid gap-4 md:grid-cols-2">
										<label className="text-sm">
											<span className="mb-1 block text-neutral-600">Màu</span>
											<select
												className="h-10 w-full rounded-xl border border-neutral-200 px-3 text-sm outline-none focus:ring-2 focus:ring-blue-500"
												value={roleForm.color}
												onChange={(e) => setRoleForm((prev) => ({ ...prev, color: e.target.value }))}
											>
												{["red", "blue", "emerald", "amber", "violet"].map((color) => (
													<option key={color} value={color}>{color}</option>
												))}
											</select>
										</label>
										<label className="text-sm">
											<span className="mb-1 block text-neutral-600">Mô tả</span>
											<input
												className="h-10 w-full rounded-xl border border-neutral-200 px-3 text-sm outline-none focus:ring-2 focus:ring-blue-500"
												value={roleForm.description}
												onChange={(e) => setRoleForm((prev) => ({ ...prev, description: e.target.value }))}
												placeholder="Mô tả ngắn cho vai trò"
											/>
										</label>
									</div>
									<div className="rounded-2xl border border-neutral-200 bg-neutral-50 p-4">
										<div className="text-sm font-semibold text-neutral-900">Quyền đã chọn</div>
										<div className="mt-2 flex flex-wrap gap-2">
											{roleForm.permissionCodes.length ? roleForm.permissionCodes.map((code) => (
												<span key={code} className="rounded-full bg-white px-3 py-1 text-xs text-neutral-700 shadow-sm">
													{resolvePermissionLabel(code, permissions)}
												</span>
											)) : (
												<span className="text-sm text-neutral-500">Chưa chọn quyền nào.</span>
											)}
										</div>
									</div>
								</div>

								<div className="space-y-4">
									{sortedTabGroups.map((tabGroup) => {
										const items = groupedPermissions[tabGroup];
										return (
											<section key={tabGroup} className="rounded-2xl border border-neutral-200 bg-white p-4">
												<div className="mb-3 flex items-center justify-between gap-3">
													<div>
														<div className="text-sm font-semibold text-neutral-900">{tabGroup}</div>
														<div className="text-xs text-neutral-500">Chọn quyền theo nhóm tab chức năng.</div>
													</div>
													<span className="rounded-full bg-neutral-100 px-2 py-1 text-xs text-neutral-600">{items.length}</span>
												</div>
												<div className="grid gap-2 md:grid-cols-2">
													{items.map((permission) => {
														const active = roleForm.permissionCodes.includes(permission.code);
														return (
															<button
																key={permission.code}
																type="button"
																onClick={() => {
																	setViewTab("roles");
																	toggleRolePermission(permission.code);
																}}
																className={`rounded-xl border p-3 text-left transition ${active ? "border-blue-300 bg-blue-50" : "border-neutral-200 hover:bg-neutral-50"}`}
															>
																<div className="flex items-start gap-3">
																	<input type="checkbox" checked={active} readOnly className="mt-1 h-4 w-4" />
																	<div className="min-w-0">
																		<div className="text-sm font-semibold text-neutral-900">{permission.label}</div>
																		<div className="font-mono text-[11px] text-neutral-400">{permission.code}</div>
																		{permission.description ? <div className="mt-1 text-xs text-neutral-500">{permission.description}</div> : null}
																	</div>
																</div>
															</button>
														);
													})}
												</div>
											</section>
										);
									})}
								</div>
							</div>
						</section>
					</div>
				) : (
					<div className="grid gap-6 xl:grid-cols-[360px_minmax(0,1fr)]">
						<section className="rounded-2xl border border-neutral-200 bg-white p-4 shadow-sm">
							<div className="mb-3">
								<h2 className="text-base font-semibold text-neutral-900">Danh sách quyền</h2>
								<p className="text-sm text-neutral-500">Mỗi quyền thuộc một tab chức năng để gán cho vai trò.</p>
							</div>

							<div className="space-y-4">
								{sortedTabGroups.map((tabGroup) => (
									<div key={tabGroup}>
										<div className="mb-2 flex items-center justify-between">
											<div className="text-sm font-semibold text-neutral-800">{tabGroup}</div>
											<div className="rounded-full bg-neutral-100 px-2 py-1 text-xs text-neutral-500">{groupedPermissions[tabGroup].length}</div>
										</div>
										<div className="space-y-2">
											{groupedPermissions[tabGroup].map((permission) => (
												<button
													key={permission.code}
													type="button"
													onClick={() => selectPermission(permission)}
													className={`w-full rounded-xl border px-3 py-2 text-left transition ${selectedPermissionCode === permission.code && !isCreatingPermission ? "border-blue-300 bg-blue-50" : "border-neutral-200 bg-white hover:bg-neutral-50"}`}
												>
													<div className="flex items-start justify-between gap-3">
														<div>
															<div className="text-sm font-semibold text-neutral-900">{permission.label}</div>
															<div className="font-mono text-[11px] text-neutral-400">{permission.code}</div>
														</div>
														<span className={`rounded-full px-2 py-1 text-[11px] ${permission.active ? "bg-green-100 text-green-700" : "bg-neutral-100 text-neutral-500"}`}>
															{permission.active ? "Đang dùng" : "Tắt"}
														</span>
													</div>
												</button>
											))}
										</div>
									</div>
								))}
							</div>
						</section>

						<section className="rounded-2xl border border-neutral-200 bg-white shadow-sm">
							<div className="border-b border-neutral-200 px-6 py-5">
								<div className="flex flex-wrap items-center justify-between gap-3">
									<div>
										<h2 className="text-lg font-semibold text-neutral-900">{isCreatingPermission ? "Thêm quyền" : selectedPermission?.label ?? "Quyền"}</h2>
										<p className="mt-1 text-sm text-neutral-500">Quản lý quyền theo tab chức năng để gán cho vai trò.</p>
									</div>
									<div className="flex gap-2">
										{!isCreatingPermission && selectedPermission ? (
											<button
												type="button"
												onClick={deletePermission}
												disabled={saving}
												className="inline-flex h-10 items-center gap-2 rounded-xl border border-red-200 px-4 text-sm font-semibold text-red-600 disabled:cursor-not-allowed disabled:opacity-50"
											>
												<Trash2 size={16} /> Xóa
											</button>
										) : null}
										<button
											type="button"
											onClick={savePermission}
											disabled={saving}
											className="inline-flex h-10 items-center gap-2 rounded-xl bg-blue-600 px-4 text-sm font-semibold text-white disabled:opacity-60"
										>
											<Save size={16} /> {saving ? "Đang lưu..." : "Lưu"}
										</button>
									</div>
								</div>
							</div>

							<div className="grid gap-5 p-6 md:grid-cols-2">
								<label className="text-sm">
									<span className="mb-1 block text-neutral-600">Mã quyền</span>
									<input
										className="h-10 w-full rounded-xl border border-neutral-200 px-3 font-mono text-sm outline-none focus:ring-2 focus:ring-blue-500"
										value={permissionForm.code}
										onChange={(e) => setPermissionForm((prev) => ({ ...prev, code: e.target.value.toUpperCase() }))}
										disabled={!isCreatingPermission}
										placeholder="STAFF_MANAGE"
									/>
								</label>
								<label className="text-sm">
									<span className="mb-1 block text-neutral-600">Tên quyền</span>
									<input
										className="h-10 w-full rounded-xl border border-neutral-200 px-3 text-sm outline-none focus:ring-2 focus:ring-blue-500"
										value={permissionForm.label}
										onChange={(e) => setPermissionForm((prev) => ({ ...prev, label: e.target.value }))}
										placeholder="Quản lý nhân viên"
									/>
								</label>
								<label className="text-sm">
									<span className="mb-1 block text-neutral-600">Tab chức năng</span>
									<input
										className="h-10 w-full rounded-xl border border-neutral-200 px-3 text-sm outline-none focus:ring-2 focus:ring-blue-500"
										value={permissionForm.tabGroup}
										onChange={(e) => setPermissionForm((prev) => ({ ...prev, tabGroup: e.target.value }))}
										placeholder="Quản trị hệ thống"
									/>
								</label>
								<label className="text-sm">
									<span className="mb-1 block text-neutral-600">Thứ tự</span>
									<input
										className="h-10 w-full rounded-xl border border-neutral-200 px-3 text-sm outline-none focus:ring-2 focus:ring-blue-500"
										type="number"
										min={0}
										value={permissionForm.sortOrder}
										onChange={(e) => setPermissionForm((prev) => ({ ...prev, sortOrder: Number(e.target.value) }))}
									/>
								</label>
								<label className="text-sm md:col-span-2">
									<span className="mb-1 block text-neutral-600">Mô tả</span>
									<textarea
										className="min-h-24 w-full rounded-xl border border-neutral-200 px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-blue-500"
										value={permissionForm.description}
										onChange={(e) => setPermissionForm((prev) => ({ ...prev, description: e.target.value }))}
									/>
								</label>
								<label className="flex items-center gap-2 text-sm text-neutral-700 md:col-span-2">
									<input
										type="checkbox"
										checked={permissionForm.active}
										onChange={(e) => setPermissionForm((prev) => ({ ...prev, active: e.target.checked }))}
									/>
									Đang áp dụng
								</label>
							</div>
						</section>
					</div>
				)}
			</div>
		</AdminShell>
	);
}