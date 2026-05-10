import type { ButtonHTMLAttributes, ReactNode } from "react";
import { cn } from "@/shared/lib/cn";

type ButtonProps = ButtonHTMLAttributes<HTMLButtonElement> & {
  children: ReactNode;
  variant?: "primary" | "ghost" | "outline";
};

export function Button({
  className,
  variant = "primary",
  children,
  ...props
}: ButtonProps) {
  return (
    <button
      type="button"
      className={cn(
        "inline-flex items-center justify-center rounded-xl font-semibold transition focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-violet-500 disabled:pointer-events-none disabled:opacity-45",
        variant === "primary" &&
          "bg-gradient-to-br from-violet-500 to-purple-600 px-7 py-3.5 text-white shadow-[0_0_40px_rgba(168,85,247,0.35)] hover:-translate-y-0.5 hover:shadow-[0_8px_48px_rgba(168,85,247,0.35)]",
        variant === "outline" &&
          "border border-white/10 bg-transparent px-3 py-3 text-sm text-zinc-100 hover:border-violet-500 hover:bg-violet-500/15",
        variant === "ghost" && "px-3 py-2 text-zinc-400 hover:text-zinc-100",
        className,
      )}
      {...props}
    >
      {children}
    </button>
  );
}
