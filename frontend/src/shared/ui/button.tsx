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
        "inline-flex items-center justify-center rounded-xl font-semibold transition focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-white disabled:pointer-events-none disabled:opacity-45",
        variant === "primary" &&
          "bg-white px-7 py-3.5 text-black hover:-translate-y-0.5 hover:bg-zinc-200",
        variant === "outline" &&
          "border border-white/10 bg-transparent px-3 py-3 text-sm text-zinc-100 hover:border-white/40 hover:bg-white/10",
        variant === "ghost" && "px-3 py-2 text-zinc-400 hover:text-zinc-100",
        className,
      )}
      {...props}
    >
      {children}
    </button>
  );
}
