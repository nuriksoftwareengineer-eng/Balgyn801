import { useEffect, useState } from "react";
import { Link, useLocation, useNavigate } from "react-router-dom";
import { useAuth } from "@/app/auth-context";
import { ApiError } from "@/shared/api/http";
import { Button } from "@/shared/ui/button";

export function LoginPage() {
  const { login, user, loading } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const from =
    (location.state as { from?: string } | null)?.from ?? undefined;

  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [afterAuth, setAfterAuth] = useState(false);

  useEffect(() => {
    if (!afterAuth || loading) return;
    if (!user) return;
    let dest = from ?? "/";
    if (dest.startsWith("/admin") && !user.roles.includes("ADMIN")) {
      dest = "/";
    }
    navigate(dest, { replace: true });
    queueMicrotask(() => setAfterAuth(false));
  }, [afterAuth, loading, user, from, navigate]);

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setError(null);
    setSubmitting(true);
    try {
      await login(email.trim(), password);
      setAfterAuth(true);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Не удалось войти");
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <div className="w-full max-w-[400px] rounded-[14px] border border-white/10 bg-zinc-900/80 p-8 shadow-[0_24px_80px_-32px_rgba(139,92,246,0.35)] backdrop-blur-md">
      <h1 className="font-display mb-2 text-3xl tracking-wide text-zinc-100">
        Вход
      </h1>
      <p className="mb-6 text-sm text-zinc-500">
        Админка доступна только пользователям с ролью ADMIN.
      </p>

      <form onSubmit={handleSubmit} className="flex flex-col gap-4">
        <label className="flex flex-col gap-1.5 text-sm">
          <span className="font-medium text-zinc-400">Email</span>
          <input
            type="email"
            autoComplete="email"
            required
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            className="rounded-[10px] border border-white/10 bg-zinc-950 px-3 py-2.5 text-zinc-100 outline-none ring-violet-500/40 focus:border-violet-500/40 focus:ring-2"
          />
        </label>
        <label className="flex flex-col gap-1.5 text-sm">
          <span className="font-medium text-zinc-400">Пароль</span>
          <input
            type="password"
            autoComplete="current-password"
            required
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            className="rounded-[10px] border border-white/10 bg-zinc-950 px-3 py-2.5 text-zinc-100 outline-none ring-violet-500/40 focus:border-violet-500/40 focus:ring-2"
          />
        </label>

        {error ? (
          <p className="m-0 text-sm font-medium text-red-400" role="alert">
            {error}
          </p>
        ) : null}

        <Button
          type="submit"
          variant="primary"
          className="mt-2 w-full rounded-[10px]"
          disabled={submitting}
        >
          {submitting ? "Входим…" : "Войти"}
        </Button>
      </form>

      <p className="mt-6 text-center text-sm text-zinc-500">
        Нет аккаунта?{" "}
        <Link
          to="/register"
          state={from ? { from } : undefined}
          className="font-semibold text-violet-400 hover:underline"
        >
          Регистрация
        </Link>
      </p>
    </div>
  );
}
