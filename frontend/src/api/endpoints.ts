import { api, API_BASE, unwrap } from "./client";
import type {
  Address,
  AdminOrderStatusUpdate,
  AdminUserResponse,
  ApiResponse,
  AuthTokens,
  Cart,
  Category,
  CreateAddressRequest,
  CreateCategoryRequest,
  CreateOrderRequest,
  CreateProductRequest,
  CreateStockRequest,
  LoginRequest,
  MergeCartResult,
  Order,
  OrderStatus,
  Payment,
  PaymentStatus,
  Product,
  RegisterRequest,
  StockResponse,
  UpdateProductRequest,
  UpdateStockRequest,
  UserInfo,
} from "@/types/api";

//  Auth 
export const authApi = {
  register: (body: RegisterRequest) =>
    api.post<ApiResponse<AuthTokens>>(`${API_BASE}/auth/register`, body).then(unwrap),
  login: (body: LoginRequest) =>
    api.post<ApiResponse<AuthTokens>>(`${API_BASE}/auth/login`, body).then(unwrap),
  me: () => api.get<ApiResponse<UserInfo>>(`${API_BASE}/auth/me`).then(unwrap),
  logout: (refreshToken: string) =>
    api.post<ApiResponse<void>>(`${API_BASE}/auth/logout`, { refreshToken }),
};

//  Products
export type ProductListParams = {
  categoryId?: string;
  minPrice?: number;
  maxPrice?: number;
  minRating?: number;
  search?: string;
  page?: number;
  size?: number;
  sort?: string;
};

export const productApi = {
  list: (params: ProductListParams = {}) =>
    api
      .get<ApiResponse<Product[]>>(`${API_BASE}/products`, { params })
      .then((r) => ({ items: r.data.data, page: r.data.page })),

  bySlug: (slug: string) =>
    api
      .get<ApiResponse<Product>>(`${API_BASE}/products/${slug}`)
      .then(unwrap),

  byIds: (ids: string[]) =>
    api
      .get<ApiResponse<Product[]>>(`${API_BASE}/products/batch`, {
        params: { ids: ids.join(",") },
      })
      .then(unwrap),
};

//  Categories 
export const categoryApi = {
  list: () =>
    api.get<ApiResponse<Category[]>>(`${API_BASE}/categories`).then(unwrap),
};

//  Cart 
export const cartApi = {
  get: () => api.get<ApiResponse<Cart>>(`${API_BASE}/cart`).then(unwrap),
  addItem: (productId: string, quantity: number) =>
    api
      .post<ApiResponse<Cart>>(`${API_BASE}/cart/items`, { productId, quantity })
      .then(unwrap),
  updateItem: (productId: string, quantity: number) =>
    api
      .put<ApiResponse<Cart>>(`${API_BASE}/cart/items/${productId}`, { quantity })
      .then(unwrap),
  removeItem: (productId: string) =>
    api
      .delete<ApiResponse<Cart>>(`${API_BASE}/cart/items/${productId}`)
      .then(unwrap),
  clear: () => api.delete<ApiResponse<void>>(`${API_BASE}/cart`),
  merge: (items: { productId: string; quantity: number }[]) =>
    api
      .post<ApiResponse<MergeCartResult>>(`${API_BASE}/cart/merge`, { items })
      .then(unwrap),
};

//  Addresses 
export const addressApi = {
  list: () =>
    api
      .get<ApiResponse<Address[]>>(`${API_BASE}/users/me/addresses`)
      .then(unwrap),
  create: (body: CreateAddressRequest) =>
    api
      .post<ApiResponse<Address>>(`${API_BASE}/users/me/addresses`, body)
      .then(unwrap),
  update: (id: string, body: CreateAddressRequest) =>
    api
      .put<ApiResponse<Address>>(`${API_BASE}/users/me/addresses/${id}`, body)
      .then(unwrap),
  remove: (id: string) =>
    api.delete<ApiResponse<void>>(`${API_BASE}/users/me/addresses/${id}`),
};

