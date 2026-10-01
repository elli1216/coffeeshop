import client from "./client";
import type { Category, CategoryRequest } from "../types";

export const getCategories = () =>
  client.get<Category[]>("/categories").then((r) => r.data);

export const getCategory = (id: number) =>
  client.get<Category>(`/categories/${id}`).then((r) => r.data);

export const createCategory = (data: CategoryRequest) =>
  client.post<Category>("/categories", data).then((r) => r.data);

export const updateCategory = (id: number, data: CategoryRequest) =>
  client.put<Category>(`/categories/${id}`, data).then((r) => r.data);

export const deleteCategory = (id: number) =>
  client.delete<void>(`/categories/${id}`).then(() => undefined);
