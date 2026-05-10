import { useQuery } from "@tanstack/react-query";
import { getProducts } from "@/shared/api/client";

export function useProducts() {
  return useQuery({
    queryKey: ["products"],
    queryFn: getProducts,
  });
}
