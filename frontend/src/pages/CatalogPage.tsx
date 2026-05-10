import { Container } from "@/shared/ui/container";
import { SectionHead } from "@/shared/ui/section-head";
import { ProductCatalogGrid } from "@/widgets/ProductCatalogGrid";

export function CatalogPage() {
  return (
    <div className="py-10 md:py-14">
      <Container>
        <SectionHead title="Каталог" />
        <ProductCatalogGrid />
      </Container>
    </div>
  );
}
