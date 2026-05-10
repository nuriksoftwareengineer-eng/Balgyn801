import { createBrowserRouter, RouterProvider } from "react-router-dom";
import { AdminDashboardPage } from "@/admin/AdminDashboardPage";
import { AdminLayout } from "@/admin/AdminLayout";
import { AdminProductsPage } from "@/admin/AdminProductsPage";
import { RequireAdmin } from "@/admin/RequireAdmin";
import { AboutPage } from "@/pages/AboutPage";
import { AuthShellLayout } from "@/pages/AuthShellLayout";
import { CartPage } from "@/pages/CartPage";
import { CatalogPage } from "@/pages/CatalogPage";
import { HomePage } from "@/pages/HomePage";
import { LoginPage } from "@/pages/LoginPage";
import { MainLayout } from "@/pages/MainLayout";
import { ProductPage } from "@/pages/ProductPage";
import { RegisterPage } from "@/pages/RegisterPage";

const router = createBrowserRouter([
  {
    path: "/",
    element: <MainLayout />,
    children: [
      { index: true, element: <HomePage /> },
      { path: "catalog", element: <CatalogPage /> },
      { path: "catalog/:productId", element: <ProductPage /> },
      { path: "cart", element: <CartPage /> },
      { path: "about", element: <AboutPage /> },
    ],
  },
  {
    element: <AuthShellLayout />,
    children: [
      { path: "login", element: <LoginPage /> },
      { path: "register", element: <RegisterPage /> },
    ],
  },
  {
    path: "/admin",
    element: <RequireAdmin />,
    children: [
      {
        element: <AdminLayout />,
        children: [
          { index: true, element: <AdminDashboardPage /> },
          { path: "products", element: <AdminProductsPage /> },
        ],
      },
    ],
  },
]);

export function AppRouter() {
  return <RouterProvider router={router} />;
}
