import { Navigate, Outlet, useLocation } from "react-router-dom";
import { useAuth } from "@/app/auth-context";

export function RequireAuth() {
  const { loading, user } = useAuth();
  const location = useLocation();

  if (loading) {
    return (
      <div className="flex min-h-[60vh] items-center justify-center">
        <div className="h-8 w-8 animate-spin rounded-full border-[3px] border-[--color-border] border-t-black" aria-hidden />
      </div>
    );
  }

  if (!user) {
    return <Navigate to="/login" replace state={{ from: location.pathname }} />;
  }

  return <Outlet />;
}
