import { useQuery } from "@tanstack/react-query";
import { getProduct, getProducts } from "@/shared/api/backend-api";

export function useProducts(category?: string | null) {
  return useQuery({
    queryKey: ["products", category ?? "all"],
    queryFn: () => getProducts(category),
  });
}

export function useProduct(id: number | undefined) {
  return useQuery({
    queryKey: ["product", id],
    queryFn: () => getProduct(id!),
    enabled: id != null && Number.isFinite(id) && id > 0,
  });
}
