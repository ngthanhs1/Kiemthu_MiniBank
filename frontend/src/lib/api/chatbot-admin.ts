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
    throw new Error(text || "Không thể xử lý yêu cầu");
  }

  if (res.status === 204) return undefined as T;
  return res.json() as Promise<T>;
}

export interface AdminFaqCategory {
  id: number;
  code: string;
  name: string;
  description: string | null;
  sortOrder: number;
  active: boolean;
  faqCount: number;
}

export interface AdminFaqItem {
  id: number;
  categoryId: number;
  categoryName: string;
  categoryCode: string;
  parentFaqId: number | null;
  question: string;
  answer: string;
  active: boolean;
  childCount: number;
  keywords: string[];
}

export interface AdminChatConversationSummary {
  id: number;
  userId: number;
  userName: string | null;
  userPhone: string | null;
  customerRank: string | null;
  status: string;
  lastIntent: string | null;
  lastConfidence: number | null;
  startedAt: string;
  escalatedAt: string | null;
  lastMessagePreview: string | null;
  assignedAdminUserId: number | null;
  assignedAdminUsername: string | null;
}

export interface AdminChatMessage {
  id: number;
  senderType: string;
  senderId: number | null;
  messageType: string;
  content: string;
  createdAt: string;
}

export interface AdminChatConversationDetail {
  id: number;
  userId: number;
  userName: string | null;
  userPhone: string | null;
  customerRank: string | null;
  channel: string;
  status: string;
  lastIntent: string | null;
  lastConfidence: number | null;
  startedAt: string;
  escalatedAt: string | null;
  assignedAdminUserId: number | null;
  assignedAdminUsername: string | null;
  messages: AdminChatMessage[];
}

export interface AdminSupportNote {
  id: number;
  userId: number;
  userName: string | null;
  userCode: string | null;
  content: string;
  noteType: string;
  conversationId: number | null;
  createdByName: string | null;
  createdAt: string;
}

const SUPPORT_NOTES_KEY = "adminSupportNotes";

function readLocalSupportNotes(): AdminSupportNote[] {
  if (typeof window === "undefined") return [];
  try {
    const raw = localStorage.getItem(SUPPORT_NOTES_KEY);
    if (!raw) return [];
    const parsed = JSON.parse(raw);
    return Array.isArray(parsed) ? (parsed as AdminSupportNote[]) : [];
  } catch {
    return [];
  }
}

function writeLocalSupportNotes(notes: AdminSupportNote[]) {
  if (typeof window === "undefined") return;
  localStorage.setItem(SUPPORT_NOTES_KEY, JSON.stringify(notes));
}

export async function listFaqCategories(): Promise<AdminFaqCategory[]> {
  return requestJson<AdminFaqCategory[]>("/api/admin/chatbot/faq/categories");
}

export async function createFaqCategory(payload: {
  code: string;
  name: string;
  description?: string;
  sortOrder: number;
  active: boolean;
}): Promise<AdminFaqCategory> {
  return requestJson<AdminFaqCategory>("/api/admin/chatbot/faq/categories", {
    method: "POST",
    body: JSON.stringify(payload),
  });
}

export async function updateFaqCategory(
  categoryId: number,
  payload: {
    code: string;
    name: string;
    description?: string;
    sortOrder: number;
    active: boolean;
  }
): Promise<AdminFaqCategory> {
  return requestJson<AdminFaqCategory>(`/api/admin/chatbot/faq/categories/${categoryId}`, {
    method: "PUT",
    body: JSON.stringify(payload),
  });
}

export async function listFaqItems(categoryId?: number, q?: string): Promise<AdminFaqItem[]> {
  const params = new URLSearchParams();
  if (categoryId) params.set("categoryId", String(categoryId));
  if (q) params.set("q", q);
  const query = params.toString() ? `?${params.toString()}` : "";
  return requestJson<AdminFaqItem[]>(`/api/admin/chatbot/faq/items${query}`);
}

export async function getFaqItem(faqId: number): Promise<AdminFaqItem> {
  return requestJson<AdminFaqItem>(`/api/admin/chatbot/faq/items/${faqId}`);
}

export async function createFaqItem(payload: {
  categoryId: number;
  parentFaqId?: number | null;
  question: string;
  answer: string;
  active: boolean;
  keywords: string[];
}): Promise<AdminFaqItem> {
  return requestJson<AdminFaqItem>("/api/admin/chatbot/faq/items", {
    method: "POST",
    body: JSON.stringify(payload),
  });
}

