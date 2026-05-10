import { useMemo } from "react";
import { useSearchParams } from "react-router-dom";
import {
  PRODUCT_CATEGORIES,
  STORE_VIEW_ALL_CATEGORY,
} from "@/shared/constants/store-content";
import { Container } from "@/shared/ui/container";
import { SectionHead } from "@/shared/ui/section-head";
import { CategoryChips } from "@/widgets/CategoryChips";
import { ProductCatalogGrid } from "@/widgets/ProductCatalogGrid";

function categoryFromSearchParam(raw: string | null): string | null {
  if (raw == null || raw === "") return null;
  try {
    const decoded = decodeURIComponent(raw);
    if (decoded === STORE_VIEW_ALL_CATEGORY) return null;
    if ((PRODUCT_CATEGORIES as readonly string[]).includes(decoded)) {
      return decoded;
    }
  } catch {
    /* ignore */
  }
  return null;
}

export function CatalogPage() {
  const [searchParams, setSearchParams] = useSearchParams();
  const selectedCategory = useMemo(
    () => categoryFromSearchParam(searchParams.get("category")),
    [searchParams],
  );

  function setCategory(next: string | null) {
    setSearchParams(
      (prev) => {
        const p = new URLSearchParams(prev);
        if (next == null) {
          p.delete("category");
        } else {
          p.set("category", next);
        }
        return p;
      },
      { replace: true },
    );
  }

  return (
    <div className="py-10 md:py-14">
      <Container>
        <SectionHead title="Каталог" />
        <div className="mb-8">
          <CategoryChips
            selectedCategory={selectedCategory}
            onSelect={setCategory}
          />
        </div>
        <ProductCatalogGrid categoryFilter={selectedCategory} />
      </Container>
    </div>
  );
}
