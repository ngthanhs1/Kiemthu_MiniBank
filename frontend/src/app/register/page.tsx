"use client";

import { useRouter } from "next/navigation";

export default function AdminRegisterPage() {
  const router = useRouter();

  return (
    <div className="flex flex-1 items-center justify-center bg-background px-6">
      <div className="w-full max-w-md rounded-2xl border border-black/[.08] bg-background p-6 dark:border-white/[.145]">
        <h1 className="text-2xl font-semibold tracking-tight">Admin Register</h1>
        <p className="mt-2 text-sm text-zinc-600 dark:text-zinc-400">
          Admin accounts can only be created by existing system admins.
        </p>

        <div className="mt-6 rounded-xl border border-black/[.08] bg-zinc-50 p-4 text-sm text-zinc-700 dark:border-white/[.145] dark:bg-black dark:text-zinc-200">
          Please contact your admin to receive access.
        </div>

        <button
          type="button"
          className="mt-6 h-11 w-full rounded-lg border border-black/[.08] bg-background text-foreground dark:border-white/[.145]"
          onClick={() => router.push("/login")}
        >
          Back to login
        </button>
      </div>
    </div>
  );
}
