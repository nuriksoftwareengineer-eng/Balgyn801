import { useState } from "react";
import { Link, useLocation, useNavigate } from "react-router-dom";
import { useTranslation } from "react-i18next";
import { useAuth } from "@/app/auth-context";
import { ApiError } from "@/shared/api/http";

const inputClass =
  "w-full border-b border-[--color-border] bg-transparent py-3 text-[15px] text-black outline-none transition placeholder:text-[--color-muted] focus:border-black";

export function RegisterPage() {
  const { t } = useTranslation();
  const { register } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const from = (location.state as { from?: string } | null)?.from ?? undefined;

  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setError(null);
    setSubmitting(true);
    try {
      await register(email.trim(), password);
      navigate("/login", { replace: true, state: { from, registered: true } });
    } catch (err) {
      setError(
        err instanceof ApiError ? err.message : t("auth.registerError"),
      );
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <>
      <h1 className="mb-2 text-[32px] font-extrabold uppercase tracking-[-0.02em] text-black">
        {t("auth.registerHeading")}
      </h1>
      <p className="mb-8 text-[13px] text-[--color-muted]">
        {t("auth.haveAccount")}{" "}
        <Link
          to="/login"
          state={from ? { from } : undefined}
          className="font-semibold text-black underline underline-offset-2 hover:opacity-70"
        >
          {t("auth.loginLinkText")}
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
            {t("auth.password")}{" "}
            <span className="normal-case font-normal tracking-normal">
              {t("auth.passwordHint")}
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
          {submitting ? t("auth.creatingAccount") : t("auth.createAccount")}
        </button>
      </form>
    </>
  );
}
