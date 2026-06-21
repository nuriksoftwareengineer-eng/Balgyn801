import type { HTMLAttributes } from "react";
import { cn } from "@/shared/lib/cn";

// ─── Types ────────────────────────────────────────────────────────────────

export interface SkeletonProps extends HTMLAttributes<HTMLDivElement> {
  /** Shorthand for fixed width + height when you just want a rectangle. */
  width?: string | number;
  height?: string | number;
}

// ─── Component ────────────────────────────────────────────────────────────

export function Skeleton({ className, width, height, style, ...props }: SkeletonProps) {
  return (
    <div
      className={cn("skeleton-shimmer rounded-sm", className)}
      style={{
        width:  width  !== undefined ? (typeof width  === "number" ? `${width}px`  : width)  : undefined,
        height: height !== undefined ? (typeof height === "number" ? `${height}px` : height) : undefined,
        ...style,
      }}
      aria-hidden="true"
      {...props}
    />
  );
}
