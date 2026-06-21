import { useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useAuth } from "@/app/auth-context";
import {
  getAdminUsers,
  grantAdminRole,
  revokeAdminRole,
} from "@/shared/api/backend-api";
import { ApiError } from "@/shared/api/http";
import type { AdminUserResponse } from "@/shared/api/types";

function RoleBadge({ role }: { role: string }) {
  const isAdmin = role === "ADMIN";
  return (
    <span
      className={`inline-flex items-center rounded px-1.5 py-0.5 text-[10px] font-semibold uppercase tracking-wide ${
        isAdmin
          ? "bg-amber-400/15 text-amber-400"
          : "bg-white/10 text-zinc-400"
      }`}
    >
      {role}
    </span>
  );
}

function fmt(iso: string) {
  try {
    return new Date(iso).toLocaleDateString("ru-RU", {
      day: "2-digit",
      month: "2-digit",
      year: "numeric",
    });
  } catch {
    return iso;
  }
}

export function AdminUsersPage() {
  const { token } = useAuth();
  const qc = useQueryClient();
  const [actionErr, setActionErr] = useState<string | null>(null);
  const [actionMsg, setActionMsg] = useState<string | null>(null);

  const { data: users = [], isLoading, error } = useQuery({
    queryKey: ["admin-users"],
    queryFn: () => getAdminUsers(token!),
    enabled: !!token,
  });

  const grantMut = useMutation({
    mutationFn: (email: string) => grantAdminRole(email, token!),
    onSuccess: (res) => {
      void qc.invalidateQueries({ queryKey: ["admin-users"] });
      setActionErr(null);
      setActionMsg(`Роль ADMIN выдана: ${res.email}`);
    },
    onError: (e) => {
      setActionMsg(null);
      setActionErr(e instanceof ApiError ? e.message : (e as Error).message);
    },
  });

  const revokeMut = useMutation({
    mutationFn: (email: string) => revokeAdminRole(email, token!),
    onSuccess: (res) => {
      void qc.invalidateQueries({ queryKey: ["admin-users"] });
      setActionErr(null);
      setActionMsg(`ADMIN снят: ${res.email}`);
    },
    onError: (e) => {
      setActionMsg(null);
      setActionErr(e instanceof ApiError ? e.message : (e as Error).message);
    },
  });

  const busy = grantMut.isPending || revokeMut.isPending;

  function handleGrant(u: AdminUserResponse) {
    setActionErr(null);
    setActionMsg(null);
    grantMut.mutate(u.email);
  }

  function handleRevoke(u: AdminUserResponse) {
    setActionErr(null);
    setActionMsg(null);
    if (
      !window.confirm(
        `Снять роль ADMIN у ${u.email}? Убедитесь, что это не последний администратор.`,
      )
    )
      return;
    revokeMut.mutate(u.email);
  }

  return (
    <div>
      <h1 className="mb-1 text-2xl font-bold text-zinc-100">Пользователи</h1>
      <p className="mb-6 text-sm text-zinc-500">
        Все зарегистрированные аккаунты. Здесь можно выдать или снять роль ADMIN.
      </p>

      {actionErr && (
        <p className="mb-4 rounded-[8px] border border-red-400/20 bg-red-400/10 px-4 py-3 text-sm text-red-400">
          {actionErr}
        </p>
      )}
      {actionMsg && (
        <p className="mb-4 rounded-[8px] border border-emerald-400/20 bg-emerald-400/10 px-4 py-3 text-sm text-emerald-400">
          {actionMsg}
        </p>
      )}

      {isLoading && <p className="text-sm text-zinc-400">Загрузка…</p>}
      {error && (
        <p className="text-sm text-red-400">
          {error instanceof Error ? error.message : "Ошибка загрузки"}
        </p>
      )}

      {users.length > 0 && (
        <>
          <p className="mb-3 text-xs text-zinc-500">{users.length} пользователей</p>
          <div className="overflow-x-auto rounded-[12px] border border-white/10">
            <table className="w-full min-w-[640px] text-sm">
              <thead>
                <tr className="border-b border-white/10 text-left text-xs text-zinc-500">
                  <th className="px-4 py-3">ID</th>
                  <th className="px-4 py-3">Email</th>
                  <th className="px-4 py-3">Роли</th>
                  <th className="px-4 py-3">Зарегистрирован</th>
                  <th className="px-4 py-3">Действия</th>
                </tr>
              </thead>
              <tbody>
                {users.map((u) => {
                  const isAdmin = u.roles.includes("ADMIN");
                  return (
                    <tr
                      key={u.id}
                      className="border-b border-white/5 hover:bg-white/[0.02]"
                    >
                      <td className="px-4 py-3 font-mono text-zinc-500">
                        {u.id}
                      </td>
                      <td className="px-4 py-3 font-medium text-zinc-100">
                        {u.email}
                      </td>
                      <td className="px-4 py-3">
                        <div className="flex flex-wrap gap-1">
                          {u.roles.map((r) => (
                            <RoleBadge key={r} role={r} />
                          ))}
                        </div>
                      </td>
                      <td className="px-4 py-3 text-zinc-400">
                        {fmt(u.createdAt)}
                      </td>
                      <td className="px-4 py-3">
                        {isAdmin ? (
                          <button
                            type="button"
                            disabled={busy}
                            onClick={() => handleRevoke(u)}
                            className="text-xs font-semibold text-red-400 hover:text-red-300 disabled:opacity-40"
                          >
                            Снять ADMIN
                          </button>
                        ) : (
                          <button
                            type="button"
                            disabled={busy}
                            onClick={() => handleGrant(u)}
                            className="text-xs font-semibold text-amber-400 hover:text-amber-300 disabled:opacity-40"
                          >
                            Выдать ADMIN
                          </button>
                        )}
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        </>
      )}
    </div>
  );
}
