"use client";

import { useEffect, useMemo, useRef, useState } from "react";

import AdminShell from "@/src/app/components/admin-shell";
import {
  addChatNote,
  AdminChatConversationDetail,
  AdminChatConversationSummary,
  AdminFaqCategory,
  AdminFaqItem,
  AdminSupportNote,
  assignChatConversation,
  closeChatConversation,
  createFaqCategory,
  createFaqItem,
  createSupportNote,
  deleteFaqItem,
  deleteSupportNote,
  getChatConversation,
  listChatConversations,
  listFaqCategories,
  listFaqItems,
  listSupportNotes,
  replyChatConversation,
  updateFaqCategory,
  updateFaqItem,
  updateSupportNote,
} from "@/src/lib/api/chatbot-admin";
import { backendWsUrl, MiniStompClient } from "@/src/lib/stomp-client";

// ─────────────────────────── TYPES ────────────────────────────

type Tab = "faq" | "chat" | "notes";

type FaqNode = AdminFaqItem & { children: FaqNode[] };

type NoteType = "GENERAL" | "COMPLAINT" | "FEEDBACK" | "TECHNICAL";

// ─────────────────────────── HELPERS ──────────────────────────

function formatTime(iso?: string | null) {
  if (!iso) return "-";
  const d = new Date(iso);
  if (Number.isNaN(d.getTime())) return "-";
  return d.toLocaleString("vi-VN", {
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
    hour: "2-digit",
    minute: "2-digit",
  });
}

function formatTimeShort(iso?: string | null) {
  if (!iso) return "-";
  const d = new Date(iso);
  if (Number.isNaN(d.getTime())) return "-";
  return `${String(d.getHours()).padStart(2, "0")}:${String(d.getMinutes()).padStart(2, "0")}`;
}

const NOTE_TYPE_CONFIG: Record<NoteType, { label: string; bg: string; text: string; border: string }> = {
  GENERAL:   { label: "Chung",    bg: "bg-violet-50",  text: "text-violet-700", border: "border-violet-200" },
  COMPLAINT: { label: "Khiếu nại", bg: "bg-red-50",   text: "text-red-700",    border: "border-red-200" },
  FEEDBACK:  { label: "Phản hồi", bg: "bg-emerald-50", text: "text-emerald-700", border: "border-emerald-200" },
  TECHNICAL: { label: "Kỹ thuật", bg: "bg-amber-50",  text: "text-amber-700",  border: "border-amber-200" },
};

const STATUS_CONFIG: Record<string, { label: string; dot: string; text: string; bg: string }> = {
  WAITING_AGENT: { label: "Đang chờ CSKH",    dot: "bg-amber-400",   text: "text-amber-700",   bg: "bg-amber-50" },
  IN_PROGRESS:   { label: "Đang xử lý",        dot: "bg-emerald-400", text: "text-emerald-700", bg: "bg-emerald-50" },
  OPEN:          { label: "Bot xử lý",          dot: "bg-violet-400",  text: "text-violet-700",  bg: "bg-violet-50" },
  CLOSED:        { label: "Đã đóng",            dot: "bg-gray-300",    text: "text-gray-500",    bg: "bg-gray-50" },
};

function statusCfg(status: string) {
  return STATUS_CONFIG[status] ?? STATUS_CONFIG.OPEN;
}

const AVATAR_COLORS = [
  "bg-violet-100 text-violet-700",
  "bg-teal-100 text-teal-700",
  "bg-blue-100 text-blue-700",
  "bg-amber-100 text-amber-700",
  "bg-pink-100 text-pink-700",
];

function avatarColor(id: number) {
  return AVATAR_COLORS[id % AVATAR_COLORS.length];
}

function initials(name?: string | null) {
  if (!name) return "?";
  return name
    .split(" ")
    .filter(Boolean)
    .slice(0, 2)
    .map((w) => w[0].toUpperCase())
    .join("");
}

// ─────────────────────────── MAIN PAGE ────────────────────────

