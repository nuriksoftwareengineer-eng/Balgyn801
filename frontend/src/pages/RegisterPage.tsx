import { useEffect, useState } from "react";
import { Link, useLocation, useNavigate } from "react-router-dom";
import { useAuth } from "@/app/auth-context";
import { ApiError } from "@/shared/api/http";

const inputClass =
  "w-full border-b border-[--color-border] bg-transparent py-3 text-[15px] text-black outline-none transition placeholder:text-[--color-muted] focus:border-black";

export function RegisterPage() {
  const { register, user, loading } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const from = (location.state as { from?: string } | null)?.from ?? undefined;

  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [afterAuth, setAfterAuth] = useState(false);

  useEffect(() => {
    if (!afterAuth || loading) return;
    if (!user) return;
    let dest = from ?? "/";
    if (dest.startsWith("/admin") && !user.roles.includes("ADMIN")) dest = "/";
    navigate(dest, { replace: true });
    queueMicrotask(() => setAfterAuth(false));
  }, [afterAuth, loading, user, from, navigate]);

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setError(null);
    setSubmitting(true);
    try {
      await register(email.trim(), password);
      setAfterAuth(true);
    } catch (err) {
      setError(
        err instanceof ApiError ? err.message : "Не удалось зарегистрироваться",
      );
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <>
      <h1 className="mb-2 text-[32px] font-extrabold uppercase tracking-[-0.02em] text-black">
        Создать аккаунт
      </h1>
      <p className="mb-8 text-[13px] text-[--color-muted]">
        Уже есть аккаунт?{" "}
        <Link
          to="/login"
          state={from ? { from } : undefined}
          className="font-semibold text-black underline underline-offset-2 hover:opacity-70"
        >
          Войти
        </Link>
      </p>

      <form onSubmit={handleSubmit} className="flex flex-col gap-6">
        <label className="flex flex-col gap-1">
          <span className="text-[11px] font-semibold uppercase tracking-[0.14em] text-[--color-muted]">
            Email
          </span>
          <input
            type="email"
            autoComplete="email"
            required
            placeholder="you@example.com"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            className={inputClass}
          />
        </label>

        <label className="flex flex-col gap-1">
          <span className="text-[11px] font-semibold uppercase tracking-[0.14em] text-[--color-muted]">
            Пароль{" "}
            <span className="normal-case font-normal tracking-normal">
              (от 8 символов)
            </span>
          </span>
          <input
            type="password"
            autoComplete="new-password"
            required
            minLength={8}
            placeholder="••••••••"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            className={inputClass}
          />
        </label>

        {error ? (
          <p className="text-[13px] font-medium text-[--color-danger]" role="alert">
            {error}
          </p>
        ) : null}

        <button
          type="submit"
          disabled={submitting}
          className="mt-2 w-full bg-black py-4 text-[13px] font-bold uppercase tracking-[0.14em] text-white transition hover:bg-zinc-800 disabled:opacity-50"
        >
          {submitting ? "Создаём…" : "Создать аккаунт"}
        </button>
      </form>
    </>
  );
}