//  Orders 
export const orderApi = {
  create: (body: CreateOrderRequest) =>
    api.post<ApiResponse<Order>>(`${API_BASE}/orders`, body).then(unwrap),
  list: (page = 0, size = 20) =>
    api
      .get<ApiResponse<Order[]>>(`${API_BASE}/orders`, { params: { page, size } })
      .then((r) => ({ items: r.data.data, page: r.data.page })),
  byId: (id: string) =>
    api.get<ApiResponse<Order>>(`${API_BASE}/orders/${id}`).then(unwrap),
  cancel: (id: string) =>
    api.put<ApiResponse<Order>>(`${API_BASE}/orders/${id}/cancel`).then(unwrap),
};

//  Payments 
export const paymentApi = {
  byOrderId: (orderId: string) =>
    api
      .get<ApiResponse<Payment>>(`${API_BASE}/payments/orders/${orderId}`)
      .then(unwrap),
  myPayments: (page = 0, size = 20) =>
    api
      .get<ApiResponse<Payment[]>>(`${API_BASE}/payments/me`, {
        params: { page, size },
      })
      .then((r) => ({ items: r.data.data, page: r.data.page })),
};

//  Tags
export type TagResponse = {
  id: string;
  name: string;
  slug: string;
};

export const tagApi = {
  list: () =>
    api.get<ApiResponse<TagResponse[]>>(`${API_BASE}/tags`).then(unwrap),
  create: (body: { name: string }) =>
    api
      .post<ApiResponse<TagResponse>>(`${API_BASE}/tags`, body)
      .then(unwrap),
};

//  Admin: Products 
export type AdminProductListParams = ProductListParams & {
  includeInactive?: boolean;
};

export const adminProductApi = {
  list: (params: AdminProductListParams = {}) =>
    api
      .get<ApiResponse<Product[]>>(`${API_BASE}/admin/products`, {
        params: { includeInactive: true, ...params },
      })
      .then((r) => ({ items: r.data.data, page: r.data.page })),
  create: (body: CreateProductRequest) =>
    api
      .post<ApiResponse<Product>>(
        `${API_BASE}/products`,
        body
      )
      .then(unwrap),
  update: (id: string, body: UpdateProductRequest) =>
    api
      .put<ApiResponse<Product>>(
        `${API_BASE}/products/${id}`,
        body
      )
      .then(unwrap),
  remove: (id: string) =>
    api.delete<ApiResponse<void>>(`${API_BASE}/products/${id}`),
  restore: (id: string) =>
    api
      .put<ApiResponse<Product>>(
        `${API_BASE}/admin/products/${id}/restore`
      )
      .then(unwrap),
};

//  Admin: Categories 
export const adminCategoryApi = {
  create: (body: CreateCategoryRequest) =>
    api
      .post<ApiResponse<Category>>(
        `${API_BASE}/categories`,
        body
      )
      .then(unwrap),
  update: (id: string, body: CreateCategoryRequest) =>
    api
      .put<ApiResponse<Category>>(
        `${API_BASE}/categories/${id}`,
        body
      )
      .then(unwrap),
  remove: (id: string, targetCategoryId?: string) => {
    const qs = targetCategoryId ? `?targetCategoryId=${targetCategoryId}` : "";
    return api.delete<ApiResponse<void>>(
      `${API_BASE}/categories/${id}${qs}`
    );
  },
};

//  Admin: Stocks
export type AdminStockListParams = {
  page?: number;
  size?: number;
  productIds?: string[];
  sort?: string;
};

