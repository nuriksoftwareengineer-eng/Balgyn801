import { lazy, Suspense } from "react";
import { createBrowserRouter, RouterProvider } from "react-router-dom";
import { AdminLayout } from "@/admin/AdminLayout";
import { RequireAdmin } from "@/admin/RequireAdmin";
import { RequireAuth } from "@/app/RequireAuth";
import { AuthShellLayout } from "@/pages/AuthShellLayout";
import { MainLayout } from "@/pages/MainLayout";
import { PageLoadFallback } from "@/shared/ui/page-load-fallback";

const HomePage = lazy(() =>
  import("@/pages/HomePage").then((m) => ({ default: m.HomePage })),
);
const CatalogIndexPage = lazy(() =>
  import("@/pages/CatalogIndexPage").then((m) => ({ default: m.CatalogIndexPage })),
);
const CatalogParamPage = lazy(() =>
  import("@/pages/CatalogParamPage").then((m) => ({ default: m.CatalogParamPage })),
);
const CollectionPage = lazy(() =>
  import("@/pages/CollectionPage").then((m) => ({ default: m.CollectionPage })),
);
const DesignPage = lazy(() =>
  import("@/pages/DesignPage").then((m) => ({ default: m.DesignPage })),
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
const PaymentReturnPage = lazy(() =>
  import("@/pages/PaymentReturnPage").then((m) => ({
    default: m.PaymentReturnPage,
  })),
);
const OrderHistoryPage = lazy(() =>
  import("@/pages/OrderHistoryPage").then((m) => ({
    default: m.OrderHistoryPage,
  })),
);
const ProfilePage = lazy(() =>
  import("@/pages/ProfilePage").then((m) => ({ default: m.ProfilePage })),
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
const AdminCustomersPage = lazy(() =>
  import("@/admin/AdminCustomersPage").then((m) => ({
    default: m.AdminCustomersPage,
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
            <CatalogIndexPage />
          </Suspense>
        ),
      },
      {
        path: "catalog/:param",
        element: (
          <Suspense fallback={<PageLoadFallback />}>
            <CatalogParamPage />
          </Suspense>
        ),
      },
      {
        path: "catalog/:groupSlug/:collectionSlug",
        element: (
          <Suspense fallback={<PageLoadFallback />}>
            <CollectionPage />
          </Suspense>
        ),
      },
      {
        path: "catalog/:groupSlug/:collectionSlug/:designSlug",
        element: (
          <Suspense fallback={<PageLoadFallback />}>
            <DesignPage />
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
      {
        path: "payment-return",
        element: (
          <Suspense fallback={<PageLoadFallback />}>
            <PaymentReturnPage />
          </Suspense>
        ),
      },
      // Auth-protected routes (inside MainLayout for header/footer)
      {
        element: <RequireAuth />,
        children: [
          {
            path: "orders",
            element: (
              <Suspense fallback={<PageLoadFallback />}>
                <OrderHistoryPage />
              </Suspense>
            ),
          },
          {
            path: "profile",
            element: (
              <Suspense fallback={<PageLoadFallback />}>
                <ProfilePage />
              </Suspense>
            ),
          },
        ],
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
          {
            path: "customers",
            element: (
              <Suspense fallback={<PageLoadFallback />}>
                <AdminCustomersPage />
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
