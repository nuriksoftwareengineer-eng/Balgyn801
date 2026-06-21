import type { HTMLAttributes } from "react";
import { cn } from "@/shared/lib/cn";

// ─── Types ────────────────────────────────────────────────────────────────

export interface SeparatorProps extends HTMLAttributes<HTMLHRElement> {
  orientation?: "horizontal" | "vertical";
}

// ─── Component ────────────────────────────────────────────────────────────

export function Separator({
  className,
  orientation = "horizontal",
  ...props
}: SeparatorProps) {
  if (orientation === "vertical") {
    return (
      <span
        role="separator"
        aria-orientation="vertical"
        className={cn("inline-block h-full w-px bg-[--color-border]", className)}
        {...(props as HTMLAttributes<HTMLSpanElement>)}
      />
    );
  }

  return (
    <hr
      role="separator"
      aria-orientation="horizontal"
      className={cn("border-0 border-t border-[--color-border]", className)}
      {...props}
    />
  );
}
