export type OrderStatus = "PENDING" | "PREPARING" | "COMPLETED" | "CANCELLED";

// Category
export interface Category {
  id: number;
  name: string;
  description: string | null;
}
export interface CategoryRequest {
  name: string;
  description?: string;
}

// Product
export interface Product {
  id: number;
  name: string;
  description: string | null;
  price: number;
  available: boolean;
  categoryId: number | null;
  categoryName: string | null;
}
export interface ProductRequest {
  name: string;
  description?: string;
  price: number;
  available?: boolean;
  categoryId: number;
}

// Order
export interface OrderItem {
  productId: number;
  productName: string;
  quantity: number;
  unitPrice: number;
  subtotal: number;
}
export interface Order {
  id: number;
  customerName: string;
  status: OrderStatus;
  totalAmount: number;
  createdAt: string; // ISO date-time from LocalDateTime
  items: OrderItem[];
}
export interface OrderItemRequest {
  productId: number;
  quantity: number;
}
export interface OrderRequest {
  customerName: string;
  items: OrderItemRequest[];
}

// Error body from GlobalExceptionHandler
export interface ProblemDetail {
  title?: string;
  status?: number;
  detail?: string;
  errors?: Record<string, string>;
}
