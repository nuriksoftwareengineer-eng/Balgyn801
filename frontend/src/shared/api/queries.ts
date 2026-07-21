import { useQuery } from "@tanstack/react-query";
import { getProduct, getProducts } from "@/shared/api/backend-api";
import { apiFetch } from "@/shared/api/http";

/** Public, unauthenticated store-wide settings (CEO photo, product-page info-block
 *  copy, etc.) — see SiteSettingController.getPublicSettings on the backend. */
export function useSiteSettings() {
  return useQuery({
    queryKey: ["site-settings"],
    queryFn: () => apiFetch<Record<string, string>>("/site-settings"),
    staleTime: 5 * 60 * 1000,
  });
}

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