export default function ChatbotManagementPage() {
  const [tab, setTab] = useState<Tab>("faq");

  // FAQ state
  const [categories, setCategories] = useState<AdminFaqCategory[]>([]);
  const [selectedCategoryId, setSelectedCategoryId] = useState<number | null>(null);
  const [faqs, setFaqs] = useState<AdminFaqItem[]>([]);
  const [selectedFaq, setSelectedFaq] = useState<AdminFaqItem | null>(null);
  const [faqSearch, setFaqSearch] = useState("");
  const [categoryForm, setCategoryForm] = useState({
    id: 0, code: "", name: "", description: "", sortOrder: 10, active: true,
  });
  const [faqForm, setFaqForm] = useState({
    id: 0, categoryId: 0, parentFaqId: 0, question: "", answer: "", active: true, keywords: "",
  });

  // Chat state
  const [chatStatus, setChatStatus] = useState("");
  const [conversations, setConversations] = useState<AdminChatConversationSummary[]>([]);
  const [selectedConversation, setSelectedConversation] = useState<AdminChatConversationDetail | null>(null);
  const [replyText, setReplyText] = useState("");
  const [noteText, setNoteText] = useState("");
  const [agentTyping, setAgentTyping] = useState(false);
  const chatBodyRef = useRef<HTMLDivElement>(null);
  const stompRef = useRef<MiniStompClient | null>(null);

  // Notes state
  const [notes, setNotes] = useState<AdminSupportNote[]>([]);
  const [noteSearch, setNoteSearch] = useState("");
  const [noteTypeFilter, setNoteTypeFilter] = useState<string>("");
  const [noteFormOpen, setNoteFormOpen] = useState(false);
  const [editingNote, setEditingNote] = useState<AdminSupportNote | null>(null);
  const [noteForm, setNoteForm] = useState({
    userId: 0, content: "", noteType: "GENERAL" as NoteType, conversationId: 0,
  });

  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");

  // ─── Derived ───

  const selectedCategory = useMemo(
    () => categories.find((item) => item.id === selectedCategoryId) ?? null,
    [categories, selectedCategoryId]
  );

  const faqTree = useMemo(() => {
    const keyword = faqSearch.trim().toLowerCase();
    const source = selectedCategoryId ? faqs.filter((item) => item.categoryId === selectedCategoryId) : faqs;
    const nodes = source.map((item) => ({ ...item, children: [] as FaqNode[] }));
    const nodeById = new Map(nodes.map((node) => [node.id, node]));
    const byParent = new Map<number, FaqNode[]>();
    for (const node of nodes) {
      const parentId = node.parentFaqId ?? 0;
      const effectiveParentId = parentId > 0 && nodeById.has(parentId) ? parentId : 0;
      byParent.set(effectiveParentId, [...(byParent.get(effectiveParentId) ?? []), node]);
    }
    for (const node of nodes) node.children = byParent.get(node.id) ?? [];
    const roots = byParent.get(0) ?? [];
    if (!keyword) return roots;
    const isMatched = (node: FaqNode) =>
      node.question.toLowerCase().includes(keyword) ||
      node.answer.toLowerCase().includes(keyword) ||
      node.keywords.some((item) => item.toLowerCase().includes(keyword));
    const filterNode = (node: FaqNode): FaqNode | null => {
      const matched = isMatched(node);
      const filteredChildren = node.children.map(filterNode).filter((item): item is FaqNode => Boolean(item));
      if (matched) return { ...node, children: node.children };
      if (filteredChildren.length > 0) return { ...node, children: filteredChildren };
      return null;
    };
    return roots.map(filterNode).filter((item): item is FaqNode => Boolean(item));
  }, [faqs, faqSearch, selectedCategoryId]);

  const faqParentOptions = useMemo(() => {
    const categoryId = Number(faqForm.categoryId || selectedCategoryId || 0);
    const sameCategoryFaqs = faqs.filter((item) => item.categoryId === categoryId);
    const childrenByParent = new Map<number, AdminFaqItem[]>();
    for (const item of sameCategoryFaqs) {
      const parentId = item.parentFaqId ?? 0;
      childrenByParent.set(parentId, [...(childrenByParent.get(parentId) ?? []), item]);
    }
    const excludedIds = new Set<number>();
    if (faqForm.id > 0) {
      const stack = [faqForm.id];
      while (stack.length > 0) {
        const id = stack.pop()!;
        excludedIds.add(id);
        for (const child of childrenByParent.get(id) ?? []) stack.push(child.id);
      }
    }
    const result: { item: AdminFaqItem; level: number }[] = [];
    const visited = new Set<number>();
    const walk = (parentId: number, level: number) => {
      const children = [...(childrenByParent.get(parentId) ?? [])].sort((a, b) =>
        a.question.localeCompare(b.question, "vi")
      );
      for (const item of children) {
        if (visited.has(item.id) || excludedIds.has(item.id)) continue;
        visited.add(item.id);
        result.push({ item, level });
        walk(item.id, level + 1);
      }
    };
    walk(0, 0);
    for (const item of sameCategoryFaqs) {
      if (visited.has(item.id) || excludedIds.has(item.id)) continue;
      visited.add(item.id);
      result.push({ item, level: 0 });
      walk(item.id, 1);
    }
    return result;
  }, [faqs, faqForm.categoryId, faqForm.id, selectedCategoryId]);

  const selectedParentFaq = useMemo(
    () => faqs.find((item) => item.id === faqForm.parentFaqId) ?? null,
    [faqs, faqForm.parentFaqId]
  );

  const filteredNotes = useMemo(() => {
    const kw = noteSearch.trim().toLowerCase();
    return notes.filter((n) => {
      const matchType = !noteTypeFilter || n.noteType === noteTypeFilter;
      const matchKw =
        !kw ||
        (n.userName ?? "").toLowerCase().includes(kw) ||
        n.content.toLowerCase().includes(kw);
      return matchType && matchKw;
    });
  }, [notes, noteSearch, noteTypeFilter]);

  const noteStats = useMemo(() => ({
    total: notes.length,
    GENERAL:   notes.filter((n) => n.noteType === "GENERAL").length,
    COMPLAINT: notes.filter((n) => n.noteType === "COMPLAINT").length,
    FEEDBACK:  notes.filter((n) => n.noteType === "FEEDBACK").length,
    TECHNICAL: notes.filter((n) => n.noteType === "TECHNICAL").length,
  }), [notes]);

  // ─── Load functions ───

  async function loadCategories() {
    const list = await listFaqCategories();
    setCategories(list);
    if (!selectedCategoryId && list.length > 0) setSelectedCategoryId(list[0].id);
  }

  async function loadFaqs() {
    if (!selectedCategoryId) { setFaqs([]); return; }
    const list = await listFaqItems(selectedCategoryId);
    setFaqs(list);
  }

  async function loadConversations() {
    const list = await listChatConversations(chatStatus || undefined);
    setConversations(list);
  }

  async function loadNotes() {
    const list = await listSupportNotes();
    setNotes(list);
  }

  // ─── Effects ───

  useEffect(() => {
    let alive = true;
    setLoading(true);
    setError("");
    Promise.all([loadCategories(), loadConversations(), loadNotes()])
      .catch((e: unknown) => {
        if (!alive) return;
        setError(e instanceof Error ? e.message : "Không thể tải dữ liệu");
      })
      .finally(() => { if (!alive) return; setLoading(false); });
    return () => { alive = false; };
  }, []);

  useEffect(() => {
    const client = new MiniStompClient(backendWsUrl());
    stompRef.current = client;
    client.connect();

    const unsub = client.subscribe("/topic/chat-waiting", (body) => {
      const event = body as {
        conversationId?: number; userId?: number; customerName?: string | null;
        customerPhone?: string | null; customerRank?: string | null; status?: string;
        startedAt?: string; escalatedAt?: string | null;
        assignedAdminUserId?: number | null; assignedAdminUsername?: string | null;
        lastMessagePreview?: string | null;
      };
      if (!event.conversationId) return;
      setConversations((prev) => {
        const next: AdminChatConversationSummary = {
          id: event.conversationId!, userId: event.userId ?? 0,
          userName: event.customerName ?? null, userPhone: event.customerPhone ?? null,
          customerRank: event.customerRank ?? null, status: event.status ?? "WAITING_AGENT",
          lastIntent: null, lastConfidence: null,
          startedAt: event.startedAt ?? new Date().toISOString(),
          escalatedAt: event.escalatedAt ?? null,
          lastMessagePreview: event.lastMessagePreview ?? null,
          assignedAdminUserId: event.assignedAdminUserId ?? null,
          assignedAdminUsername: event.assignedAdminUsername ?? null,
        };
        return [next, ...prev.filter((item) => item.id !== next.id)];
      });
      setSelectedConversation((current) => {
        if (!current || current.id !== event.conversationId) return current;
        return {
          ...current,
          status: event.status ?? current.status,
          assignedAdminUserId: event.assignedAdminUserId ?? current.assignedAdminUserId,
          assignedAdminUsername: event.assignedAdminUsername ?? current.assignedAdminUsername,
        };
      });
    });

    return () => { unsub(); client.disconnect(); stompRef.current = null; };
  }, []);

  useEffect(() => {
    if (!selectedConversation || !stompRef.current) return;
    const conversationId = selectedConversation.id;
    const unsubMsg = stompRef.current.subscribe(`/topic/chat/${conversationId}`, (body) => {
      const message = body as AdminChatConversationDetail["messages"][number];
      if (!message?.id) return;
      setSelectedConversation((current) => {
        if (!current || current.id !== conversationId || current.messages.some((item) => item.id === message.id)) return current;
        return { ...current, messages: [...current.messages, message] };
      });
      scrollChatToBottom();
    });
    const unsubTyping = stompRef.current.subscribe(`/topic/chat/${conversationId}/typing`, (body) => {
      const evt = body as { senderType?: string; typing?: boolean };
      if (evt.senderType?.toUpperCase() === "USER") {
        setAgentTyping(evt.typing ?? false);
        if (evt.typing) setTimeout(() => setAgentTyping(false), 4000);
      }
    });
    return () => { unsubMsg(); unsubTyping(); };
  }, [selectedConversation?.id]);

  useEffect(() => {
    let alive = true;
    loadFaqs().catch((e: unknown) => {
      if (!alive) return;
      setError(e instanceof Error ? e.message : "Không thể tải FAQ");
    });
    return () => { alive = false; };
  }, [selectedCategoryId]);

  useEffect(() => {
    let alive = true;
    loadConversations().catch((e: unknown) => {
      if (!alive) return;
      setError(e instanceof Error ? e.message : "Không thể tải danh sách chat");
    });
    return () => { alive = false; };
  }, [chatStatus]);

  function scrollChatToBottom() {
    setTimeout(() => {
      if (chatBodyRef.current) {
        chatBodyRef.current.scrollTop = chatBodyRef.current.scrollHeight;
      }
    }, 60);
  }

  // ─── FAQ actions ───

  function selectFaq(item: AdminFaqItem) {
    setSelectedFaq(item);
    setFaqForm({
      id: item.id, categoryId: item.categoryId, parentFaqId: item.parentFaqId ?? 0,
      question: item.question, answer: item.answer, active: item.active,
      keywords: item.keywords.join(", "),
    });
  }

  async function onSaveCategory() {
    setSaving(true); setError("");
    try {
      const payload = {
        code: categoryForm.code.trim(), name: categoryForm.name.trim(),
        description: categoryForm.description.trim(),
        sortOrder: Number(categoryForm.sortOrder), active: categoryForm.active,
      };
      if (!payload.code || !payload.name) throw new Error("Mã và tên danh mục là bắt buộc");
      if (categoryForm.id > 0) await updateFaqCategory(categoryForm.id, payload);
      else await createFaqCategory(payload);
      await loadCategories();
      setCategoryForm({ id: 0, code: "", name: "", description: "", sortOrder: 10, active: true });
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : "Không thể lưu danh mục");
    } finally { setSaving(false); }
  }

  async function onSaveFaq() {
    setSaving(true); setError("");
    try {
      const categoryId = Number(faqForm.categoryId || selectedCategoryId || 0);
      if (!categoryId) throw new Error("Vui lòng chọn danh mục");
      if (!faqForm.question.trim() || !faqForm.answer.trim()) throw new Error("Câu hỏi và câu trả lời là bắt buộc");
      if (faqForm.parentFaqId > 0) {
        const parentFaq = faqs.find((item) => item.id === faqForm.parentFaqId);
        if (!parentFaq) throw new Error("FAQ cha không tồn tại");
        if (parentFaq.categoryId !== categoryId) throw new Error("FAQ cha phải cùng danh mục");
        if (faqForm.id > 0 && parentFaq.id === faqForm.id) throw new Error("Không thể chọn chính FAQ này làm cha");
      }
      const payload = {
        categoryId, parentFaqId: faqForm.parentFaqId > 0 ? faqForm.parentFaqId : null,
        question: faqForm.question.trim(), answer: faqForm.answer.trim(), active: faqForm.active,
        keywords: faqForm.keywords.split(",").map((v) => v.trim()).filter(Boolean),
      };
      if (faqForm.id > 0) await updateFaqItem(faqForm.id, payload);
      else await createFaqItem(payload);
      await Promise.all([loadFaqs(), loadCategories()]);
      setFaqForm({ id: 0, categoryId, parentFaqId: faqForm.parentFaqId, question: "", answer: "", active: true, keywords: "" });
      setSelectedFaq(null);
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : "Không thể lưu FAQ");
    } finally { setSaving(false); }
  }

  async function onDeleteFaq() {
    if (!selectedFaq) return;
    if (!window.confirm("Xóa FAQ đã chọn?")) return;
    setSaving(true); setError("");
    try {
      await deleteFaqItem(selectedFaq.id);
      await Promise.all([loadFaqs(), loadCategories()]);
      setSelectedFaq(null);
      setFaqForm({ id: 0, categoryId: selectedCategoryId ?? 0, parentFaqId: 0, question: "", answer: "", active: true, keywords: "" });
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : "Không thể xóa FAQ");
    } finally { setSaving(false); }
  }

  // ─── Chat actions ───

  async function openConversation(conversationId: number) {
    setLoading(true); setError("");
    try {
      const detail = await getChatConversation(conversationId);
      setSelectedConversation(detail);
      scrollChatToBottom();
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : "Không thể tải chi tiết cuộc chat");
    } finally { setLoading(false); }
  }

  async function onAssignConversation() {
    if (!selectedConversation) return;
    setSaving(true); setError("");
    try {
      const detail = await assignChatConversation(selectedConversation.id);
      setSelectedConversation(detail);
      await loadConversations();
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : "Không thể nhận cuộc chat");
    } finally { setSaving(false); }
  }

  async function onReplyConversation() {
    if (!selectedConversation || !replyText.trim()) return;
    setSaving(true); setError("");
    try {
      const detail = await replyChatConversation(selectedConversation.id, replyText.trim());
      setSelectedConversation(detail);
      setReplyText("");
      scrollChatToBottom();
      await loadConversations();
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : "Không thể gửi phản hồi");
    } finally { setSaving(false); }
  }

  async function onCloseConversation() {
    if (!selectedConversation) return;
    if (!window.confirm("Đóng cuộc chat này?")) return;
    setSaving(true); setError("");
    try {
      const detail = await closeChatConversation(selectedConversation.id);
      setSelectedConversation(detail);
      await loadConversations();
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : "Không thể đóng cuộc chat");
    } finally { setSaving(false); }
  }

  async function onAddNote() {
    if (!selectedConversation || !noteText.trim()) return;
    setSaving(true); setError("");
    try {
      await addChatNote(selectedConversation.id, noteText.trim());
      setNoteText("");
      await loadNotes();
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : "Không thể lưu ghi chú");
    } finally { setSaving(false); }
  }

  // ─── Notes actions ───

  function openNoteForm(note?: AdminSupportNote) {
    if (note) {
      setEditingNote(note);
      setNoteForm({
        userId: note.userId, content: note.content,
        noteType: note.noteType as NoteType,
        conversationId: note.conversationId ?? 0,
      });
    } else {
      setEditingNote(null);
      setNoteForm({ userId: 0, content: "", noteType: "GENERAL", conversationId: 0 });
    }
    setNoteFormOpen(true);
  }

  async function onSaveNote() {
    setSaving(true); setError("");
    try {
      const payload = {
        userId: noteForm.userId, content: noteForm.content.trim(),
        noteType: noteForm.noteType,
        conversationId: noteForm.conversationId > 0 ? noteForm.conversationId : null,
      };
      if (!payload.content) throw new Error("Nội dung ghi chú là bắt buộc");
      if (editingNote) await updateSupportNote(editingNote.id, payload);
      else await createSupportNote(payload);
      await loadNotes();
      setNoteFormOpen(false);
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : "Không thể lưu ghi chú");
    } finally { setSaving(false); }
  }

  async function onDeleteNote(id: number) {
    if (!window.confirm("Xóa ghi chú này?")) return;
    setSaving(true); setError("");
    try {
      await deleteSupportNote(id);
      await loadNotes();
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : "Không thể xóa ghi chú");
    } finally { setSaving(false); }
  }

  // ─── RENDER ───

  return (
    <AdminShell title="FAQ / Chat CSKH" subtitle="Quản lý cây hỏi đáp nghiệp vụ, luồng chat CSKH và ghi chú hỗ trợ">
      {/* ── Tab bar ── */}
      <div className="mb-4 flex items-center gap-2 rounded-2xl border border-gray-100 bg-white p-1.5 shadow-sm">
        {(["faq", "chat", "notes"] as Tab[]).map((t) => {
          const icons: Record<Tab, string> = { faq: "📋", chat: "💬", notes: "📝" };
          const labels: Record<Tab, string> = { faq: "Quản lý FAQ", chat: "Luồng chat CSKH", notes: "Ghi chú hỗ trợ" };
          return (
            <button
              key={t}
              type="button"
              onClick={() => setTab(t)}
              className={`flex flex-1 items-center justify-center gap-2 rounded-xl px-4 py-2.5 text-sm font-semibold transition-all ${
                tab === t
                  ? "bg-violet-600 text-white shadow-sm"
                  : "text-gray-500 hover:bg-gray-50"
              }`}
            >
              <span>{icons[t]}</span>
              {labels[t]}
            </button>
          );
        })}
      </div>

      {/* ── Error ── */}
      {error ? (
        <div className="mb-3 flex items-center gap-2 rounded-xl border border-red-100 bg-red-50 px-4 py-3 text-sm text-red-700">
          <span className="text-base">⚠️</span>
          <span>{error}</span>
          <button type="button" onClick={() => setError("")} className="ml-auto text-red-400 hover:text-red-600">✕</button>
        </div>
      ) : null}

      {/* ══════════════ FAQ TAB ══════════════ */}
      {tab === "faq" && (
        <div className="grid gap-4 xl:grid-cols-[260px_1fr_1.1fr]">
          {/* Category list */}
          <section className="rounded-2xl border border-gray-100 bg-white p-4 shadow-sm">
            <div className="mb-3 text-xs font-semibold uppercase tracking-wider text-gray-400">Danh mục</div>
            <div className="space-y-1.5">
              {categories.map((item) => (
                <button
                  key={item.id}
                  type="button"
                  onClick={() => setSelectedCategoryId(item.id)}
                  className={`w-full rounded-xl border px-3 py-2.5 text-left transition-all ${
                    selectedCategoryId === item.id
                      ? "border-violet-200 bg-violet-50"
                      : "border-transparent hover:bg-gray-50"
                  }`}
                >
                  <div className="flex items-center justify-between">
                    <span className="text-sm font-semibold text-gray-800">{item.name}</span>
                    <span className={`rounded-full px-2 py-0.5 text-xs font-semibold ${
                      selectedCategoryId === item.id ? "bg-violet-100 text-violet-700" : "bg-gray-100 text-gray-500"
                    }`}>{item.faqCount}</span>
                  </div>
                  <div className="mt-0.5 text-xs text-gray-400">{item.code}</div>
                </button>
              ))}
            </div>

            <div className="mt-4 space-y-2 border-t border-gray-100 pt-4">
              <input
                className="w-full rounded-lg border border-gray-200 px-3 py-2 text-sm outline-none focus:border-violet-400"
                placeholder="Mã danh mục" value={categoryForm.code}
                onChange={(e) => setCategoryForm((p) => ({ ...p, code: e.target.value }))}
              />
              <input
                className="w-full rounded-lg border border-gray-200 px-3 py-2 text-sm outline-none focus:border-violet-400"
                placeholder="Tên danh mục" value={categoryForm.name}
                onChange={(e) => setCategoryForm((p) => ({ ...p, name: e.target.value }))}
              />
              <textarea
                className="w-full rounded-lg border border-gray-200 px-3 py-2 text-sm outline-none focus:border-violet-400"
                placeholder="Mô tả" rows={2} value={categoryForm.description}
                onChange={(e) => setCategoryForm((p) => ({ ...p, description: e.target.value }))}
              />
              <div className="flex items-center gap-2">
                <input
                  className="w-20 rounded-lg border border-gray-200 px-3 py-2 text-sm outline-none"
                  type="number" value={categoryForm.sortOrder}
                  onChange={(e) => setCategoryForm((p) => ({ ...p, sortOrder: Number(e.target.value) }))}
                />
                <label className="flex items-center gap-1.5 text-xs text-gray-600">
                  <input type="checkbox" checked={categoryForm.active}
                    onChange={(e) => setCategoryForm((p) => ({ ...p, active: e.target.checked }))} />
                  Kích hoạt
                </label>
              </div>
              <div className="flex gap-2">
                <button type="button" disabled={saving} onClick={onSaveCategory}
                  className="rounded-lg bg-violet-600 px-3 py-2 text-xs font-semibold text-white disabled:opacity-60">
                  Lưu danh mục
                </button>
                <button type="button" onClick={() => {
                  if (!selectedCategory) return;
                  setCategoryForm({ id: selectedCategory.id, code: selectedCategory.code, name: selectedCategory.name,
                    description: selectedCategory.description ?? "", sortOrder: selectedCategory.sortOrder, active: selectedCategory.active });
                }} className="rounded-lg border border-gray-200 px-3 py-2 text-xs font-semibold text-gray-600 hover:bg-gray-50">
                  Sửa mục chọn
                </button>
              </div>
            </div>
          </section>

          {/* FAQ tree */}
          <section className="rounded-2xl border border-gray-100 bg-white p-4 shadow-sm">
            <div className="mb-3 flex items-center justify-between gap-3">
              <div className="text-xs font-semibold uppercase tracking-wider text-gray-400">Danh sách câu hỏi</div>
              <div className="flex items-center gap-2 rounded-lg border border-gray-200 px-3 py-1.5">
                <span className="text-gray-400 text-sm">🔍</span>
                <input
                  className="w-36 border-none bg-transparent text-sm outline-none placeholder:text-gray-400"
                  placeholder="Tìm câu hỏi..." value={faqSearch}
                  onChange={(e) => setFaqSearch(e.target.value)}
                />
              </div>
            </div>
            <div className="max-h-[560px] space-y-2 overflow-auto pr-1">
              {faqTree.length ? faqTree.map((item) => (
                <FaqTreeRow key={item.id} node={item} level={0} selectedId={selectedFaq?.id ?? null} onSelect={selectFaq} />
              )) : (
                <div className="rounded-xl border border-dashed border-gray-200 p-6 text-center text-sm text-gray-400">
                  Chưa có câu hỏi nào trong danh mục này.
                </div>
              )}
            </div>
          </section>

          {/* FAQ form */}
          <section className="rounded-2xl border border-gray-100 bg-white p-4 shadow-sm">
            <div className="mb-3 text-xs font-semibold uppercase tracking-wider text-gray-400">Chi tiết FAQ</div>
            <div className="space-y-2.5">
              <select
                className="w-full rounded-lg border border-gray-200 px-3 py-2 text-sm outline-none focus:border-violet-400"
                value={faqForm.categoryId || selectedCategoryId || 0}
                onChange={(e) => setFaqForm((p) => ({ ...p, categoryId: Number(e.target.value), parentFaqId: 0 }))}
              >
                <option value={0}>Chọn danh mục</option>
                {categories.map((item) => <option key={item.id} value={item.id}>{item.name}</option>)}
              </select>
              <select
                className="w-full rounded-lg border border-gray-200 px-3 py-2 text-sm outline-none focus:border-violet-400"
                value={faqForm.parentFaqId}
                onChange={(e) => setFaqForm((p) => ({ ...p, parentFaqId: Number(e.target.value) }))}
              >
                <option value={0}>— Câu hỏi gốc (không có cha) —</option>
                {faqParentOptions.map(({ item, level }) => (
                  <option key={item.id} value={item.id}>
                    {`${"— ".repeat(level)}${item.question.length > 60 ? `${item.question.slice(0, 60)}…` : item.question}`}
                  </option>
                ))}
              </select>
              {selectedParentFaq ? (
                <div className="rounded-lg border border-violet-100 bg-violet-50 px-3 py-2 text-xs text-violet-700">
                  FAQ cha: <span className="font-semibold">{selectedParentFaq.question}</span>
                </div>
              ) : null}
              <input
                className="w-full rounded-lg border border-gray-200 px-3 py-2 text-sm outline-none focus:border-violet-400"
                placeholder="Câu hỏi" value={faqForm.question}
                onChange={(e) => setFaqForm((p) => ({ ...p, question: e.target.value }))}
              />
              <textarea
                className="w-full rounded-lg border border-gray-200 px-3 py-2 text-sm outline-none focus:border-violet-400"
                placeholder="Câu trả lời" rows={6} value={faqForm.answer}
                onChange={(e) => setFaqForm((p) => ({ ...p, answer: e.target.value }))}
              />
              <textarea
                className="w-full rounded-lg border border-gray-200 px-3 py-2 text-sm outline-none focus:border-violet-400"
                placeholder="Keywords, ngăn cách bằng dấu phẩy" rows={2} value={faqForm.keywords}
                onChange={(e) => setFaqForm((p) => ({ ...p, keywords: e.target.value }))}
              />
              <label className="flex items-center gap-2 text-xs text-gray-600">
                <input type="checkbox" checked={faqForm.active}
                  onChange={(e) => setFaqForm((p) => ({ ...p, active: e.target.checked }))} />
                Kích hoạt
              </label>
              <div className="flex flex-wrap gap-2 pt-1">
                <button type="button" disabled={saving} onClick={onSaveFaq}
                  className="rounded-lg bg-violet-600 px-4 py-2 text-xs font-semibold text-white disabled:opacity-60">
                  Lưu FAQ
                </button>
                <button type="button" disabled={saving} onClick={onDeleteFaq}
                  className="rounded-lg border border-red-200 px-4 py-2 text-xs font-semibold text-red-600 hover:bg-red-50 disabled:opacity-60">
                  Xóa FAQ
                </button>
                <button type="button" onClick={() => {
                  const parentId = selectedFaq?.id ?? 0;
                  setSelectedFaq(null);
                  setFaqForm({ id: 0, categoryId: selectedCategoryId ?? 0, parentFaqId: parentId, question: "", answer: "", active: true, keywords: "" });
                }} className="rounded-lg border border-gray-200 px-4 py-2 text-xs font-semibold text-gray-600 hover:bg-gray-50">
                  {selectedFaq ? `+ Nhánh con của "${selectedFaq.question.slice(0, 18)}…"` : "Mới (gốc)"}
                </button>
              </div>
            </div>
          </section>
        </div>
      )}

      {/* ══════════════ CHAT TAB ══════════════ */}
      {tab === "chat" && (
        <div className="grid gap-4 xl:grid-cols-[340px_1fr_260px]">
          {/* Queue list */}
          <section className="rounded-2xl border border-gray-100 bg-white shadow-sm">
            <div className="flex items-center justify-between border-b border-gray-100 px-4 py-3">
              <div>
                <div className="text-sm font-semibold text-gray-800">Hàng chờ chat</div>
                <div className="mt-0.5 text-xs text-gray-400">{conversations.length} cuộc hội thoại</div>
              </div>
              <select
                className="rounded-lg border border-gray-200 px-2.5 py-1.5 text-xs outline-none focus:border-violet-400"
                value={chatStatus} onChange={(e) => setChatStatus(e.target.value)}
              >
                <option value="">Tất cả</option>
                <option value="WAITING_AGENT">Đang chờ CSKH</option>
                <option value="IN_PROGRESS">Đang xử lý</option>
                <option value="OPEN">Bot xử lý</option>
                <option value="CLOSED">Đã đóng</option>
              </select>
            </div>
            <div className="max-h-[640px] overflow-auto">
              {conversations.length === 0 ? (
                <div className="p-8 text-center text-sm text-gray-400">Không có cuộc hội thoại nào.</div>
              ) : conversations.map((item) => {
                const cfg = statusCfg(item.status);
                const isSelected = selectedConversation?.id === item.id;
                return (
                  <button key={item.id} type="button" onClick={() => openConversation(item.id)}
                    className={`w-full border-b border-gray-50 px-4 py-3 text-left transition-colors last:border-0 ${
                      isSelected ? "bg-violet-50" : "hover:bg-gray-50"
                    }`}
                  >
                    <div className="flex items-start gap-3">
                      <div className={`mt-0.5 flex h-9 w-9 flex-shrink-0 items-center justify-center rounded-full text-sm font-semibold ${avatarColor(item.id)}`}>
                        {initials(item.userName)}
                      </div>
                      <div className="min-w-0 flex-1">
                        <div className="flex items-center justify-between gap-1">
                          <span className="truncate text-sm font-semibold text-gray-800">{item.userName ?? "Khách hàng"}</span>
                          <span className="flex-shrink-0 text-[10px] text-gray-400">{formatTimeShort(item.startedAt)}</span>
                        </div>
                        <div className="mt-0.5 flex items-center gap-1.5">
                          <span className={`inline-flex items-center gap-1 rounded-full px-2 py-0.5 text-[10px] font-semibold ${cfg.bg} ${cfg.text}`}>
                            <span className={`h-1.5 w-1.5 rounded-full ${cfg.dot}`} />
                            {cfg.label}
                          </span>
                          {item.customerRank ? (
                            <span className="rounded-full bg-amber-50 px-1.5 py-0.5 text-[10px] font-semibold text-amber-700">
                              {item.customerRank}
                            </span>
                          ) : null}
                        </div>
                        <div className="mt-1 truncate text-xs text-gray-500">{item.lastMessagePreview ?? "Chưa có tin nhắn"}</div>
                      </div>
                    </div>
                  </button>
                );
              })}
            </div>
          </section>

          {/* Chat detail */}
          <section className="rounded-2xl border border-gray-100 bg-white shadow-sm">
            {!selectedConversation ? (
              <div className="flex h-[680px] flex-col items-center justify-center gap-3 text-gray-400">
                <span className="text-5xl">💬</span>
                <span className="text-sm">Chọn một cuộc chat để bắt đầu hỗ trợ</span>
              </div>
            ) : (
              <div className="flex h-[680px] flex-col">
                {/* Conv header */}
                <div className="flex items-center justify-between border-b border-gray-100 px-4 py-3">
                  <div className="flex items-center gap-3">
                    <div className={`flex h-9 w-9 flex-shrink-0 items-center justify-center rounded-full text-sm font-semibold ${avatarColor(selectedConversation.id)}`}>
                      {initials(selectedConversation.userName)}
                    </div>
                    <div>
                      <div className="flex items-center gap-2">
                        <span className="text-sm font-semibold text-gray-800">{selectedConversation.userName ?? "Khách hàng"}</span>
                        {selectedConversation.customerRank ? (
                          <span className="rounded-full bg-amber-50 px-2 py-0.5 text-[10px] font-semibold text-amber-700">
                            ♔ {selectedConversation.customerRank}
                          </span>
                        ) : null}
                      </div>
                      <div className="mt-0.5 flex items-center gap-2 text-xs text-gray-400">
                        <span>{selectedConversation.userPhone ?? "-"}</span>
                        <span>·</span>
                        <span className={`font-medium ${statusCfg(selectedConversation.status).text}`}>
                          {statusCfg(selectedConversation.status).label}
                        </span>
                        {selectedConversation.assignedAdminUsername ? (
                          <>
                            <span>·</span>
                            <span>NV: {selectedConversation.assignedAdminUsername}</span>
                          </>
                        ) : null}
                      </div>
                    </div>
                  </div>
                  <div className="flex items-center gap-2">
                    <button type="button" disabled={saving} onClick={onAssignConversation}
                      className="rounded-lg border border-gray-200 px-3 py-1.5 text-xs font-semibold text-gray-700 hover:bg-gray-50 disabled:opacity-60">
                      Nhận xử lý
                    </button>
                    <button type="button" disabled={saving} onClick={onCloseConversation}
                      className="rounded-lg border border-red-200 px-3 py-1.5 text-xs font-semibold text-red-600 hover:bg-red-50 disabled:opacity-60">
                      Đóng chat
                    </button>
                  </div>
                </div>

                {/* Messages */}
                <div ref={chatBodyRef} className="flex-1 space-y-3 overflow-auto bg-gray-50 p-4">
                  {selectedConversation.messages.map((msg) => {
                    const isUser = msg.senderType === "USER";
                    const isAdmin = msg.senderType === "ADMIN";
                    return (
                      <div key={msg.id} className={`flex items-end gap-2 ${isUser || isAdmin ? "flex-row-reverse" : ""}`}>
                        {!isUser && !isAdmin ? (
                          <div className="flex h-7 w-7 flex-shrink-0 items-center justify-center rounded-full bg-violet-100 text-xs">🤖</div>
                        ) : isAdmin ? (
                          <div className="flex h-7 w-7 flex-shrink-0 items-center justify-center rounded-full bg-emerald-100 text-xs font-semibold text-emerald-700">A</div>
                        ) : (
                          <div className={`flex h-7 w-7 flex-shrink-0 items-center justify-center rounded-full text-xs font-semibold ${avatarColor(selectedConversation.id)}`}>
                            {initials(selectedConversation.userName)}
                          </div>
                        )}
                        <div className={`max-w-[78%] rounded-2xl px-3.5 py-2.5 text-sm leading-relaxed ${
                          isUser
                            ? "rounded-br-sm bg-violet-600 text-white"
                            : isAdmin
                            ? "rounded-br-sm bg-emerald-600 text-white"
                            : "rounded-bl-sm border border-gray-100 bg-white text-gray-800"
                        }`}>
                          {isAdmin ? (
                            <div className="mb-1 text-[10px] font-semibold opacity-70">Nhân viên CSKH</div>
                          ) : null}
                          <div>{msg.content}</div>
                          <div className={`mt-1 text-[10px] ${isUser || isAdmin ? "opacity-60" : "text-gray-400"}`}>
                            {formatTimeShort(msg.createdAt)}
                          </div>
                        </div>
                      </div>
                    );
                  })}
                  {agentTyping ? (
                    <div className="flex items-center gap-2">
                      <div className="flex h-7 w-7 items-center justify-center rounded-full bg-gray-100 text-xs">👤</div>
                      <div className="flex gap-1 rounded-2xl rounded-bl-sm border border-gray-100 bg-white px-3.5 py-3">
                        {[0, 1, 2].map((i) => (
                          <span key={i} className="inline-block h-1.5 w-1.5 animate-bounce rounded-full bg-gray-400"
                            style={{ animationDelay: `${i * 150}ms` }} />
                        ))}
                      </div>
                    </div>
                  ) : null}
                </div>

                {/* Reply bar */}
                <div className="border-t border-gray-100 p-3">
                  <div className="flex items-end gap-2">
                    <textarea
                      className="flex-1 resize-none rounded-xl border border-gray-200 px-3.5 py-2.5 text-sm outline-none focus:border-violet-400"
                      placeholder="Nhập phản hồi cho khách hàng..." rows={2}
                      value={replyText} onChange={(e) => setReplyText(e.target.value)}
                      onKeyDown={(e) => { if (e.key === "Enter" && !e.shiftKey) { e.preventDefault(); void onReplyConversation(); }}}
                    />
                    <button type="button" disabled={saving || !replyText.trim()} onClick={onReplyConversation}
                      className="flex h-10 w-10 items-center justify-center rounded-xl bg-violet-600 text-white transition-opacity disabled:opacity-50">
                      ➤
                    </button>
                  </div>
                  <div className="mt-2 flex items-center gap-2">
                    <input
                      className="flex-1 rounded-lg border border-gray-200 px-3 py-2 text-xs outline-none focus:border-violet-400"
                      placeholder="Ghi chú nội bộ..." value={noteText}
                      onChange={(e) => setNoteText(e.target.value)}
                    />
                    <button type="button" disabled={saving || !noteText.trim()} onClick={onAddNote}
                      className="rounded-lg border border-gray-200 px-3 py-2 text-xs font-semibold text-gray-600 hover:bg-gray-50 disabled:opacity-50">
                      Lưu ghi chú
                    </button>
                  </div>
                </div>
              </div>
            )}
          </section>

          {/* Customer info panel */}
          <section className="rounded-2xl border border-gray-100 bg-white shadow-sm">
            {!selectedConversation ? (
              <div className="p-6 text-center text-xs text-gray-400">Chọn cuộc chat để xem thông tin khách hàng.</div>
            ) : (
              <div className="p-4">
                <div className="mb-4 text-xs font-semibold uppercase tracking-wider text-gray-400">Thông tin khách hàng</div>
                <div className="space-y-3">
                  <div>
                    <div className="text-[10px] uppercase tracking-wide text-gray-400">Họ tên</div>
                    <div className="mt-0.5 text-sm font-semibold text-gray-800">{selectedConversation.userName ?? "-"}</div>
                  </div>
                  <div>
                    <div className="text-[10px] uppercase tracking-wide text-gray-400">Số tài khoản</div>
                    <div className="mt-0.5 font-mono text-sm text-gray-800">{selectedConversation.userPhone ?? "-"}</div>
                  </div>
                  {selectedConversation.customerRank ? (
                    <div>
                      <div className="text-[10px] uppercase tracking-wide text-gray-400">Phân hạng</div>
                      <div className="mt-0.5">
                        <span className="rounded-full bg-amber-50 px-2.5 py-1 text-xs font-semibold text-amber-700">
                          ♔ {selectedConversation.customerRank}
                        </span>
                      </div>
                    </div>
                  ) : null}
                  <div>
                    <div className="text-[10px] uppercase tracking-wide text-gray-400">Bắt đầu lúc</div>
                    <div className="mt-0.5 text-xs text-gray-600">{formatTime(selectedConversation.startedAt)}</div>
                  </div>
                  {selectedConversation.escalatedAt ? (
                    <div>
                      <div className="text-[10px] uppercase tracking-wide text-gray-400">Chuyển CSKH lúc</div>
                      <div className="mt-0.5 text-xs text-gray-600">{formatTime(selectedConversation.escalatedAt)}</div>
                    </div>
                  ) : null}
                  {selectedConversation.lastIntent ? (
                    <div>
                      <div className="text-[10px] uppercase tracking-wide text-gray-400">Intent · Confidence</div>
                      <div className="mt-0.5 text-xs text-gray-600">
                        {selectedConversation.lastIntent} · {selectedConversation.lastConfidence ?? 0}%
                      </div>
                    </div>
                  ) : null}
                </div>
              </div>
            )}
          </section>
        </div>
      )}

      {/* ══════════════ NOTES TAB ══════════════ */}
      {tab === "notes" && (
        <div className="rounded-2xl border border-gray-100 bg-white shadow-sm">
          {/* Header */}
          <div className="flex items-center justify-between border-b border-gray-100 px-5 py-4">
            <div>
              <div className="text-base font-semibold text-gray-800">📝 Ghi chú hỗ trợ</div>
              <div className="mt-0.5 text-xs text-gray-400">Quản lý ghi chú tương tác với khách hàng</div>
            </div>
            <button type="button" onClick={() => openNoteForm()}
              className="flex items-center gap-2 rounded-xl bg-violet-600 px-4 py-2.5 text-sm font-semibold text-white shadow-sm hover:bg-violet-700">
              + Thêm ghi chú
            </button>
          </div>

          {/* Stats */}
          <div className="grid grid-cols-5 divide-x divide-gray-100 border-b border-gray-100">
            {[
              { label: "Tổng ghi chú", val: noteStats.total, color: "text-gray-800" },
              { label: "Chung",        val: noteStats.GENERAL,   color: "text-violet-600" },
              { label: "Khiếu nại",   val: noteStats.COMPLAINT,  color: "text-red-600" },
              { label: "Phản hồi",    val: noteStats.FEEDBACK,   color: "text-emerald-600" },
              { label: "Kỹ thuật",    val: noteStats.TECHNICAL,  color: "text-amber-600" },
            ].map((s) => (
              <div key={s.label} className="px-5 py-3 text-center">
                <div className={`text-xl font-bold ${s.color}`}>{s.val}</div>
                <div className="mt-0.5 text-xs text-gray-400">{s.label}</div>
              </div>
            ))}
          </div>

          {/* Search & filter */}
          <div className="flex items-center gap-3 border-b border-gray-100 px-5 py-3">
            <div className="flex flex-1 items-center gap-2 rounded-xl border border-gray-200 px-4 py-2.5">
              <span className="text-gray-400">🔍</span>
              <input
                className="flex-1 border-none bg-transparent text-sm outline-none placeholder:text-gray-400"
                placeholder="Tìm theo tên KH, nội dung ghi chú..."
                value={noteSearch} onChange={(e) => setNoteSearch(e.target.value)}
              />
            </div>
            <select
              className="rounded-xl border border-gray-200 px-3 py-2.5 text-sm outline-none focus:border-violet-400"
              value={noteTypeFilter} onChange={(e) => setNoteTypeFilter(e.target.value)}
            >
              <option value="">Tất cả loại</option>
              {(Object.entries(NOTE_TYPE_CONFIG) as [NoteType, typeof NOTE_TYPE_CONFIG[NoteType]][]).map(([key, cfg]) => (
                <option key={key} value={key}>{cfg.label}</option>
              ))}
            </select>
          </div>

          {/* Notes list */}
          <div className="divide-y divide-gray-50">
            {filteredNotes.length === 0 ? (
              <div className="p-10 text-center text-sm text-gray-400">Không có ghi chú nào.</div>
            ) : filteredNotes.map((note) => {
              const typeCfg = NOTE_TYPE_CONFIG[note.noteType as NoteType] ?? NOTE_TYPE_CONFIG.GENERAL;
              return (
                <div key={note.id} className="flex items-start gap-4 px-5 py-4 hover:bg-gray-50 transition-colors">
                  <div className={`flex h-10 w-10 flex-shrink-0 items-center justify-center rounded-full text-sm font-semibold ${avatarColor(note.userId)}`}>
                    {initials(note.userName)}
                  </div>
                  <div className="min-w-0 flex-1">
                    <div className="flex items-start justify-between gap-3">
                      <div>
                        <span className="text-sm font-semibold text-gray-800">{note.userName ?? "Khách hàng"}</span>
                        <span className="ml-2 text-xs text-gray-400">Mã: {note.userCode ?? "-"}</span>
                      </div>
                      <div className="flex flex-shrink-0 items-center gap-2">
                        <span className={`rounded-full border px-2.5 py-0.5 text-xs font-semibold ${typeCfg.bg} ${typeCfg.text} ${typeCfg.border}`}>
                          {typeCfg.label}
                        </span>
                        <button type="button" onClick={() => openNoteForm(note)}
                          className="flex h-7 w-7 items-center justify-center rounded-lg border border-gray-200 text-gray-400 hover:bg-gray-100 hover:text-gray-600">
                          ✏️
                        </button>
                        <button type="button" onClick={() => onDeleteNote(note.id)}
                          className="flex h-7 w-7 items-center justify-center rounded-lg border border-red-100 text-red-300 hover:bg-red-50 hover:text-red-500">
                          🗑
                        </button>
                      </div>
                    </div>
                    <p className="mt-1.5 text-sm leading-relaxed text-gray-600">{note.content}</p>
                    <div className="mt-2 flex items-center gap-3 text-xs text-gray-400">
                      <span>Tạo bởi: {note.createdByName ?? "-"}</span>
                      <span>·</span>
                      <span>{formatTime(note.createdAt)}</span>
                      {note.conversationId ? (
                        <>
                          <span>·</span>
                          <button type="button" onClick={() => { setTab("chat"); void openConversation(note.conversationId!); }}
                            className="font-medium text-violet-600 hover:underline">
                            Cuộc hội thoại: CHAT{String(note.conversationId).padStart(3, "0")}
                          </button>
                        </>
                      ) : null}
                    </div>
                  </div>
                </div>
              );
            })}
          </div>
        </div>
      )}

      {/* ══ Note form modal ══ */}
      {noteFormOpen ? (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/30 p-4">
          <div className="w-full max-w-md rounded-2xl border border-gray-100 bg-white p-6 shadow-xl">
            <div className="mb-4 flex items-center justify-between">
              <div className="text-base font-semibold text-gray-800">
                {editingNote ? "Chỉnh sửa ghi chú" : "Thêm ghi chú mới"}
              </div>
              <button type="button" onClick={() => setNoteFormOpen(false)} className="text-gray-400 hover:text-gray-600">✕</button>
            </div>
            <div className="space-y-3">
              <div>
                <label className="mb-1 block text-xs font-semibold text-gray-500">Loại ghi chú</label>
                <div className="flex gap-2 flex-wrap">
                  {(Object.entries(NOTE_TYPE_CONFIG) as [NoteType, typeof NOTE_TYPE_CONFIG[NoteType]][]).map(([key, cfg]) => (
                    <button key={key} type="button" onClick={() => setNoteForm((p) => ({ ...p, noteType: key }))}
                      className={`rounded-lg border px-3 py-1.5 text-xs font-semibold transition-all ${
                        noteForm.noteType === key
                          ? `${cfg.bg} ${cfg.text} ${cfg.border} border`
                          : "border-gray-200 text-gray-500 hover:bg-gray-50"
                      }`}>
                      {cfg.label}
                    </button>
                  ))}
                </div>
              </div>
              <div>
                <label className="mb-1 block text-xs font-semibold text-gray-500">Nội dung</label>
                <textarea
                  className="w-full rounded-xl border border-gray-200 px-4 py-3 text-sm outline-none focus:border-violet-400"
                  rows={5} placeholder="Nhập nội dung ghi chú..."
                  value={noteForm.content} onChange={(e) => setNoteForm((p) => ({ ...p, content: e.target.value }))}
                />
              </div>
            </div>
            <div className="mt-5 flex justify-end gap-2">
              <button type="button" onClick={() => setNoteFormOpen(false)}
                className="rounded-xl border border-gray-200 px-4 py-2 text-sm font-semibold text-gray-600 hover:bg-gray-50">
                Hủy
              </button>
              <button type="button" disabled={saving} onClick={onSaveNote}
                className="rounded-xl bg-violet-600 px-5 py-2 text-sm font-semibold text-white hover:bg-violet-700 disabled:opacity-60">
                {saving ? "Đang lưu..." : "Lưu ghi chú"}
              </button>
            </div>
          </div>
        </div>
      ) : null}

      {loading ? (
        <div className="mt-3 flex items-center gap-2 text-xs text-gray-400">
          <div className="h-3 w-3 animate-spin rounded-full border border-violet-300 border-t-violet-600" />
          Đang tải dữ liệu...
        </div>
      ) : null}
    </AdminShell>
  );
}

