import type { ReactNode } from "react";
import { Link } from "react-router-dom";

export interface Crumb {
  label: string;
  to?: string;
}

/** Общая обёртка каталожных страниц в стиле дизайна BALGYN: крошки + крупный uppercase-заголовок. */
export function CatalogShell({
  crumbs,
  title,
  subtitle,
  children,
}: {
  crumbs: Crumb[];
  title: string;
  subtitle?: string;
  children: ReactNode;
}) {
  return (
    <div className="py-14 md:py-20">
      <div className="container mx-auto px-4 md:px-8">
        <nav className="mb-6 flex flex-wrap items-center gap-2 text-[10px] uppercase tracking-[0.2em] text-[#7A7A7A]">
          {crumbs.map((c, i) => (
            <span key={i} className="flex items-center gap-2">
              {c.to ? (
                <Link to={c.to} className="transition-colors hover:text-black">
                  {c.label}
                </Link>
              ) : (
                <span className="text-black">{c.label}</span>
              )}
              {i < crumbs.length - 1 ? <span aria-hidden>/</span> : null}
            </span>
          ))}
        </nav>

        <h1 className="text-[40px] font-extrabold uppercase leading-[1.05] tracking-[-0.04em] sm:text-[56px] md:text-[88px]">
          {title}
        </h1>
        {subtitle ? (
          <p className="mt-4 max-w-[560px] text-[15px] leading-relaxed text-[#7A7A7A]">
            {subtitle}
          </p>
        ) : null}

        <div className="mt-10 md:mt-14">{children}</div>
      </div>
    </div>
  );
}