export async function updateFaqItem(
  faqId: number,
  payload: {
    categoryId: number;
    parentFaqId?: number | null;
    question: string;
    answer: string;
    active: boolean;
    keywords: string[];
  }
): Promise<AdminFaqItem> {
  return requestJson<AdminFaqItem>(`/api/admin/chatbot/faq/items/${faqId}`, {
    method: "PUT",
    body: JSON.stringify(payload),
  });
}

export async function deleteFaqItem(faqId: number): Promise<void> {
  await requestJson<void>(`/api/admin/chatbot/faq/items/${faqId}`, { method: "DELETE" });
}

export async function listChatConversations(status?: string): Promise<AdminChatConversationSummary[]> {
  const params = new URLSearchParams();
  if (status) params.set("status", status);
  const query = params.toString() ? `?${params.toString()}` : "";
  return requestJson<AdminChatConversationSummary[]>(`/api/admin/chatbot/conversations${query}`);
}

export async function getChatConversation(conversationId: number): Promise<AdminChatConversationDetail> {
  return requestJson<AdminChatConversationDetail>(`/api/admin/chatbot/conversations/${conversationId}`);
}

export async function assignChatConversation(conversationId: number): Promise<AdminChatConversationDetail> {
  return requestJson<AdminChatConversationDetail>(`/api/admin/chatbot/conversations/${conversationId}/assign-self`, {
    method: "POST",
  });
}

export async function replyChatConversation(conversationId: number, message: string): Promise<AdminChatConversationDetail> {
  return requestJson<AdminChatConversationDetail>(`/api/admin/chatbot/conversations/${conversationId}/reply`, {
    method: "POST",
    body: JSON.stringify({ message }),
  });
}

export async function closeChatConversation(conversationId: number): Promise<AdminChatConversationDetail> {
  return requestJson<AdminChatConversationDetail>(`/api/admin/chatbot/conversations/${conversationId}/close`, {
    method: "POST",
  });
}

export async function addChatNote(conversationId: number, note: string): Promise<void> {
  await requestJson<void>(`/api/admin/chatbot/conversations/${conversationId}/notes`, {
    method: "POST",
    body: JSON.stringify({ note }),
  });
}

export async function listSupportNotes(): Promise<AdminSupportNote[]> {
  return readLocalSupportNotes().sort((a, b) => b.createdAt.localeCompare(a.createdAt));
}

export async function createSupportNote(payload: {
  userId: number;
  content: string;
  noteType: string;
  conversationId?: number | null;
}): Promise<AdminSupportNote> {
  const notes = readLocalSupportNotes();
  const note: AdminSupportNote = {
    id: Date.now(),
    userId: payload.userId,
    userName: payload.userId > 0 ? `KH #${payload.userId}` : "Khách hàng",
    userCode: payload.userId > 0 ? String(payload.userId) : null,
    content: payload.content,
    noteType: payload.noteType,
    conversationId: payload.conversationId ?? null,
    createdByName: "Admin",
    createdAt: new Date().toISOString(),
  };
  writeLocalSupportNotes([note, ...notes]);
  return note;
}

export async function updateSupportNote(
  noteId: number,
  payload: {
    userId: number;
    content: string;
    noteType: string;
    conversationId?: number | null;
  }
): Promise<AdminSupportNote> {
  const notes = readLocalSupportNotes();
  const existing = notes.find((item) => item.id === noteId);
  if (!existing) throw new Error("Không tìm thấy ghi chú");
  const updated: AdminSupportNote = {
    ...existing,
    userId: payload.userId,
    userName: payload.userId > 0 ? existing.userName ?? `KH #${payload.userId}` : existing.userName,
    userCode: payload.userId > 0 ? existing.userCode ?? String(payload.userId) : existing.userCode,
    content: payload.content,
    noteType: payload.noteType,
    conversationId: payload.conversationId ?? null,
  };
  writeLocalSupportNotes(notes.map((item) => (item.id === noteId ? updated : item)));
  return updated;
}

export async function deleteSupportNote(noteId: number): Promise<void> {
  writeLocalSupportNotes(readLocalSupportNotes().filter((item) => item.id !== noteId));
}
