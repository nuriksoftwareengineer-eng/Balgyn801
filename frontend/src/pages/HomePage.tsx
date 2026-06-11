import { Link } from "react-router-dom";
import { Container } from "@/shared/ui/container";
import { SectionHead } from "@/shared/ui/section-head";
import { CustomDesignCTASection } from "@/widgets/CustomDesignCTASection";
import { HeroSection } from "@/widgets/HeroSection";
import { ProductCatalogGrid } from "@/widgets/ProductCatalogGrid";

export function HomePage() {
  return (
    <>
      <HeroSection />

      <section className="py-16 md:py-24" id="shop">
        <Container>
          <SectionHead
            title="Хиты продаж"
            action={
              <Link
                to="/catalog"
                className="text-[0.65rem] uppercase tracking-[0.14em] text-[--color-muted] transition-colors hover:text-black"
              >
                Смотреть всё
              </Link>
            }
          />
          <ProductCatalogGrid categoryFilter={null} />
        </Container>
      </section>

      <CustomDesignCTASection />
    </>
  );
}
