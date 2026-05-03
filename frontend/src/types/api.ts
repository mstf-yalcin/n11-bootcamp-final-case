export type ErrorDetail = {
  field?: string;
  message: string;
};

export type PageableInfo = {
  pageNumber: number;
  pageSize: number;
  totalElements: number;
  totalPages: number;
  hasNext: boolean;
  hasPrevious: boolean;
};

export type ApiResponse<T> = {
  success: boolean;
  data: T;
  message?: string;
  errorCode?: string;
  page?: PageableInfo;
  errors?: ErrorDetail[];
};

export type Category = {
  id: string;
  name: string;
  slug: string;
  description?: string;
  imageUrl?: string;
  createdAt?: string;
  updatedAt?: string;
};

export type Tag = {
  id: string;
  name: string;
  slug: string;
};

export type StockStatus = "IN_STOCK" | "LOW_STOCK" | "OUT_OF_STOCK" | "UNKNOWN";

export type Product = {
  id: string;
  slug: string;
  name: string;
  description?: string;
  price: number;
  currency: string;
  ratingCount: number;
  ratingAverage: number;
  imageUrl?: string;
  tags: Tag[];
  categoryId: string;
  categoryName: string;
  createdAt?: string;
  updatedAt?: string;
  stockStatus?: StockStatus;
  availableQuantity?: number | null;
  isActive?: boolean;
};

export type CartItem = {
  productId: string;
  productName: string;
  imageUrl?: string;
  unitPrice: number;
  currency: string;
  quantity: number;
  subtotal: number;
  stockStatus?: StockStatus;
  availableQuantity?: number | null;
};

export type Cart = {
  userId: string;
  items: CartItem[];
  totalAmount: number;
  currency: string;
  updatedAt: string;
};

export type MergeCartResult = {
  cart: Cart;
  skippedProductIds: string[];
};

export type Address = {
  id: string;
  title: string;
  contactName: string;
  fullAddress: string;
  city: string;
  district?: string;
  country?: string;
  zipCode?: string;
  phone?: string;
  isDefault: boolean;
};

export type CreateAddressRequest = {
  title: string;
  contactName: string;
  fullAddress: string;
  city: string;
  district?: string;
  country?: string;
  zipCode?: string;
  phone?: string;
  isDefault?: boolean;
};

export type UserInfo = {
  id: string;
  email: string;
  firstName: string;
  lastName: string;
  phone?: string;
  roles: string[];
};

export type AuthTokens = {
  accessToken: string;
  refreshToken: string;
};

export type RegisterRequest = {
  email: string;
  password: string;
  firstName: string;
  lastName: string;
  phone?: string;
};

export type LoginRequest = {
  email: string;
  password: string;
};

export type OrderStatus =
  | "PENDING"
  | "STOCK_RESERVED"
  | "PAYMENT_PROCESSING"
  | "CONFIRMED"
  | "SHIPPED"
  | "DELIVERED"
  | "CANCELLED";

export type CancelReason =
  | "STOCK_UNAVAILABLE"
  | "PAYMENT_FAILED"
  | "USER_CANCELLED"
  | "ADMIN_CANCELLED"
  | "TIMEOUT";

export type OrderItem = {
  productId: string;
  productName: string;
  unitPrice: number;
  quantity: number;
  currency: string;
  subtotal: number;
};

export type Order = {
  id: string;
  userId: string;
  status: OrderStatus;
  cancelReason?: CancelReason;
  totalAmount: number;
  currency: string;
  addressId: string;
  items: OrderItem[];
  createdAt: string;
  updatedAt: string;
  buyerEmail?: string;
  buyerFullName?: string;
  shippingCity?: string;
  shippingDistrict?: string;
};

export type CreateOrderRequest = {
  addressId: string;
  identityNumber: string;
};

export type PaymentStatus =
  | "PENDING"
  | "COMPLETED"
  | "FAILED"
  | "CANCELLED"
  | "REFUNDED";

export type Payment = {
  id: string;
  orderId: string;
  userId: string;
  amount: number;
  currency: string;
  status: PaymentStatus;
  provider: string;
  providerPaymentId?: string;
  errorCode?: string;
  errorMessage?: string;
  createdAt: string;
  updatedAt: string;
};

export type AnonymousCartItem = {
  productId: string;
  quantity: number;
};

//  Admin 

export type AdminUserResponse = {
  id: string;
  email: string;
  firstName: string;
  lastName: string;
  phone?: string;
  roles: string[];
  isActive: boolean;
  createdAt: string;
  lastLoginAt?: string;
};

export type StockResponse = {
  id: string;
  productId: string;
  quantity: number;
  reserved: number;
  available: number;
  updatedAt: string;
};

export type CreateProductRequest = {
  name: string;
  description?: string;
  price: number;
  currency: string;
  imageUrl: string;
  categoryId: string;
  tagIds?: string[];
};

export type UpdateProductRequest = CreateProductRequest;

export type CreateCategoryRequest = {
  name: string;
  description?: string;
  imageUrl: string;
};

export type CreateTagRequest = {
  name: string;
};

export type CreateStockRequest = {
  productId: string;
  quantity: number;
};

export type UpdateStockRequest = {
  quantity: number;
};

export type AdminOrderStatusUpdate = {
  status: OrderStatus;
};

export const Roles = {
  USER: "USER",
  ADMIN: "ADMIN",
} as const;

export type Role = (typeof Roles)[keyof typeof Roles];
