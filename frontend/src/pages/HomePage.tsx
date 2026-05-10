import { useState } from "react";
import { Link } from "react-router-dom";
import { Container } from "@/shared/ui/container";
import { SectionHead } from "@/shared/ui/section-head";
import { CategoryChips } from "@/widgets/CategoryChips";
import { CustomDesignCTASection } from "@/widgets/CustomDesignCTASection";
import { HeroSection } from "@/widgets/HeroSection";
import { ProductCatalogGrid } from "@/widgets/ProductCatalogGrid";
import { TrustStrip } from "@/widgets/TrustStrip";

export function HomePage() {
  const [categoryFilter, setCategoryFilter] = useState<string | null>(null);

  return (
    <>
      <HeroSection />
      <TrustStrip />
      <section className="py-8 md:pb-16" id="shop">
        <Container>
          <SectionHead
            title="Подборка категорий"
            action={
              <Link to="/catalog" className="text-violet-400 hover:underline">
                Смотреть всё
              </Link>
            }
          />
          <CategoryChips
            selectedCategory={categoryFilter}
            onSelect={setCategoryFilter}
          />
          <SectionHead
            title="Хиты каталога"
            className="mt-12"
            action={
              <span className="text-sm font-semibold text-violet-400/85">
                Из вашего API
              </span>
            }
          />
          <ProductCatalogGrid categoryFilter={categoryFilter} />
        </Container>
      </section>
      <CustomDesignCTASection />
    </>
  );
}
