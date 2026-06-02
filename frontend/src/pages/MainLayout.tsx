import { Outlet } from "react-router-dom";
import { AnnouncementBar } from "@/widgets/AnnouncementBar";
import { AnimatedBackground } from "@/widgets/AnimatedBackground";
import { SiteFooter } from "@/widgets/SiteFooter";
import { SiteHeader } from "@/widgets/SiteHeader";

export function MainLayout() {
  return (
    <div className="relative min-h-screen bg-zinc-950 text-zinc-100 antialiased">
      <AnimatedBackground />
      <div className="relative z-10">
        <AnnouncementBar />
        <SiteHeader />
        <Outlet />
        <SiteFooter />
      </div>
    </div>
  );
}
