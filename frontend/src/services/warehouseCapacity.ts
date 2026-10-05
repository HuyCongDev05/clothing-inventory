import { apiFetch } from "./api";
import type { ApiResponse } from "../types/common.types";

export type WarehouseCapacityStatus = "SAFE" | "MODERATE" | "WARNING" | "FULL";

export interface WarehouseCapacityResponse {
  maxCapacity: number;
  currentInventory: number;
  remainingCapacity: number;
  occupancyRate: number;
  warningThreshold: number;
  status: WarehouseCapacityStatus;
  statusMessage: string;
  isWarning?: boolean;
  isFull?: boolean;
}

export interface UpdateWarehouseCapacityPayload {
  maxCapacity: number;
  warningThreshold: number;
}

/**
 * Lấy thông tin dung lượng và sức chứa kho hàng
 * GET /api/v1/settings/warehouse-capacity
 */
export async function getWarehouseCapacity(): Promise<WarehouseCapacityResponse> {
  const response = await apiFetch<ApiResponse<WarehouseCapacityResponse> | WarehouseCapacityResponse>(
    "/settings/warehouse-capacity"
  );
  if (response && typeof response === "object" && "data" in response && response.data) {
    return response.data;
  }
  return response as WarehouseCapacityResponse;
}

/**
 * Cập nhật cấu hình sức chứa tối đa và ngưỡng cảnh báo kho hàng (Chỉ dành cho Admin)
 * PUT /api/v1/settings/warehouse-capacity
 */
export async function updateWarehouseCapacity(
  payload: UpdateWarehouseCapacityPayload
): Promise<WarehouseCapacityResponse> {
  const response = await apiFetch<ApiResponse<WarehouseCapacityResponse> | WarehouseCapacityResponse>(
    "/settings/warehouse-capacity",
    {
      method: "PUT",
      body: JSON.stringify(payload),
    }
  );
  if (response && typeof response === "object" && "data" in response && response.data) {
    return response.data;
  }
  return response as WarehouseCapacityResponse;
}
