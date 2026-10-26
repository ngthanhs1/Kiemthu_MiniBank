"use client";

import {
  Shield,
  Pencil,
  ArrowLeft,
  Check,
  X,
  Plus,
} from "lucide-react";
import Link from "next/link";
import { useState } from "react";

type Permission = {
  module: string;
  view: boolean;
  create: boolean;
  edit: boolean;
  delete: boolean;
};

type Role = {
  title: string;
  sub: string;
  desc: string;
  users: number;
  gradient: string;
  permissions: Permission[];
};

export default function RolesPage() {
  const [roles, setRoles] = useState<Role[]>([
    {
      title: "Quản trị viên",
      sub: "Admin",
      desc: "Toàn quyền trên hệ thống",
      users: 3,
      gradient: "from-[#ff3b3b] to-[#ff0000]",
      permissions: [
        { module: "Khách hàng", view: true, create: true, edit: true, delete: true },
        { module: "Tài khoản", view: true, create: true, edit: true, delete: true },
        { module: "Giao dịch", view: true, create: true, edit: true, delete: true },
        { module: "Sản phẩm", view: true, create: true, edit: true, delete: true },
        { module: "Nhân viên", view: true, create: true, edit: true, delete: true },
      ],
    },
    {
      title: "Quản lý",
      sub: "Manager",
      desc: "Duyệt nghiệp vụ và quản lý nhân viên",
      users: 8,
      gradient: "from-[#ff7a00] to-[#ff5c00]",
      permissions: [
        { module: "Khách hàng", view: true, create: false, edit: true, delete: false },
        { module: "Tài khoản", view: true, create: false, edit: true, delete: false },
        { module: "Giao dịch", view: true, create: false, edit: true, delete: false },
        { module: "Sản phẩm", view: true, create: false, edit: false, delete: false },
        { module: "Nhân viên", view: true, create: false, edit: true, delete: false },
      ],
    },
    {
      title: "Nhân viên",
      sub: "Staff",
      desc: "Xử lý hồ sơ và hỗ trợ khách hàng",
      users: 25,
      gradient: "from-[#3b82f6] to-[#2563eb]",
      permissions: [
        { module: "Khách hàng", view: true, create: false, edit: false, delete: false },
        { module: "Tài khoản", view: true, create: false, edit: false, delete: false },
        { module: "Giao dịch", view: true, create: false, edit: false, delete: false },
        { module: "Sản phẩm", view: true, create: false, edit: false, delete: false },
        { module: "Nhân viên", view: false, create: false, edit: false, delete: false },
      ],
    },
  ]);

  const [selectedRole, setSelectedRole] = useState<Role>(roles[0]);
  const [showModal, setShowModal] = useState(false);

  const [newRole, setNewRole] = useState({
    title: "",
    sub: "",
    desc: "",
  });

  const handleCreateRole = () => {
    if (!newRole.title || !newRole.sub || !newRole.desc) return;

    const role: Role = {
      ...newRole,
      users: 0,
      gradient: "from-[#7c3aed] to-[#6d28d9]",
      permissions: [
        { module: "Khách hàng", view: false, create: false, edit: false, delete: false },
        { module: "Tài khoản", view: false, create: false, edit: false, delete: false },
        { module: "Giao dịch", view: false, create: false, edit: false, delete: false },
        { module: "Sản phẩm", view: false, create: false, edit: false, delete: false },
        { module: "Nhân viên", view: false, create: false, edit: false, delete: false },
      ],
    };

    const updatedRoles = [...roles, role];
    setRoles(updatedRoles);
    setSelectedRole(role);

    setNewRole({
      title: "",
      sub: "",
      desc: "",
    });

    setShowModal(false);
  };

  return (
    <div className="min-h-screen bg-gray-50 p-6">
      <div className="mx-auto max-w-6xl">

        {/* HEADER */}
        <div className="mb-6 flex items-center justify-between">
          <div>
            <div className="flex items-center gap-3">
              <Link
                href="/"
                className="flex items-center gap-1 text-gray-600 transition hover:text-black"
              >
                <ArrowLeft size={18} />
              </Link>

              <Shield className="text-purple-600" />

              <h1 className="text-2xl font-bold text-black">
                Quản lý vai trò
              </h1>
            </div>

            <p className="ml-7 mt-1 text-sm text-gray-500">
              Cấu hình vai trò và phân quyền người dùng
            </p>
          </div>

          <button
            onClick={() => setShowModal(true)}
            className="flex items-center gap-2 rounded-xl bg-gradient-to-r from-purple-500 to-purple-700 px-5 py-2 text-white shadow transition hover:scale-[1.02]"
          >
            <Plus size={18} />
            Tạo vai trò
          </button>
        </div>

        {/* GRID */}
        <div className="mb-10 grid grid-cols-1 gap-6 md:grid-cols-3">
          {roles.map((role) => (
            <button
              key={role.title}
              onClick={() => setSelectedRole(role)}
              className={`overflow-hidden rounded-2xl border bg-white text-left shadow transition ${selectedRole.title === role.title
                ? "ring-2 ring-purple-500 scale-[1.02]"
                : "hover:shadow-md"
                }`}
            >
              <div
                className={`bg-gradient-to-r ${role.gradient} p-5 text-white`}
              >
                <div className="flex items-start justify-between">
                  <Shield size={20} />

                  <div className="rounded-lg bg-white/20 p-2">
                    <Pencil size={16} />
                  </div>
                </div>

                <div className="mt-4">
                  <h2 className="text-lg font-semibold">
                    {role.title}
                  </h2>
                  <p className="text-sm opacity-90">{role.sub}</p>
                </div>
              </div>

              <div className="p-5 text-sm text-gray-600">
                <p>{role.desc}</p>

                <div className="mt-4 flex justify-between">
                  <span>Số người dùng:</span>
                  <span className="font-semibold text-black">
                    {role.users}
                  </span>
                </div>
              </div>
            </button>
          ))}
        </div>

        {/* TABLE */}
        <div className="overflow-hidden rounded-2xl border bg-white shadow">
          <div className="border-b p-6">
            <h2 className="text-2xl font-bold text-black">
              Chi tiết phân quyền - {selectedRole.title}
            </h2>
          </div>

          <table className="w-full border-collapse">
            <thead>
              <tr className="border-b bg-gray-50">
                <th className="p-4 text-left text-lg font-semibold text-gray-800">
                  Module
                </th>
                <th className="p-4 text-center text-lg font-semibold text-gray-800">
                  Xem
                </th>
                <th className="p-4 text-center text-lg font-semibold text-gray-800">
                  Tạo
                </th>
                <th className="p-4 text-center text-lg font-semibold text-gray-800">
                  Sửa
                </th>
                <th className="p-4 text-center text-lg font-semibold text-gray-800">
                  Xóa
                </th>
              </tr>
            </thead>

            <tbody>
              {selectedRole.permissions.map((item, index) => (
                <tr
                  key={item.module}
                  className={`border-b transition hover:bg-gray-50 ${index % 2 === 0 ? "bg-white" : "bg-gray-50/30"
                    }`}
                >
                  <td className="p-4 text-base font-semibold text-gray-900">
                    {item.module}
                  </td>

                  {["view", "create", "edit", "delete"].map((key) => (
                    <td
                      key={key}
                      className="p-4 text-center"
                    >
                      {item[key as keyof Permission] ? (
                        <Check
                          className="mx-auto text-green-600"
                          size={20}
                          strokeWidth={3}
                        />
                      ) : (
                        <span className="text-lg font-bold text-gray-400">
                          —
                        </span>
                      )}
                    </td>
                  ))}
                </tr>
              ))}
            </tbody>
          </table>
        </div>

        {/* MODAL */}
        {showModal && (
          <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40">
            <div className="w-full max-w-md rounded-2xl bg-white p-6 shadow-xl">
              <div className="mb-5 flex items-center justify-between">
                <h2 className="text-xl font-bold text-black">
                  Tạo vai trò mới
                </h2>

                <button
                  onClick={() => setShowModal(false)}
                  className="text-gray-500 hover:text-black"
                >
                  <X />
                </button>
              </div>

              <div className="space-y-4">
                <input
                  type="text"
                  placeholder="Tên vai trò"
                  value={newRole.title}
                  onChange={(e) =>
                    setNewRole({
                      ...newRole,
                      title: e.target.value,
                    })
                  }
                  className="w-full rounded-xl border px-4 py-3 text-black placeholder:text-gray-400 focus:border-purple-500 focus:outline-none"
                />

                <input
                  type="text"
                  placeholder="Tên tiếng Anh (VD: Supervisor)"
                  value={newRole.sub}
                  onChange={(e) =>
                    setNewRole({
                      ...newRole,
                      sub: e.target.value,
                    })
                  }
                  className="w-full rounded-xl border px-4 py-3 text-black placeholder:text-gray-400 focus:border-purple-500 focus:outline-none"
                />

                <textarea
                  placeholder="Mô tả vai trò"
                  value={newRole.desc}
                  onChange={(e) =>
                    setNewRole({
                      ...newRole,
                      desc: e.target.value,
                    })
                  }
                  className="w-full rounded-xl border px-4 py-3 text-black placeholder:text-gray-400 focus:border-purple-500 focus:outline-none"
                />

                <button
                  onClick={handleCreateRole}
                  className="w-full rounded-xl bg-purple-600 py-3 font-semibold text-white transition hover:bg-purple-700"
                >
                  Tạo vai trò
                </button>
              </div>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}
