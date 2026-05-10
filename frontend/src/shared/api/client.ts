const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? "http://localhost:8080/api/v1";

export type Customer = {
  id: number;
  name: string;
  phone: string;
};

export type Product = {
  id: number;
  title: string;
  description?: string;
  price: number;
  imageUrl?: string | null;
  inStock: boolean;
};

export type Order = {
  id: number;
  customerName: string;
  customerPhone: string;
  totalPrice: number;
  createdAt: string;
};

async function request<T>(path: string): Promise<T> {
  const response = await fetch(`${API_BASE_URL}${path}`);
  if (!response.ok) {
    throw new Error(`Request failed: ${response.status}`);
  }
  return response.json() as Promise<T>;
}

export function getCustomers() {
  return request<Customer[]>("/customer");
}

export function getProducts() {
  return request<Product[]>("/product");
}

export function getOrders() {
  return request<Order[]>("/order");
}
