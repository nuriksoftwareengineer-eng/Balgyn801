import { useQuery } from "@tanstack/react-query";
import {
  getCatalogGroups,
  getCatalogGroup,
  getCatalogCollection,
  getCatalogDesigns,
  getCatalogDesign,
} from "@/shared/api/backend-api";

export function useCatalogGroups() {
  return useQuery({
    queryKey: ["catalog", "groups"],
    queryFn: getCatalogGroups,
    staleTime: 5 * 60 * 1000,
  });
}

export function useCatalogGroup(slug: string | undefined) {
  return useQuery({
    queryKey: ["catalog", "group", slug],
    queryFn: () => getCatalogGroup(slug!),
    enabled: !!slug,
    staleTime: 5 * 60 * 1000,
  });
}

export function useCatalogCollection(slug: string | undefined) {
  return useQuery({
    queryKey: ["catalog", "collection", slug],
    queryFn: () => getCatalogCollection(slug!),
    enabled: !!slug,
    staleTime: 5 * 60 * 1000,
  });
}

export function useCatalogDesigns(collectionId?: number | null) {
  return useQuery({
    queryKey: ["catalog", "designs", collectionId ?? "all"],
    queryFn: () => getCatalogDesigns(collectionId),
    staleTime: 5 * 60 * 1000,
  });
}

export function useCatalogDesign(slug: string | undefined) {
  return useQuery({
    queryKey: ["catalog", "design", slug],
    queryFn: () => getCatalogDesign(slug!),
    enabled: !!slug,
    staleTime: 5 * 60 * 1000,
  });
}
