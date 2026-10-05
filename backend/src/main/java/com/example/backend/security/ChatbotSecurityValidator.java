package com.example.backend.security;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

@Component
public class ChatbotSecurityValidator {

    // Danh sách mẫu regex phát hiện prompt injection và cố gắng leo quyền
    private static final List<Pattern> INJECTION_PATTERNS = List.of(
            Pattern.compile("(?i)(ignore|disregard|bypass)\\s+(all|any)?\\s*(previous|prior|above)?\\s*(instructions|directions|rules|prompts)"),
            Pattern.compile("(?i)(bỏ qua|quên|hủy bỏ)\\s+(hết|mọi|tất cả)?\\s*(hướng dẫn|chỉ dẫn|quy tắc|lệnh|prompt)\\s*(trước|cũ|trên)?"),
            Pattern.compile("(?i)(act as|pretend to be|you are now)\\s+(admin|administrator|root|developer|superuser)"),
            Pattern.compile("(?i)(đóng vai|giả lập|bạn là|hãy là)\\s+(admin|quản trị viên|root|developer|chủ hệ thống)"),
            Pattern.compile("(?i)(show|reveal|display|output|print)\\s+(system prompt|prompt gốc|hidden prompt|initial prompt)"),
            Pattern.compile("(?i)(tiết lộ|cho xem|hiển thị|đọc)\\s+(system prompt|prompt gốc|chỉ thị hệ thống|hướng dẫn ẩn)"),
            Pattern.compile("(?i)(jailbreak|dan mode|developer mode|dev mode)")
    );

    public Optional<String> validateInput(String message) {
        if (message == null || message.isBlank()) {
            return Optional.empty();
        }

        for (Pattern pattern : INJECTION_PATTERNS) {
            if (pattern.matcher(message).find()) {
                return Optional.of("Yêu cầu của bạn chứa cú pháp không được phép hoặc cố gắng thay đổi quy tắc vận hành của trợ lý. Vui lòng đặt câu hỏi liên quan đến nghiệp vụ kho hàng may mặc.");
            }
        }

        return Optional.empty();
    }

    // Khử các thẻ phá vỡ cấu trúc phân cách
    public String sanitizeInput(String message) {
        if (message == null) return "";
        return message
                .replace("<user_input>", "&lt;user_input&gt;")
                .replace("</user_input>", "&lt;/user_input&gt;")
                .replace("<suggestions>", "&lt;suggestions&gt;")
                .replace("</suggestions>", "&lt;/suggestions&gt;");
    }
}
