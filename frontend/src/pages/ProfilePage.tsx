import { Link } from "react-router-dom";
import { useTranslation } from "react-i18next";
import { useAuth } from "@/app/auth-context";
import { Container } from "@/shared/ui/container";

export function ProfilePage() {
  const { t } = useTranslation();
  const { user, logout } = useAuth();

  if (!user) return null;

  const isAdmin = user.roles?.includes("ADMIN");

  return (
    <>
      {/* ── Hero ────────────────────────────────────────────── */}
      <div className="border-b border-[--color-border] bg-black">
        <Container className="py-12 md:py-16">
          <h1 className="text-4xl font-extrabold uppercase tracking-[-0.01em] text-white md:text-5xl">
            {t("profile.title")}
          </h1>
        </Container>
      </div>

      {/* ── Content ─────────────────────────────────────────── */}
      <Container className="py-10 md:py-14">
        <div className="max-w-[480px]">
          {/* User info */}
          <dl className="flex flex-col gap-6 border-b border-[--color-border] pb-8">
            <div>
              <dt className="mb-1 text-[11px] font-semibold uppercase tracking-[0.14em] text-[--color-muted]">
                Email
              </dt>
              <dd className="text-[15px] text-black">{user.email}</dd>
            </div>
          </dl>

          {/* Actions */}
          <div className="mt-8 flex flex-col gap-4">
            <Link
              to="/orders"
              className="text-[13px] font-semibold uppercase tracking-[0.1em] text-black underline underline-offset-4 hover:opacity-60 transition-opacity"
            >
              {t("profile.myOrders")}
            </Link>
            {isAdmin && (
              <Link
                to="/admin"
                className="text-[13px] font-semibold uppercase tracking-[0.1em] text-black underline underline-offset-4 hover:opacity-60 transition-opacity"
              >
                {t("profile.adminPanel")}
              </Link>
            )}
            <button
              type="button"
              onClick={logout}
              className="w-fit text-[13px] font-semibold uppercase tracking-[0.1em] text-[--color-muted] underline underline-offset-4 hover:text-black transition-colors"
            >
              {t("profile.logout")}
            </button>
          </div>
        </div>
      </Container>
    </>
  );
}
