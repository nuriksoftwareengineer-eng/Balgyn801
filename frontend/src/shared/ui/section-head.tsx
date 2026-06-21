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
      <h2 className="text-3xl font-semibold uppercase tracking-[0.05em] text-black md:text-4xl">
        {title}
      </h2>
      {action ?? null}
    </div>
  );
}
