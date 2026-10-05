import { apiFetch } from "./api";
import type { ApiResponse } from "../types/common.types";

export interface ChatProduct {
  id: number;
  name: string;
  code: string;
  category: string;
  totalStock: number;
  url: string | null;
  imageUrl: string | null;
}

export interface ChatMessageResponse {
  reply: string;
  suggestions: string[];
  products: ChatProduct[];
}

export interface ChatHistoryItem {
  user: string;
  answer: string;
}

export interface ChatMessageRequest {
  message: string;
  history?: ChatHistoryItem[];
}

export interface ChatMessage {
  id: string;
  sender: "user" | "bot";
  text: string;
  timestamp: string;
  suggestions?: string[];
  products?: ChatProduct[];
}

export const INITIAL_BOT_MESSAGES: ChatMessage[] = [
  {
    id: "welcome-1",
    sender: "bot",
    text: "Xin chào! Tôi là **Trợ lý Kho Thông minh (Clothing AI Assistant)**. Tôi có thể hỗ trợ bạn tra cứu tồn kho, quy trình đơn hàng, nhà cung cấp hoặc hướng dẫn thao tác hệ thống.",
    timestamp: new Date().toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" }),
    suggestions: [
      "Kiểm tra mặt hàng sắp hết kho",
      "Quy trình tạo đơn mua hàng (PO)",
      "Danh sách nhà cung cấp vải uy tín",
      "Thống kê tổng tồn kho hiện tại",
    ],
  },
];

export async function sendChatMessage(
  text: string,
  history: ChatHistoryItem[] = []
): Promise<ChatMessage> {
  const response = await apiFetch<ApiResponse<ChatMessageResponse> | ChatMessageResponse>(
    "/chatbot/message",
    {
      method: "POST",
      body: JSON.stringify({
        message: text,
        history,
      }),
    }
  );

  const data: Partial<ChatMessageResponse> =
    response && typeof response === "object" && "data" in response && response.data
      ? response.data
      : (response as ChatMessageResponse) || {};

  const replyText = data.reply || "Tôi không nhận được phản hồi từ hệ thống trợ lý.";
  const suggestions = Array.isArray(data.suggestions) ? data.suggestions : [];
  const products = Array.isArray(data.products) ? data.products : [];

  return {
    id: `bot-${Date.now()}`,
    sender: "bot",
    text: replyText,
    timestamp: new Date().toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" }),
    suggestions: suggestions.length > 0 ? suggestions : undefined,
    products: products.length > 0 ? products : undefined,
  };
}
