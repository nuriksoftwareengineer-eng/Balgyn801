import { createBrowserRouter, RouterProvider } from "react-router-dom";
import { AboutPage } from "@/pages/AboutPage";
import { CartPage } from "@/pages/CartPage";
import { CatalogPage } from "@/pages/CatalogPage";
import { HomePage } from "@/pages/HomePage";
import { MainLayout } from "@/pages/MainLayout";

const router = createBrowserRouter([
  {
    path: "/",
    element: <MainLayout />,
    children: [
      { index: true, element: <HomePage /> },
      { path: "catalog", element: <CatalogPage /> },
      { path: "cart", element: <CartPage /> },
      { path: "about", element: <AboutPage /> },
    ],
  },
]);

export function AppRouter() {
  return <RouterProvider router={router} />;
}
