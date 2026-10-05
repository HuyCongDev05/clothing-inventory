import { apiFetch } from "./api";
import type { ApiResponse } from "../types/common.types";

export interface MonthlyStockMovement {
  month: string;
  inbound: number;
  outbound: number;
  balance: number;
  inboundValue: number;
  outboundValue: number;
}

export interface CategoryStockDistribution {
  id: string;
  name: string;
  quantity: number;
  totalValue: number;
  percentage: number;
  color: string;
}

export interface StockHealthSegment {
  id: "safe" | "low" | "over";
  label: string;
  skuCount: number;
  percentage: number;
  color: string;
  description: string;
}

export interface OrderStatusDistribution {
  status: "RECEIVED" | "PENDING" | "DRAFT" | "CANCELLED";
  label: string;
  orderCount: number;
  totalAmount: number;
  percentage: number;
  color: string;
}

export interface TopValueProduct {
  id: string;
  productName: string;
  sku: string;
  category: string;
  quantity: number;
  unitPrice: number;
  totalValue: number;
}

export interface InventoryAnalyticsResponse {
  timeframe: string;
  monthlyMovements: MonthlyStockMovement[];
  categoryDistribution: CategoryStockDistribution[];
  stockHealthSegments: StockHealthSegment[];
  orderStatusDistribution: OrderStatusDistribution[];
  topValueProducts: TopValueProduct[];
}


export async function getInventoryAnalytics(
  timeframe: "6months" | "3months" | "year" = "6months",
): Promise<InventoryAnalyticsResponse> {
  const res = await apiFetch<ApiResponse<InventoryAnalyticsResponse>>(
    `/dashboard/analytics?timeframe=${timeframe}`,
  );
  return res.data;
}
