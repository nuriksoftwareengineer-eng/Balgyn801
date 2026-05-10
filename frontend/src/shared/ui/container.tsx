import type { ReactNode } from "react";
import { cn } from "@/shared/lib/cn";

type ContainerProps = {
  children: ReactNode;
  className?: string;
};

export function Container({ children, className }: ContainerProps) {
  return (
    <div className={cn("mx-auto w-full max-w-[1200px] px-5", className)}>
      {children}
    </div>
  );
}
