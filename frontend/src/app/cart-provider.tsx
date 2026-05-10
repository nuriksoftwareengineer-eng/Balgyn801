import {
  useCallback,
  useMemo,
  useState,
  type ReactNode,
} from "react";
import { CartContext } from "@/app/cart-context";

export function CartProvider({ children }: { children: ReactNode }) {
  const [count, setCount] = useState(0);
  const addOne = useCallback(() => {
    setCount((c) => c + 1);
  }, []);

  const value = useMemo(() => ({ count, addOne }), [count, addOne]);

  return <CartContext.Provider value={value}>{children}</CartContext.Provider>;
}
