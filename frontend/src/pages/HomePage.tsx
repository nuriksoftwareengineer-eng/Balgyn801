import { Hero } from "@/widgets/home/Hero";
import { BestSellers } from "@/widgets/home/BestSellers";
import { NewArrivalsSection } from "@/widgets/home/NewArrivalsSection";
import { PopularSection } from "@/widgets/home/PopularSection";
import { ValueStrip } from "@/widgets/home/ValueStrip";
import { ReviewCarousel } from "@/widgets/home/ReviewCarousel";
import { InstagramSection } from "@/widgets/home/InstagramSection";

export function HomePage() {
  return (
    <>
      <Hero />
      <BestSellers />
      <NewArrivalsSection />
      <PopularSection />
      <ValueStrip />
      <ReviewCarousel />
      <InstagramSection />
    </>
  );
}
