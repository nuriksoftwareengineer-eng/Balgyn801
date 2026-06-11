import type { HTMLAttributes } from "react";
import { cn } from "@/shared/lib/cn";

// ─── Types ────────────────────────────────────────────────────────────────

type Variant = "default" | "secondary" | "outline";

export interface BadgeProps extends HTMLAttributes<HTMLSpanElement> {
  variant?: Variant;
}

// ─── Component ────────────────────────────────────────────────────────────

export function Badge({
  className,
  variant = "default",
  ...props
}: BadgeProps) {
  return (
    <span
      className={cn(
        "inline-flex items-center px-2.5 py-0.5 text-[0.65rem] font-semibold uppercase tracking-widest",

        variant === "default"   && "bg-black text-white",
        variant === "secondary" && "bg-[--color-surface] text-black",
        variant === "outline"   && "border border-black bg-transparent text-black",

        className,
      )}
      {...props}
    />
  );
}
