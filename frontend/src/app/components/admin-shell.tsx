"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { useEffect, useState } from "react";
import { backendWsUrl, MiniStompClient } from "../../lib/stomp-client";

type AdminUser = {
  id: number;
  type: string;
  username?: string | null;
  roles: string[];
  permissions?: string[];
};

type NavItem = {
  label: string;
  href: string;
  isDropdown?: boolean;
  requiredRoles?: string[];
  requiredPermissions?: string[];
  subItems?: {
    label: string;
    href: string;
    requiredRoles?: string[];
    requiredPermissions?: string[];
  }[];
};

type AdminNotification = {
  id?: number;
  title?: string;
  content?: string;
  type?: string;
  createdAt?: string;
};

const ADMIN_ONLY = ["ADMIN", "SUPER_ADMIN"];
const STAFF_ACCESS = ["ADMIN", "SUPER_ADMIN", "STAFF"];
const SERVICE_OFFICER_ACCESS = ["ADMIN", "SUPER_ADMIN", "SERVICE_OFFICER"];

const PERMISSION_DASHBOARD = ["Xem dashboard", "Xem dash board"];
const PERMISSION_KYC_VIEW = ["Xem hồ sơ KYC", "Xem ho so KYC"];
const PERMISSION_SERVICE_REQUEST_VIEW = [
  "Xem yêu cầu dịch vụ",
  "Xem yeu cau dich vu",
];
const PERMISSION_SAVING_APPROVAL = [
  "SAVING_APPROVAL",
  "Duyệt tiết kiệm",
  "Duyet tiet kiem",
];
const PERMISSION_LOAN_APPROVAL = [
  "LOAN_APPLICATION_APPROVAL",
  "Duyệt vay",
  "Duyệt vay vốn",
  "Duyet vay",
  "Duyet vay von",
];
const PERMISSION_SYSTEM_LOG = ["Xem log hệ thống", "Xem log he thong"];

function normalizePermission(value: string) {
  return value
    .normalize("NFD")
    .replace(/[\u0300-\u036f]/g, "")
    .toLowerCase()
    .trim();
}

function hasAccess(user: AdminUser | null, item: Pick<NavItem, "requiredRoles" | "requiredPermissions">) {
  if (!user) return true;
  const roles = new Set((user.roles ?? []).map((role) => role.toUpperCase()));
  if (roles.has("ADMIN") || roles.has("SUPER_ADMIN")) return true;

  if (item.requiredRoles && item.requiredRoles.some((role) => roles.has(role.toUpperCase()))) {
    return true;
  }

  if (item.requiredPermissions && item.requiredPermissions.length > 0) {
    const permissions = new Set((user.permissions ?? []).map(normalizePermission));
    return item.requiredPermissions.some((permission) =>
      permissions.has(normalizePermission(permission))
    );
  }

  return !item.requiredRoles && !item.requiredPermissions;
}

