import { apiFetch } from "./api";
import type { ApiResponse } from "../types/common.types";



// Phương thức thanh toán từ backend
export interface BackendPaymentMethodResponse {
  id: number;
  code: string;
  name: string;
  status: string;
  createdAt: string;
  updatedAt: string;
}

// Giao dịch thanh toán từ backend
export interface BackendPaymentResponse {
  id: number;
  purchaseOrderId: number;
  purchaseOrderCode: string;
  paymentMethodId: number;
  paymentMethodCode: string;
  paymentMethodName: string;
  paymentDate: string;
  amount: number;
  note: string | null;
  createdById: number;
  createdByName: string;
  totalPaidAmount: number;
  remainingAmount: number;
  createdAt: string;
  updatedAt: string;
}

// Phân trang danh sách thanh toán từ backend
export interface PaginatedPaymentsResponse {
  items: BackendPaymentResponse[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

// Giao dịch thanh toán ở frontend
export interface PaymentRecord {
  id: number;
  purchaseOrderId: number;
  purchaseOrderCode: string;
  paymentMethodId: number;
  paymentMethodCode: string;
  paymentMethodName: string;
  paymentDate: string;
  amount: number;
  note: string | null;
  createdById: number;
  createdByName: string;
  totalPaidAmount: number;
  remainingAmount: number;
  createdAt: string;
}

// Phương thức thanh toán ở frontend
export interface PaymentMethod {
  id: number;
  code: string;
  name: string;
  status: string;
}

// Phân trang danh sách thanh toán ở frontend
export interface PaginatedPayments {
  items: PaymentRecord[];
  page: number;
  pageSize: number;
  totalElements: number;
  totalPages: number;
}

// Dữ liệu tạo giao dịch thanh toán
export interface PaymentCreateRequestDto {
  purchaseOrderId: number;
  paymentMethodId: number;
  paymentDate: string; // ISO 8601 LocalDateTime
  amount: number;
  note?: string;
}



function mapBackendPaymentToFrontend(p: BackendPaymentResponse): PaymentRecord {
  return {
    id: p.id,
    purchaseOrderId: p.purchaseOrderId,
    purchaseOrderCode: p.purchaseOrderCode,
    paymentMethodId: p.paymentMethodId,
    paymentMethodCode: p.paymentMethodCode,
    paymentMethodName: p.paymentMethodName,
    paymentDate: p.paymentDate,
    amount: Number(p.amount) || 0,
    note: p.note ?? null,
    createdById: p.createdById,
    createdByName: p.createdByName,
    totalPaidAmount: Number(p.totalPaidAmount) || 0,
    remainingAmount: Number(p.remainingAmount) || 0,
    createdAt: p.createdAt,
  };
}

function mapBackendPaymentMethodToFrontend(
  m: BackendPaymentMethodResponse,
): PaymentMethod {
  return {
    id: m.id,
    code: m.code,
    name: m.name,
    status: m.status,
  };
}



// Lấy danh sách phương thức thanh toán
export async function getPaymentMethods(): Promise<PaymentMethod[]> {
  const response = await apiFetch<ApiResponse<BackendPaymentMethodResponse[]>>(
    "/payment-methods",
  );
  return (response.data || []).map(mapBackendPaymentMethodToFrontend);
}

// Tạo giao dịch thanh toán mới
export async function createPayment(
  payload: PaymentCreateRequestDto,
): Promise<PaymentRecord> {
  const response = await apiFetch<ApiResponse<BackendPaymentResponse>>(
    "/payments",
    {
      method: "POST",
      body: JSON.stringify(payload),
    },
  );
  return mapBackendPaymentToFrontend(response.data);
}

// Lấy lịch sử thanh toán theo ID đơn hàng
export async function getPaymentHistoryByPurchaseOrderId(
  purchaseOrderId: number,
  page: number = 1,
): Promise<PaginatedPayments> {
  const response = await apiFetch<ApiResponse<PaginatedPaymentsResponse>>(
    `/payments/purchase-order/${purchaseOrderId}?page=${page}`,
  );
  const data = response.data;
  return {
    items: (data.items || []).map(mapBackendPaymentToFrontend),
    page: data.page,
    pageSize: data.size,
    totalElements: data.totalElements,
    totalPages: data.totalPages,
  };
}
