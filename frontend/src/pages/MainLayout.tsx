import { Outlet } from "react-router-dom";
import { AnnouncementBar } from "@/widgets/AnnouncementBar";
import { SiteFooter } from "@/widgets/SiteFooter";
import { SiteHeader } from "@/widgets/SiteHeader";

export function MainLayout() {
  return (
    <div className="layout-glow min-h-screen bg-zinc-950 bg-[radial-gradient(ellipse_120%_80%_at_50%_-20%,rgba(168,85,247,0.14),transparent_55%)] text-zinc-100 antialiased">
      <AnnouncementBar />
      <SiteHeader />
      <Outlet />
      <SiteFooter />
    </div>
  );
}