const navItems: NavItem[] = [
  {
    label: "Dashboard",
    href: "/",
    requiredRoles: ADMIN_ONLY,
    requiredPermissions: PERMISSION_DASHBOARD,
  },
  {
    label: "Quan ly khach hang",
    href: "#",
    isDropdown: true,
    subItems: [
      {
        label: "Danh sach khach hang",
        href: "/customers",
        requiredRoles: ADMIN_ONLY,
      },
      {
        label: "Kiem duyet KYC",
        href: "/customers/kyc",
        requiredRoles: ADMIN_ONLY,
        requiredPermissions: PERMISSION_KYC_VIEW,
      },
      {
        label: "Tai lieu khach hang",
        href: "/customers/documents",
        requiredRoles: ADMIN_ONLY,
      },
    ],
  },
  {
    label: "Tai khoan & Giao dich",
    href: "#",
    isDropdown: true,
    subItems: [
      {
        label: "Phan loai giao dich",
        href: "/transactions/classification",
        requiredRoles: ADMIN_ONLY,
      },
      {
        label: "GD lon cho duyet",
        href: "/transactions/large-approval",
        requiredRoles: ADMIN_ONLY,
      },
      {
        label: "Tai khoan ngan hang",
        href: "/transactions/bank-accounts",
        requiredRoles: ADMIN_ONLY,
      },
      {
        label: "Giao dich",
        href: "/transactions/list",
        requiredRoles: ADMIN_ONLY,
      },
      {
        label: "Bien dong so du",
        href: "/transactions/balance-fluctuations",
        requiredRoles: ADMIN_ONLY,
      },
    ],
  },
  {
    label: "San pham tai chinh",
    href: "#",
    isDropdown: true,
    subItems: [
      {
        label: "San pham tiet kiem",
        href: "/financial-products/savings",
        requiredRoles: STAFF_ACCESS,
      },
      {
        label: "Bac lai suat tiet kiem",
        href: "/financial-products/savings/tiers",
        requiredRoles: STAFF_ACCESS,
      },
      {
        label: "So tiet kiem",
        href: "/financial-products/savings/accounts",
        requiredRoles: SERVICE_OFFICER_ACCESS,
      },
      {
        label: "San pham vay",
        href: "/financial-products/loans",
        requiredRoles: STAFF_ACCESS,
      },
      {
        label: "Bac lai suat vay",
        href: "/financial-products/loans/tiers",
        requiredRoles: STAFF_ACCESS,
      },
      {
        label: "Yeu cau tat toan so",
        href: "/financial-products/savings/closure-requests",
        requiredRoles: STAFF_ACCESS,
      },
      {
        label: "Ho so vay",
        href: "/financial-products/loans/applications",
        requiredRoles: STAFF_ACCESS,
      },
      {
        label: "Khoan vay",
        href: "/financial-products/loans/contracts",
        requiredRoles: STAFF_ACCESS,
      },
      {
        label: "Lich tra no",
        href: "/financial-products/loans/repayments",
        requiredRoles: STAFF_ACCESS,
      },
    ],
  },
  {
    label: "Yeu cau thu tuc",
    href: "#",
    isDropdown: true,
    subItems: [
      {
        label: "Tat ca yeu cau",
        href: "/requests",
        requiredRoles: ADMIN_ONLY,
        requiredPermissions: PERMISSION_SERVICE_REQUEST_VIEW,
      },
      {
        label: "Yeu cau nang han muc",
        href: "/requests/limits",
        requiredRoles: ADMIN_ONLY,
        requiredPermissions: PERMISSION_SERVICE_REQUEST_VIEW,
      },
      {
        label: "Yeu cau doi thong tin",
        href: "/requests/profile",
        requiredRoles: ADMIN_ONLY,
        requiredPermissions: PERMISSION_SERVICE_REQUEST_VIEW,
      },
      {
        label: "Duyet so tiet kiem",
        href: "/requests/savings",
        requiredRoles: ADMIN_ONLY,
        requiredPermissions: PERMISSION_SAVING_APPROVAL,
      },
      {
        label: "Duyet vay von",
        href: "/requests/loans",
        requiredRoles: ADMIN_ONLY,
        requiredPermissions: PERMISSION_LOAN_APPROVAL,
      },
    ],
  },
  {
    label: "Hợp đồng & thỏa thuận",
    href: "#",
    isDropdown: true,
    subItems: [
      {
        label: "Template hợp đồng & thỏa thuận",
        href: "/contracts",
        requiredRoles: STAFF_ACCESS,
      },
      {
        label: "Tài liệu đã sinh",
        href: "/contracts/list",
        requiredRoles: STAFF_ACCESS,
      },
    ],
  },
  {
    label: "Quan tri he thong",
    href: "#",
    isDropdown: true,
    subItems: [
      {
        label: "Nhan vien",
        href: "/staff",
        requiredRoles: ADMIN_ONLY,
      },
      {
        label: "Vai tro",
        href: "/system/roles",
        requiredRoles: ADMIN_ONLY,
      },
      {
        label: "FAQ / Chat CSKH",
        href: "/system/chatbot",
        requiredRoles: ADMIN_ONLY,
      },
      {
        label: "Muc duyet nghiep vu",
        href: "/system/approval-policies",
        requiredRoles: ADMIN_ONLY,
      },
      {
        label: "Nhat ky he thong",
        href: "/system/audit",
        requiredRoles: ADMIN_ONLY,
        requiredPermissions: PERMISSION_SYSTEM_LOG,
      },
    ],
  },
];

