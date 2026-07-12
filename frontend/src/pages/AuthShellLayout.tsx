import { Link, Outlet } from "react-router-dom";

export function AuthShellLayout() {
  return (
    <div className="flex min-h-screen">
      {/* ── Left brand panel (desktop only) ─────────────────── */}
      <div className="hidden flex-col bg-black text-white md:flex md:w-[420px] lg:w-[500px]">
        <Link
          to="/"
          className="p-10 text-[22px] font-bold uppercase tracking-[0.02em] transition-opacity hover:opacity-70"
        >
          BALGYN
        </Link>
        <div className="flex flex-1 flex-col justify-end p-10">
          <p className="text-[40px] font-bold uppercase leading-[0.92] tracking-[-0.03em] text-white lg:text-[48px]">
            ВЫШИВКА<br />И УЛИЧНАЯ<br />КУЛЬТУРА
          </p>
          <p className="mt-6 text-[13px] text-white/40 uppercase tracking-[0.14em]">
            Алматы, Казахстан
          </p>
        </div>
      </div>

      {/* ── Right form panel ────────────────────────────────── */}
      <div className="flex flex-1 flex-col bg-white">
        {/* Mobile-only logo */}
        <div className="border-b border-[--color-border] px-5 py-5 md:hidden">
          <Link
            to="/"
            className="text-[20px] font-bold uppercase tracking-[0.02em] text-black"
          >
            BALGYN
          </Link>
        </div>

        <div className="flex flex-1 items-center justify-center px-5 py-14">
          <div className="w-full max-w-[380px]">
            <Outlet />
          </div>
        </div>
      </div>
    </div>
  );
}
