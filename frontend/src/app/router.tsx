import { lazy, memo, Suspense } from "react";
import { createBrowserRouter, RouterProvider } from "react-router-dom";
import { AdminLayout } from "@/admin/AdminLayout";
import { RequireAdmin } from "@/admin/RequireAdmin";
import { RequireAuth } from "@/app/RequireAuth";
import { AuthShellLayout } from "@/pages/AuthShellLayout";
import { MainLayout } from "@/pages/MainLayout";
import { PageLoadFallback } from "@/shared/ui/page-load-fallback";
import { ErrorBoundary } from "@/shared/ui/error-boundary";
// Statically imported (not lazy): these are the most common landing/browsing routes.
// Code-splitting them forces every first visit through a cold Suspense-boundary reveal
// (fallback → fetch chunk → render) on top of that page's own data fetch. Bundling them
// (all small chunks — a few KB gzipped) keeps first paint of real content to a single
// data round trip instead of two. Large, rarely-visited sections (Admin) stay lazy.
import { HomePage } from "@/pages/HomePage";
import { CatalogIndexPage } from "@/pages/CatalogIndexPage";
import { CollectionPage } from "@/pages/CollectionPage";

const CatalogParamPage = lazy(() =>
  import("@/pages/CatalogParamPage").then((m) => ({ default: m.CatalogParamPage })),
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
const TermsPage = lazy(() =>
  import("@/pages/TermsPage").then((m) => ({ default: m.TermsPage })),
);
const PrivacyPage = lazy(() =>
  import("@/pages/PrivacyPage").then((m) => ({ default: m.PrivacyPage })),
);
const ReturnsPage = lazy(() =>
  import("@/pages/ReturnsPage").then((m) => ({ default: m.ReturnsPage })),
);
const DeliveryInfoPage = lazy(() =>
  import("@/pages/DeliveryInfoPage").then((m) => ({
    default: m.DeliveryInfoPage,
  })),
);
const ContactsPage = lazy(() =>
  import("@/pages/ContactsPage").then((m) => ({ default: m.ContactsPage })),
);
const TrackOrderPage = lazy(() =>
  import("@/pages/TrackOrderPage").then((m) => ({
    default: m.TrackOrderPage,
  })),
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
const PaymentSuccessPage = lazy(() =>
  import("@/pages/PaymentSuccessPage").then((m) => ({
    default: m.PaymentSuccessPage,
  })),
);
const PaymentFailedPage = lazy(() =>
  import("@/pages/PaymentFailedPage").then((m) => ({
    default: m.PaymentFailedPage,
  })),
);
const PaymentCancelledPage = lazy(() =>
  import("@/pages/PaymentCancelledPage").then((m) => ({
    default: m.PaymentCancelledPage,
  })),
);
const NotFoundPage = lazy(() =>
  import("@/pages/NotFoundPage").then((m) => ({
    default: m.NotFoundPage,
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
const AdminCategoriesPage = lazy(() =>
  import("@/admin/AdminCategoriesPage").then((m) => ({
    default: m.AdminCategoriesPage,
  })),
);
const AdminCollectionsPage = lazy(() =>
  import("@/admin/AdminCollectionsPage").then((m) => ({
    default: m.AdminCollectionsPage,
  })),
);
const AdminDesignsPage = lazy(() =>
  import("@/admin/AdminDesignsPage").then((m) => ({
    default: m.AdminDesignsPage,
  })),
);
const AdminDesignVariantsPage = lazy(() =>
  import("@/admin/AdminDesignVariantsPage").then((m) => ({
    default: m.AdminDesignVariantsPage,
  })),
);
const AdminCustomersPage = lazy(() =>
  import("@/admin/AdminCustomersPage").then((m) => ({
    default: m.AdminCustomersPage,
  })),
);
const AdminSizeChartsPage = lazy(() =>
  import("@/admin/AdminSizeChartsPage").then((m) => ({
    default: m.AdminSizeChartsPage,
  })),
);
const AdminPaymentsPage = lazy(() =>
  import("@/admin/AdminPaymentsPage").then((m) => ({
    default: m.AdminPaymentsPage,
  })),
);
const AdminExchangeRatePage = lazy(() =>
  import("@/admin/AdminExchangeRatePage").then((m) => ({
    default: m.AdminExchangeRatePage,
  })),
);
const AdminUsersPage = lazy(() =>
  import("@/admin/AdminUsersPage").then((m) => ({
    default: m.AdminUsersPage,
  })),
);
const AdminSiteSettingsPage = lazy(() =>
  import("@/admin/AdminSiteSettingsPage").then((m) => ({
    default: m.AdminSiteSettingsPage,
  })),
);
const ReviewsPage = lazy(() =>
  import("@/pages/ReviewsPage").then((m) => ({ default: m.ReviewsPage })),
);
const FAQPage = lazy(() =>
  import("@/pages/FAQPage").then((m) => ({ default: m.FAQPage })),
);
const AdminReviewsPage = lazy(() =>
  import("@/admin/AdminReviewsPage").then((m) => ({ default: m.AdminReviewsPage })),
);
const WishlistPage = lazy(() =>
  import("@/pages/WishlistPage").then((m) => ({ default: m.WishlistPage })),
);
const AdminCouponsPage = lazy(() =>
  import("@/admin/AdminCouponsPage").then((m) => ({
    default: m.AdminCouponsPage,
  })),
);
const AdminGarmentProfilesPage = lazy(() =>
  import("@/admin/AdminGarmentProfilesPage").then((m) => ({
    default: m.AdminGarmentProfilesPage,
  })),
);

const router = createBrowserRouter([
  {
    path: "/",
    element: <ErrorBoundary><MainLayout /></ErrorBoundary>,
    children: [
      {
        index: true,
        element: <HomePage />,
      },
      {
        path: "catalog",
        element: <CatalogIndexPage />,
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
        element: <CollectionPage />,
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
        path: "terms",
        element: (
          <Suspense fallback={<PageLoadFallback />}>
            <TermsPage />
          </Suspense>
        ),
      },
      {
        path: "privacy",
        element: (
          <Suspense fallback={<PageLoadFallback />}>
            <PrivacyPage />
          </Suspense>
        ),
      },
      {
        path: "returns",
        element: (
          <Suspense fallback={<PageLoadFallback />}>
            <ReturnsPage />
          </Suspense>
        ),
      },
      {
        path: "delivery",
        element: (
          <Suspense fallback={<PageLoadFallback />}>
            <DeliveryInfoPage />
          </Suspense>
        ),
      },
      {
        path: "contacts",
        element: (
          <Suspense fallback={<PageLoadFallback />}>
            <ContactsPage />
          </Suspense>
        ),
      },
      {
        path: "reviews",
        element: (
          <Suspense fallback={<PageLoadFallback />}>
            <ReviewsPage />
          </Suspense>
        ),
      },
      {
        path: "faq",
        element: (
          <Suspense fallback={<PageLoadFallback />}>
            <FAQPage />
          </Suspense>
        ),
      },
      {
        path: "track-order",
        element: (
          <Suspense fallback={<PageLoadFallback />}>
            <TrackOrderPage />
          </Suspense>
        ),
      },
      {
        path: "payment-return",
        element: (
          <ErrorBoundary>
            <Suspense fallback={<PageLoadFallback />}>
              <PaymentReturnPage />
            </Suspense>
          </ErrorBoundary>
        ),
      },
      {
        path: "payment/success",
        element: (
          <ErrorBoundary>
            <Suspense fallback={<PageLoadFallback />}>
              <PaymentSuccessPage />
            </Suspense>
          </ErrorBoundary>
        ),
      },
      {
        path: "payment/failed",
        element: (
          <ErrorBoundary>
            <Suspense fallback={<PageLoadFallback />}>
              <PaymentFailedPage />
            </Suspense>
          </ErrorBoundary>
        ),
      },
      {
        path: "payment/cancelled",
        element: (
          <ErrorBoundary>
            <Suspense fallback={<PageLoadFallback />}>
              <PaymentCancelledPage />
            </Suspense>
          </ErrorBoundary>
        ),
      },
      {
        path: "wishlist",
        element: (
          <Suspense fallback={<PageLoadFallback />}>
            <WishlistPage />
          </Suspense>
        ),
      },
      {
        path: "*",
        element: (
          <Suspense fallback={<PageLoadFallback />}>
            <NotFoundPage />
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
    element: <ErrorBoundary><AuthShellLayout /></ErrorBoundary>,
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
        element: <ErrorBoundary><AdminLayout /></ErrorBoundary>,
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
            path: "categories",
            element: (
              <Suspense fallback={<PageLoadFallback />}>
                <AdminCategoriesPage />
              </Suspense>
            ),
          },
          {
            path: "collections",
            element: (
              <Suspense fallback={<PageLoadFallback />}>
                <AdminCollectionsPage />
              </Suspense>
            ),
          },
          {
            path: "designs",
            element: (
              <Suspense fallback={<PageLoadFallback />}>
                <AdminDesignsPage />
              </Suspense>
            ),
          },
          {
            path: "designs/:designId/variants",
            element: (
              <Suspense fallback={<PageLoadFallback />}>
                <AdminDesignVariantsPage />
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
          {
            path: "size-charts",
            element: (
              <Suspense fallback={<PageLoadFallback />}>
                <AdminSizeChartsPage />
              </Suspense>
            ),
          },
          {
            path: "payments",
            element: (
              <Suspense fallback={<PageLoadFallback />}>
                <AdminPaymentsPage />
              </Suspense>
            ),
          },
          {
            path: "exchange-rate",
            element: (
              <Suspense fallback={<PageLoadFallback />}>
                <AdminExchangeRatePage />
              </Suspense>
            ),
          },
          {
            path: "users",
            element: (
              <Suspense fallback={<PageLoadFallback />}>
                <AdminUsersPage />
              </Suspense>
            ),
          },
          {
            path: "site-settings",
            element: (
              <Suspense fallback={<PageLoadFallback />}>
                <AdminSiteSettingsPage />
              </Suspense>
            ),
          },
          {
            path: "reviews",
            element: (
              <Suspense fallback={<PageLoadFallback />}>
                <AdminReviewsPage />
              </Suspense>
            ),
          },
          {
            path: "coupons",
            element: (
              <Suspense fallback={<PageLoadFallback />}>
                <AdminCouponsPage />
              </Suspense>
            ),
          },
          {
            path: "garment-profiles",
            element: (
              <Suspense fallback={<PageLoadFallback />}>
                <AdminGarmentProfilesPage />
              </Suspense>
            ),
          },
        ],
      },
    ],
  },
]);

// Memoized so unrelated ancestor state changes (e.g. auth/currency resolving after
// mount) don't force a re-render cascade through the whole route tree — it still
// reacts to real navigation via RouterProvider's own internal subscription.
export const AppRouter = memo(function AppRouter() {
  return <RouterProvider router={router} />;
});
