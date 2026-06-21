import { Navigate, Outlet, useLocation } from "react-router-dom";
import { useAuth } from "@/app/auth-context";

export function RequireAdmin() {
  const { loading, user } = useAuth();
  const location = useLocation();

  if (loading) {
    return (
      <div className="flex min-h-screen items-center justify-center bg-zinc-950 text-zinc-400">
        <div
          className="h-10 w-10 animate-spin rounded-full border-[3px] border-white/10 border-t-white"
          aria-hidden
        />
      </div>
    );
  }

  if (!user?.roles.includes("ADMIN")) {
    return (
      <Navigate to="/login" replace state={{ from: location.pathname }} />
    );
  }

  return <Outlet />;
}
