import { Link, Outlet } from "react-router-dom";

export function AuthShellLayout() {
  return (
    <div className="flex min-h-screen flex-col bg-white">
      <header className="border-b border-[--color-border] px-5 py-5">
        <Link
          to="/"
          className="text-base font-semibold uppercase tracking-[0.18em] text-black transition hover:text-[--color-muted]"
        >
          BALGYN
        </Link>
      </header>
      <main className="flex flex-1 items-center justify-center px-4 py-12">
        <Outlet />
      </main>
    </div>
  );
}
