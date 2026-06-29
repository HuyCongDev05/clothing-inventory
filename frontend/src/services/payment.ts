import { apiFetch } from "./api";
import type { ApiResponse } from "../types/common.types";

// ─── Backend DTO Interfaces ───────────────────────────────────────────────────

/** Ánh xạ PaymentMethodResponseDto */
export interface BackendPaymentMethodResponse {
  id: number;
  code: string;
  name: string;
  status: string;
  createdAt: string;
  updatedAt: string;
}

/** Ánh xạ PaymentResponseDto */
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

/**
 * Ánh xạ PageResponseDto<PaymentResponseDto>.
 * Backend trả trực tiếp object này bên trong trường `data` của ApiResponse.
 */
export interface PaginatedPaymentsResponse {
  items: BackendPaymentResponse[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

/** Frontend-friendly interface cho một giao dịch thanh toán */
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
  createdByName: string;
  totalPaidAmount: number;
  remainingAmount: number;
  createdAt: string;
}

/** Frontend-friendly interface cho một phương thức thanh toán */
export interface PaymentMethod {
  id: number;
  code: string;
  name: string;
  status: string;
}

/** Phân trang danh sách giao dịch thanh toán */
export interface PaginatedPayments {
  items: PaymentRecord[];
  page: number;
  pageSize: number;
  totalElements: number;
  totalPages: number;
}

/** Request body để tạo thanh toán — ánh xạ PaymentRequestDto */
export interface PaymentCreateRequestDto {
  purchaseOrderId: number;
  paymentMethodId: number;
  paymentDate: string; // ISO 8601 LocalDateTime
  amount: number;
  note?: string;
}

// ─── Mapper Functions ─────────────────────────────────────────────────────────

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

// ─── Service Functions ────────────────────────────────────────────────────────

/**
 * Lấy danh sách phương thức thanh toán.
 * Backend: GET /payment-methods
 * Response: ApiResponse<List<PaymentMethodResponseDto>>
 */
export async function getPaymentMethods(): Promise<PaymentMethod[]> {
  const response = await apiFetch<ApiResponse<BackendPaymentMethodResponse[]>>(
    "/payment-methods",
  );
  return (response.data || []).map(mapBackendPaymentMethodToFrontend);
}

/**
 * Tạo một giao dịch thanh toán mới.
 * Backend: POST /payments
 * Response: ApiResponse<PaymentResponseDto>
 */
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

/**
 * Lấy lịch sử thanh toán theo mã đơn đặt hàng.
 * Backend: GET /payments/purchase-order/{purchaseOrderId}?page=
 * Response: ApiResponse<PageResponseDto<PaymentResponseDto>>
 */
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
