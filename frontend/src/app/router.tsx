import { lazy, Suspense } from "react";
import { createBrowserRouter, RouterProvider } from "react-router-dom";
import { AdminLayout } from "@/admin/AdminLayout";
import { RequireAdmin } from "@/admin/RequireAdmin";
import { AuthShellLayout } from "@/pages/AuthShellLayout";
import { MainLayout } from "@/pages/MainLayout";
import { PageLoadFallback } from "@/shared/ui/page-load-fallback";

const HomePage = lazy(() =>
  import("@/pages/HomePage").then((m) => ({ default: m.HomePage })),
);
const CatalogPage = lazy(() =>
  import("@/pages/CatalogPage").then((m) => ({ default: m.CatalogPage })),
);
const ProductPage = lazy(() =>
  import("@/pages/ProductPage").then((m) => ({ default: m.ProductPage })),
);
const CartPage = lazy(() =>
  import("@/pages/CartPage").then((m) => ({ default: m.CartPage })),
);
const CustomDesignPage = lazy(() =>
  import("@/pages/CustomDesignPage").then((m) => ({
    default: m.CustomDesignPage,
  })),
);
const AboutPage = lazy(() =>
  import("@/pages/AboutPage").then((m) => ({ default: m.AboutPage })),
);
const LoginPage = lazy(() =>
  import("@/pages/LoginPage").then((m) => ({ default: m.LoginPage })),
);
const RegisterPage = lazy(() =>
  import("@/pages/RegisterPage").then((m) => ({ default: m.RegisterPage })),
);
const AdminDashboardPage = lazy(() =>
  import("@/admin/AdminDashboardPage").then((m) => ({
    default: m.AdminDashboardPage,
  })),
);
const AdminOrdersPage = lazy(() =>
  import("@/admin/AdminOrdersPage").then((m) => ({
    default: m.AdminOrdersPage,
  })),
);
const AdminOrderDetailPage = lazy(() =>
  import("@/admin/AdminOrderDetailPage").then((m) => ({
    default: m.AdminOrderDetailPage,
  })),
);
const AdminProductsPage = lazy(() =>
  import("@/admin/AdminProductsPage").then((m) => ({
    default: m.AdminProductsPage,
  })),
);

const router = createBrowserRouter([
  {
    path: "/",
    element: <MainLayout />,
    children: [
      {
        index: true,
        element: (
          <Suspense fallback={<PageLoadFallback />}>
            <HomePage />
          </Suspense>
        ),
      },
      {
        path: "catalog",
        element: (
          <Suspense fallback={<PageLoadFallback />}>
            <CatalogPage />
          </Suspense>
        ),
      },
      {
        path: "catalog/:productId",
        element: (
          <Suspense fallback={<PageLoadFallback />}>
            <ProductPage />
          </Suspense>
        ),
      },
      {
        path: "cart",
        element: (
          <Suspense fallback={<PageLoadFallback />}>
            <CartPage />
          </Suspense>
        ),
      },
      {
        path: "custom-design",
        element: (
          <Suspense fallback={<PageLoadFallback />}>
            <CustomDesignPage />
          </Suspense>
        ),
      },
      {
        path: "about",
        element: (
          <Suspense fallback={<PageLoadFallback />}>
            <AboutPage />
          </Suspense>
        ),
      },
    ],
  },
  {
    element: <AuthShellLayout />,
    children: [
      {
        path: "login",
        element: (
          <Suspense fallback={<PageLoadFallback />}>
            <LoginPage />
          </Suspense>
        ),
      },
      {
        path: "register",
        element: (
          <Suspense fallback={<PageLoadFallback />}>
            <RegisterPage />
          </Suspense>
        ),
      },
    ],
  },
  {
    path: "/admin",
    element: <RequireAdmin />,
    children: [
      {
        element: <AdminLayout />,
        children: [
          {
            index: true,
            element: (
              <Suspense fallback={<PageLoadFallback />}>
                <AdminDashboardPage />
              </Suspense>
            ),
          },
          {
            path: "orders",
            element: (
              <Suspense fallback={<PageLoadFallback />}>
                <AdminOrdersPage />
              </Suspense>
            ),
          },
          {
            path: "orders/:orderId",
            element: (
              <Suspense fallback={<PageLoadFallback />}>
                <AdminOrderDetailPage />
              </Suspense>
            ),
          },
          {
            path: "products",
            element: (
              <Suspense fallback={<PageLoadFallback />}>
                <AdminProductsPage />
              </Suspense>
            ),
          },
        ],
      },
    ],
  },
]);

export function AppRouter() {
  return <RouterProvider router={router} />;
}
