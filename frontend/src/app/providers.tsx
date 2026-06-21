import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { useState, type ReactNode } from "react";
import { AuthProvider } from "@/app/auth-context";
import { CartDrawerProvider } from "@/app/cart-drawer-context";
import { CartProvider } from "@/app/cart-provider";
import { CurrencyProvider } from "@/app/currency-context";

export function Providers({ children }: { children: ReactNode }) {
  const [queryClient] = useState(
    () =>
      new QueryClient({
        defaultOptions: {
          queries: {
            staleTime: 60 * 1000,
            retry: 1,
          },
        },
      }),
  );

  return (
    <QueryClientProvider client={queryClient}>
      <AuthProvider>
        <CurrencyProvider>
          <CartDrawerProvider>
            <CartProvider>{children}</CartProvider>
          </CartDrawerProvider>
        </CurrencyProvider>
      </AuthProvider>
    </QueryClientProvider>
  );
}
