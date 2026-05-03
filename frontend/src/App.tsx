import { Navigate, Route, Routes } from "react-router-dom";
import { Suspense, lazy } from "react";
import { RootLayout } from "@/components/layout/RootLayout";
import { AccountLayout } from "@/components/layout/AccountLayout";
import { AdminLayout } from "@/components/layout/AdminLayout";
import { ProtectedRoute } from "@/routes/ProtectedRoute";
import { RequireRole } from "@/routes/RequireRole";
import { ErrorBoundary } from "@/components/ErrorBoundary";
import { FullPageSpinner } from "@/components/ui/spinner";
import { Roles } from "@/types/api";

const HomePage = lazy(() => import("@/pages/HomePage"));
const ProductListPage = lazy(() => import("@/pages/ProductListPage"));
const ProductDetailPage = lazy(() => import("@/pages/ProductDetailPage"));
const CartPage = lazy(() => import("@/pages/CartPage"));
const CheckoutPage = lazy(() => import("@/pages/CheckoutPage"));
const LoginPage = lazy(() => import("@/pages/LoginPage"));
const RegisterPage = lazy(() => import("@/pages/RegisterPage"));
const OrderListPage = lazy(() => import("@/pages/OrderListPage"));
const OrderDetailPage = lazy(() => import("@/pages/OrderDetailPage"));
const AccountAddressesPage = lazy(
  () => import("@/pages/AccountAddressesPage")
);
const AccountProfilePage = lazy(() => import("@/pages/AccountProfilePage"));
const PaymentHistoryPage = lazy(() => import("@/pages/PaymentHistoryPage"));
const NotFoundPage = lazy(() => import("@/pages/NotFoundPage"));
const FavoritesPage = lazy(() => import("@/pages/FavoritesPage"));

const AdminLoginPage = lazy(() => import("@/pages/admin/AdminLoginPage"));
const AdminDashboardPage = lazy(
  () => import("@/pages/admin/AdminDashboardPage")
);
const AdminProductsPage = lazy(() => import("@/pages/admin/AdminProductsPage"));
const AdminCategoriesPage = lazy(
  () => import("@/pages/admin/AdminCategoriesPage")
);
const AdminTagsPage = lazy(() => import("@/pages/admin/AdminTagsPage"));
const AdminStocksPage = lazy(() => import("@/pages/admin/AdminStocksPage"));
const AdminOrdersPage = lazy(() => import("@/pages/admin/AdminOrdersPage"));
const AdminUsersPage = lazy(() => import("@/pages/admin/AdminUsersPage"));
const AdminPaymentsPage = lazy(
  () => import("@/pages/admin/AdminPaymentsPage")
);
const AdminNotFoundPage = lazy(
  () => import("@/pages/admin/AdminNotFoundPage")
);

export default function App() {
  return (
    <ErrorBoundary>
      <Suspense fallback={<FullPageSpinner />}>
        <Routes>
          <Route element={<RootLayout />}>
            <Route index element={<HomePage />} />
            <Route path="products" element={<ProductListPage />} />
            <Route path="products/:slug" element={<ProductDetailPage />} />
            <Route path="cart" element={<CartPage />} />
            <Route path="favorites" element={<FavoritesPage />} />
            <Route path="login" element={<LoginPage />} />
            <Route path="register" element={<RegisterPage />} />
            <Route
              path="checkout"
              element={
                <ProtectedRoute>
                  <CheckoutPage />
                </ProtectedRoute>
              }
            />
            <Route
              element={
                <ProtectedRoute>
                  <AccountLayout />
                </ProtectedRoute>
              }
            >
              <Route path="orders" element={<OrderListPage />} />
              <Route path="orders/:id" element={<OrderDetailPage />} />
              <Route path="account">
                <Route index element={<Navigate to="profile" replace />} />
                <Route path="profile" element={<AccountProfilePage />} />
                <Route path="addresses" element={<AccountAddressesPage />} />
                <Route path="payments" element={<PaymentHistoryPage />} />
              </Route>
            </Route>
            <Route path="*" element={<NotFoundPage />} />
          </Route>

          <Route path="/admin/login" element={<AdminLoginPage />} />

          <Route
            path="/admin"
            element={
              <RequireRole role={Roles.ADMIN}>
                <AdminLayout />
              </RequireRole>
            }
          >
            <Route index element={<Navigate to="dashboard" replace />} />
            <Route path="dashboard" element={<AdminDashboardPage />} />
            <Route path="products" element={<AdminProductsPage />} />
            <Route path="categories" element={<AdminCategoriesPage />} />
            <Route path="tags" element={<AdminTagsPage />} />
            <Route path="stocks" element={<AdminStocksPage />} />
            <Route path="orders" element={<AdminOrdersPage />} />
            <Route path="payments" element={<AdminPaymentsPage />} />
            <Route path="users" element={<AdminUsersPage />} />
            <Route path="*" element={<AdminNotFoundPage />} />
          </Route>
        </Routes>
      </Suspense>
    </ErrorBoundary>
  );
}
