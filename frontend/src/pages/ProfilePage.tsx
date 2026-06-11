import { Link } from "react-router-dom";
import { useAuth } from "@/app/auth-context";
import { Button } from "@/components/ui/button";

export function ProfilePage() {
  const { user, logout } = useAuth();

  if (!user) return null;

  return (
    <div className="min-h-screen bg-white">
      <div className="mx-auto max-w-[520px] px-5 py-12">
        <h1 className="mb-8 text-2xl font-semibold uppercase tracking-[0.06em] text-black">
          Профиль
        </h1>

        <div className="border border-[--color-border] bg-white px-6 py-6">
          <dl className="flex flex-col gap-4">
            <div>
              <dt className="text-[0.6rem] font-medium uppercase tracking-[0.14em] text-[--color-muted]">
                Email
              </dt>
              <dd className="mt-1 text-sm text-black">{user.email}</dd>
            </div>
            <div>
              <dt className="text-[0.6rem] font-medium uppercase tracking-[0.14em] text-[--color-muted]">
                Роли
              </dt>
              <dd className="mt-1 flex flex-wrap gap-2">
                {user.roles.map((role) => (
                  <span
                    key={role}
                    className="border border-[--color-border] px-2 py-0.5 text-[0.6rem] font-medium uppercase tracking-[0.1em] text-black"
                  >
                    {role}
                  </span>
                ))}
              </dd>
            </div>
          </dl>

          <div className="mt-6 flex flex-col gap-3 border-t border-[--color-border] pt-6">
            <Link
              to="/orders"
              className="text-[0.7rem] font-medium uppercase tracking-[0.14em] text-black underline underline-offset-2 hover:text-[--color-muted]"
            >
              Мои заказы
            </Link>
            <Button
              variant="outline"
              size="sm"
              onClick={logout}
              className="w-fit"
            >
              Выйти из аккаунта
            </Button>
          </div>
        </div>
      </div>
    </div>
  );
}