// ─────────────────────────── FAQ TREE ROW ─────────────────────

function FaqTreeRow({
  node, level, selectedId, onSelect,
}: {
  node: FaqNode; level: number; selectedId: number | null; onSelect: (item: AdminFaqItem) => void;
}) {
  return (
    <div className="space-y-1.5">
      <div className="flex gap-2" style={{ marginLeft: level * 18 }}>
        {level > 0 ? (
          <div className="mt-3 h-auto w-3 flex-shrink-0 border-l border-t border-gray-200 rounded-tl" />
        ) : null}
        <button type="button" onClick={() => onSelect(node)}
          className={`flex-1 rounded-xl border p-3 text-left transition-all ${
            selectedId === node.id
              ? "border-violet-200 bg-violet-50"
              : "border-gray-100 bg-white hover:border-gray-200 hover:bg-gray-50"
          }`}
        >
          <div className="flex items-start justify-between gap-2">
            <div className="min-w-0 flex-1">
              <div className="text-[10px] font-semibold uppercase tracking-wide text-gray-400">
                {level === 0 ? "Câu gốc" : `Nhánh cấp ${level + 1}`}
              </div>
              <div className="mt-0.5 truncate text-sm font-semibold text-gray-800">{node.question}</div>
            </div>
            {node.childCount > 0 ? (
              <span className="flex-shrink-0 rounded-full bg-gray-100 px-2 py-0.5 text-[10px] font-semibold text-gray-500">
                {node.childCount} nhánh
              </span>
            ) : null}
          </div>
          <div className="mt-1.5 line-clamp-2 text-xs text-gray-500 leading-relaxed">{node.answer}</div>
          <div className={`mt-2 inline-flex items-center gap-1 rounded-full px-2 py-0.5 text-[10px] font-semibold ${
            node.active ? "bg-emerald-50 text-emerald-600" : "bg-gray-100 text-gray-400"
          }`}>
            {node.active ? "● Hoạt động" : "○ Tắt"}
          </div>
        </button>
      </div>
      {node.children.map((child) => (
        <FaqTreeRow key={child.id} node={child} level={level + 1} selectedId={selectedId} onSelect={onSelect} />
      ))}
    </div>
  );
}