import client from "./client";
import type { Order, OrderRequest, OrderStatus } from "../types";

export const getOrders = (status?: OrderStatus) =>
  client
    .get<Order[]>("/orders", { params: status ? { status } : undefined })
    .then((r) => r.data);

export const getOrder = (id: number) =>
  client.get<Order>(`/orders/${id}`).then((r) => r.data);

export const createOrder = (data: OrderRequest) =>
  client.post<Order>("/orders", data).then((r) => r.data);

export const updateOrderStatus = (id: number, status: OrderStatus) =>
  client.patch<Order>(`/orders/${id}/status`, { status }).then((r) => r.data);

export const cancelOrder = (id: number) =>
  client.patch<Order>(`/orders/${id}/cancel`).then((r) => r.data);
