import { Link, Outlet } from "react-router-dom";

export function AuthShellLayout() {
  return (
    <div className="flex min-h-screen flex-col bg-zinc-950 bg-[radial-gradient(ellipse_80%_60%_at_50%_-10%,rgba(139,92,246,0.2),transparent)] text-zinc-100">
      <header className="border-b border-white/10 px-5 py-5">
        <Link
          to="/"
          className="font-display text-xl tracking-[0.08em] text-zinc-100 hover:text-violet-300"
        >
          BALG<span className="text-violet-400">YN</span>
        </Link>
      </header>
      <main className="flex flex-1 items-center justify-center px-4 py-12">
        <Outlet />
      </main>
    </div>
  );
}
