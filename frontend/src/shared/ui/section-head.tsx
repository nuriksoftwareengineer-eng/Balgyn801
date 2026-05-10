import type { ReactNode } from "react";
import { cn } from "@/shared/lib/cn";

type SectionHeadProps = {
  title: string;
  action?: ReactNode;
  className?: string;
};

export function SectionHead({ title, action, className }: SectionHeadProps) {
  return (
    <div
      className={cn(
        "mb-7 flex flex-wrap items-baseline justify-between gap-3",
        className,
      )}
    >
      <h2 className="font-display text-4xl tracking-[0.04em] text-zinc-100 md:text-[2rem]">
        {title}
      </h2>
      {action ? <div className="text-sm font-semibold">{action}</div> : null}
    </div>
  );
}
