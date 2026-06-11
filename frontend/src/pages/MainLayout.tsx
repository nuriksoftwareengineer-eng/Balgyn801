import { Outlet } from "react-router-dom";
import { AnnouncementBar } from "@/widgets/AnnouncementBar";
import { SiteFooter } from "@/widgets/SiteFooter";
import { SiteHeader } from "@/widgets/SiteHeader";

export function MainLayout() {
  return (
    <div className="flex min-h-screen flex-col bg-white text-black antialiased">
      <AnnouncementBar />
      <SiteHeader />
      <main className="flex-1">
        <Outlet />
      </main>
      <SiteFooter />
    </div>
  );
}
