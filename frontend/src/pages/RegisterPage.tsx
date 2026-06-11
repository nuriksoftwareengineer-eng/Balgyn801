import { useEffect, useState } from "react";
import { Link, useLocation, useNavigate } from "react-router-dom";
import { useAuth } from "@/app/auth-context";
import { ApiError } from "@/shared/api/http";
import { Button } from "@/components/ui/button";

const inputClass =
  "rounded-none border border-[--color-border] bg-white px-3 py-2.5 text-sm text-black outline-none transition focus:border-black focus:ring-1 focus:ring-black";

export function RegisterPage() {
  const { register, user, loading } = useAuth();
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
    <div className="w-full max-w-[400px] border border-[--color-border] bg-white p-8">
      <h1 className="mb-1 text-2xl font-semibold uppercase tracking-[0.04em] text-black">
        Регистрация
      </h1>
      <p className="mb-6 text-sm text-[--color-muted]">
        После регистрации вы получите роль USER.
      </p>

      <form onSubmit={handleSubmit} className="flex flex-col gap-4">
        <label className="flex flex-col gap-1.5">
          <span className="text-[0.65rem] font-medium uppercase tracking-[0.1em] text-[--color-muted]">
            Email
          </span>
          <input
            type="email"
            autoComplete="email"
            required
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            className={inputClass}
          />
        </label>
        <label className="flex flex-col gap-1.5">
          <span className="text-[0.65rem] font-medium uppercase tracking-[0.1em] text-[--color-muted]">
            Пароль <span className="normal-case font-normal">(от 8 символов)</span>
          </span>
          <input
            type="password"
            autoComplete="new-password"
            required
            minLength={8}
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            className={inputClass}
          />
        </label>

        {error ? (
          <p className="m-0 text-sm font-medium text-[--color-danger]" role="alert">
            {error}
          </p>
        ) : null}

        <Button
          type="submit"
          size="lg"
          className="mt-2 w-full"
          disabled={submitting}
        >
          {submitting ? "Создаём…" : "Создать аккаунт"}
        </Button>
      </form>

      <p className="mt-6 text-center text-sm text-[--color-muted]">
        Уже есть аккаунт?{" "}
        <Link
          to="/login"
          state={from ? { from } : undefined}
          className="font-semibold text-black underline underline-offset-2 hover:text-[--color-muted]"
        >
          Войти
        </Link>
      </p>
    </div>
  );
}