export const adminStockApi = {
  list: (params: AdminStockListParams = {}) => {
    const { productIds, ...rest } = params;
    const query: Record<string, unknown> = { ...rest };
    if (productIds && productIds.length > 0) {
      query.productIds = productIds.join(",");
    }
    return api
      .get<ApiResponse<StockResponse[]>>(`${API_BASE}/stocks`, { params: query })
      .then((r) => ({ items: r.data.data, page: r.data.page }));
  },
  stockedProductIds: () =>
    api
      .get<ApiResponse<string[]>>(`${API_BASE}/stocks/product-ids`)
      .then(unwrap),
  byProductId: (productId: string) =>
    api
      .get<ApiResponse<StockResponse>>(
        `${API_BASE}/stocks/${productId}`
      )
      .then(unwrap),
  create: (body: CreateStockRequest) =>
    api
      .post<ApiResponse<StockResponse>>(
        `${API_BASE}/stocks`,
        body
      )
      .then(unwrap),
  update: (productId: string, body: UpdateStockRequest) =>
    api
      .put<ApiResponse<StockResponse>>(
        `${API_BASE}/stocks/${productId}`,
        body
      )
      .then(unwrap),
  remove: (productId: string) =>
    api.delete<ApiResponse<void>>(`${API_BASE}/stocks/${productId}`),
};

//  Admin: Orders 
export type AdminOrderListParams = {
  status?: OrderStatus;
  userId?: string;
  from?: string;
  to?: string;
  search?: string;
  page?: number;
  size?: number;
  sort?: string;
};

export const adminOrderApi = {
  list: (params: AdminOrderListParams = {}) =>
    api
      .get<ApiResponse<Order[]>>(`${API_BASE}/admin/orders`, {
        params,
      })
      .then((r) => ({ items: r.data.data, page: r.data.page })),
  byId: (id: string) =>
    api
      .get<ApiResponse<Order>>(
        `${API_BASE}/admin/orders/${id}`
      )
      .then(unwrap),
  updateStatus: (
    id: string,
    body: AdminOrderStatusUpdate
  ) =>
    api
      .put<ApiResponse<Order>>(
        `${API_BASE}/admin/orders/${id}/status`,
        body
      )
      .then(unwrap),
  cancel: (id: string) =>
    api
      .put<ApiResponse<Order>>(`${API_BASE}/admin/orders/${id}/cancel`)
      .then(unwrap),
};

//  Admin: Payments
export type AdminPaymentListParams = {
  status?: PaymentStatus;
  userId?: string;
  search?: string;
  from?: string;
  to?: string;
  page?: number;
  size?: number;
  sort?: string;
};

export const adminPaymentApi = {
  list: (params: AdminPaymentListParams = {}) =>
    api
      .get<ApiResponse<Payment[]>>(
        `${API_BASE}/admin/payments`,
        { params }
      )
      .then((r) => ({ items: r.data.data, page: r.data.page })),
  refund: (orderId: string, reason: string) =>
    api
      .post<ApiResponse<Payment>>(
        `${API_BASE}/admin/payments/${orderId}/refund`,
        { reason }
      )
      .then(unwrap),
};

//  Admin: Users 
export type AdminUserListParams = {
  search?: string;
  role?: string;
  isActive?: boolean;
  page?: number;
  size?: number;
  sort?: string;
};

export const adminUserApi = {
  list: (params: AdminUserListParams = {}) =>
    api
      .get<ApiResponse<AdminUserResponse[]>>(
        `${API_BASE}/admin/users`,
        { params }
      )
      .then((r) => ({ items: r.data.data, page: r.data.page })),
  byId: (id: string) =>
    api
      .get<ApiResponse<AdminUserResponse>>(
        `${API_BASE}/admin/users/${id}`
      )
      .then(unwrap),
  updateRoles: (id: string, roles: string[]) =>
    api
      .put<ApiResponse<AdminUserResponse>>(
        `${API_BASE}/admin/users/${id}/roles`,
        { roles }
      )
      .then(unwrap),
  updateStatus: (id: string, isActive: boolean) =>
    api
      .put<ApiResponse<AdminUserResponse>>(
        `${API_BASE}/admin/users/${id}/status`,
        { isActive }
      )
      .then(unwrap),
};