type AdminShellProps = {
  title: string;
  subtitle?: string;
  actions?: React.ReactNode;
  children: React.ReactNode;
  onLogout?: () => void;
};

export default function AdminShell({
  title,
  subtitle,
  actions,
  children,
  onLogout,
}: AdminShellProps) {
  const pathname = usePathname();
  const [menuOpen, setMenuOpen] = useState(false);
  const [notificationOpen, setNotificationOpen] = useState(false);
  const [notifications, setNotifications] = useState<AdminNotification[]>([]);
  const [user, setUser] = useState<AdminUser | null>(null);

  useEffect(() => {
    const u = localStorage.getItem("adminUser");
    if (u) {
      try {
        setUser(JSON.parse(u));
      } catch {
        setUser(null);
      }
    }
  }, []);

  useEffect(() => {
    const client = new MiniStompClient(backendWsUrl());
    client.connect();
    const unsub = client.subscribe("/topic/admin/notifications", (body) => {
      const item = typeof body === "object" && body ? (body as AdminNotification) : { title: "Thông báo", content: String(body ?? "") };
      setNotifications((prev) => [item, ...prev].slice(0, 20));
    });
    return () => {
      unsub();
      client.disconnect();
    };
  }, []);

  const handleLogout = () => {
    localStorage.removeItem("adminToken");
    localStorage.removeItem("adminUser");
    if (onLogout) {
      onLogout();
      return;
    }
    window.location.href = "/login";
  };

  return (
    <div className="flex min-h-screen w-full bg-[#f6f7fb] text-[#111827]">
      <aside className="hidden w-64 flex-col border-r border-black/5 bg-white p-5 lg:flex">
        <div className="flex items-center gap-2">
          <div className="flex h-9 w-9 items-center justify-center rounded-xl bg-blue-600 text-sm font-semibold text-white">
            MB
          </div>
          <div>
            <div className="text-sm font-semibold">Admin Dashboard</div>
            <div className="text-xs text-zinc-500">He thong quan tri noi bo</div>
          </div>
        </div>

        {navItems.map((item) => {
          const active =
            pathname === item.href ||
            item.subItems?.some((sub) => pathname === sub.href);

          if (item.isDropdown) {
            const visibleSubItems = item.subItems?.filter((sub) => hasAccess(user, sub)) ?? [];
            if (!visibleSubItems.length) return null;
            return (
              <div key={item.label} className="flex flex-col gap-1">
                <div className="flex items-center justify-between px-3 py-2 text-sm font-bold text-zinc-800">
                  <span>{item.label}</span>
                  <span className="text-[10px] opacity-40">▼</span>
                </div>

                <div className="ml-2 flex flex-col gap-1 border-l border-zinc-100 pl-3">
                  {visibleSubItems.map((sub) => (
                    <Link
                      key={sub.label}
                      href={sub.href}
                      className={`block rounded-lg px-3 py-2 text-sm transition ${pathname === sub.href
                        ? "bg-blue-50 text-blue-700 font-bold"
                        : "text-zinc-500 hover:text-zinc-800 hover:bg-zinc-50"
                        }`}
                    >
                      {sub.label}
                    </Link>
                  ))}
                </div>
              </div>
            );
          }

          if (!hasAccess(user, item)) return null;

          return (
            <Link
              key={item.label}
              href={item.href}
              className={`block rounded-xl px-3 py-2 text-sm transition ${active
                ? "bg-blue-50 text-blue-700 font-bold"
                : "text-zinc-600 hover:bg-zinc-50 hover:text-zinc-800"
                }`}
            >
              {item.label}
            </Link>
          );
        })}

        <div className="rounded-2xl border border-black/5 bg-gradient-to-br from-blue-600 to-indigo-600 p-4 text-white">
          <div className="text-sm font-semibold">Bao cao thang</div>
          <div className="mt-2 text-xs text-white/80">
            Tong quan giao dich va hieu suat he thong.
          </div>
          <button className="mt-4 h-9 w-full rounded-lg bg-white/15 text-xs font-medium">
            Xem chi tiet
          </button>
        </div>
      </aside>

      <div className="flex min-h-screen flex-1 flex-col">
        <header className="flex flex-wrap items-center justify-between gap-4 border-b border-black/5 bg-white px-6 py-4">
          <div>
            <div className="text-lg font-semibold">{title}</div>
            {subtitle ? (
              <div className="text-xs text-zinc-500">{subtitle}</div>
            ) : null}
          </div>
          <div className="flex items-center gap-3">
            {actions}
            <div className="relative">
              <button
                className="relative flex h-9 w-9 items-center justify-center rounded-full border border-black/10 bg-white text-sm"
                onClick={() => setNotificationOpen((prev) => !prev)}
                type="button"
              >
                🔔
                {notifications.length > 0 ? (
                  <span className="absolute -right-1 -top-1 rounded-full bg-red-600 px-1.5 py-0.5 text-[9px] font-bold text-white">
                    {notifications.length}
                  </span>
                ) : null}
              </button>
              {notificationOpen ? (
                <div className="absolute right-0 z-50 mt-2 w-80 overflow-hidden rounded-xl border border-black/10 bg-white text-sm shadow-lg">
                  <div className="border-b border-black/5 px-4 py-3 text-xs font-bold uppercase text-zinc-500">Thông báo realtime</div>
                  {notifications.length === 0 ? (
                    <div className="px-4 py-5 text-xs text-zinc-400">Chưa có thông báo mới trong phiên này.</div>
                  ) : notifications.map((item, index) => (
                    <div key={`${item.id ?? "n"}-${index}`} className="border-b border-black/5 px-4 py-3 last:border-b-0">
                      <div className="text-xs font-bold text-zinc-900">{item.title ?? item.type ?? "Thông báo"}</div>
                      <div className="mt-1 line-clamp-2 text-xs text-zinc-500">{item.content ?? "Có cập nhật mới từ hệ thống"}</div>
                    </div>
                  ))}
                </div>
              ) : null}
            </div>
            <div className="relative">
              <button
                className="flex items-center gap-3 rounded-full border border-black/10 bg-white px-3 py-1.5"
                onClick={() => setMenuOpen((prev) => !prev)}
                type="button"
              >
                <div className="flex h-8 w-8 items-center justify-center rounded-full bg-blue-600 text-xs font-semibold text-white">
                  {user?.username?.[0]?.toUpperCase() ?? "TB"}
                </div>
                <div className="text-xs text-left">
                  <div className="font-semibold">{user?.username ?? "Tran Van B"}</div>
                  <div className="text-zinc-500">Toan quyen</div>
                </div>
                <span className="text-xs text-zinc-400">▾</span>
              </button>

              {menuOpen ? (
                <div className="absolute right-0 mt-2 w-52 overflow-hidden rounded-xl border border-black/10 bg-white text-sm shadow-lg">
                  <Link
                    className="flex items-center gap-2 px-4 py-2 text-zinc-700 hover:bg-zinc-50"
                    href="/profile"
                    onClick={() => setMenuOpen(false)}
                  >
                    Thong tin ca nhan
                  </Link>
                  <Link
                    className="flex items-center gap-2 px-4 py-2 text-zinc-700 hover:bg-zinc-50"
                    href="/change-password"
                    onClick={() => setMenuOpen(false)}
                  >
                    Doi mat khau
                  </Link>
                  <button
                    className="flex w-full items-center gap-2 px-4 py-2 text-left text-red-600 hover:bg-red-50"
                    onClick={handleLogout}
                    type="button"
                  >
                    Dang xuat
                  </button>
                </div>
              ) : null}
            </div>
          </div>
        </header>

        <main className="flex flex-1 flex-col gap-6 px-6 py-6">{children}</main>
      </div>
    </div>
  );
}
