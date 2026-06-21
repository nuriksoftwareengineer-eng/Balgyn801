import { Hero } from "@/widgets/home/Hero";
import { BestSellers } from "@/widgets/home/BestSellers";
import { ValueStrip } from "@/widgets/home/ValueStrip";
import { InstagramSection } from "@/widgets/home/InstagramSection";

export function HomePage() {
  return (
    <>
      <Hero />
      <BestSellers />
      <ValueStrip />
      <InstagramSection />
    </>
  );
}
