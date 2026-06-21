import type { ButtonHTMLAttributes } from "react";
import { cn } from "@/shared/lib/cn";

type IconButtonProps = ButtonHTMLAttributes<HTMLButtonElement>;

export function IconButton({ className, ...props }: IconButtonProps) {
  return (
    <button
      type="button"
      className={cn(
        "inline-flex h-[42px] w-[42px] items-center justify-center rounded-[10px] border border-white/10 bg-zinc-900 text-zinc-100 transition hover:border-white/40 hover:bg-white/10",
        className,
      )}
      {...props}
    />
  );
}
