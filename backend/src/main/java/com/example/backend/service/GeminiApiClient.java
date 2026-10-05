package com.example.backend.service;

import com.example.backend.dto.request.ChatHistoryItem;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class GeminiApiClient {

    private final RestTemplate geminiRestTemplate;
    private final ObjectMapper objectMapper;

    @Value("${gemini.api.key:}")
    private String apiKey;

    @Value("${gemini.api.model}")
    private String model;

    @Value("${gemini.api.base-url}")
    private String baseUrl;

    public String generateContent(String systemPrompt, List<ChatHistoryItem> history, String userMessage) {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            log.warn("Gemini API Key chưa được cấu hình. Vui lòng đặt GEMINI_API_KEY trong biến môi trường.");
            return "Hệ thống Trợ lý AI chưa được cấu hình Gemini API Key. Quản trị viên vui lòng cấu hình GEMINI_API_KEY để kích hoạt tính năng.<suggestions>[\"Kiểm tra cấu hình hệ thống\", \"Xem thông tin phiên bản\"]</suggestions>";
        }

        try {
            return doGenerateContent(this.model, systemPrompt, history, userMessage);
        } catch (HttpClientErrorException.NotFound e) {
            log.warn("Model '{}' không tìm thấy (404). Đang kiểm tra danh sách model khả dụng...", this.model);
            List<String> availableModels = getAvailableModels();

            if (!availableModels.isEmpty()) {
                log.info("Các model Gemini khả dụng trên API Key: {}", availableModels);

                // Tự động chuyển model dự phòng khi model chính trả về 404
                String fallbackModel = selectBestModel(availableModels);
                if (fallbackModel != null && !fallbackModel.equalsIgnoreCase(this.model)) {
                    log.info("Tự động chuyển sang model: {}", fallbackModel);
                    try {
                        String result = doGenerateContent(fallbackModel, systemPrompt, history, userMessage);
                        this.model = fallbackModel;
                        return result;
                    } catch (Exception retryEx) {
                        log.error("Thử lại với model {} thất bại: {}", fallbackModel, retryEx.getMessage());
                    }
                }

                return "Model AI hiện tại (" + this.model + ") không khả dụng. Danh sách model hỗ trợ: "
                        + availableModels + ". Vui lòng cập nhật GEMINI_API_MODEL.<suggestions>[\"Tồn kho hiện tại\", \"Mặt hàng sắp hết\"]</suggestions>";
            }

            return "Không tìm thấy model AI phù hợp trên máy chủ Google Gemini. Vui lòng kiểm tra lại API Key và cấu hình model.<suggestions>[\"Tồn kho hiện tại\"]</suggestions>";
        } catch (Exception e) {
            log.error("Lỗi khi gọi Gemini API: {}", e.getMessage(), e);
            return "Hệ thống AI hiện đang bận hoặc gặp sự cố kết nối. Vui lòng thử lại sau.<suggestions>[\"Tồn kho hiện tại\", \"Mặt hàng sắp hết\"]</suggestions>";
        }
    }

    private String doGenerateContent(String targetModel, String systemPrompt, List<ChatHistoryItem> history, String userMessage) throws Exception {
        String requestUrl = baseUrl + "/models/" + targetModel + ":generateContent?key=" + apiKey.trim();

        ObjectNode rootNode = objectMapper.createObjectNode();

        if (systemPrompt != null && !systemPrompt.isBlank()) {
            ObjectNode systemInstruction = rootNode.putObject("systemInstruction");
            ArrayNode partsNode = systemInstruction.putArray("parts");
            partsNode.addObject().put("text", systemPrompt);
        }

        ArrayNode contentsNode = rootNode.putArray("contents");
        if (history != null && !history.isEmpty()) {
            for (ChatHistoryItem item : history) {
                // Tự gắn role "user" trong code cho câu hỏi trước đó
                if (item.getQuestion() != null && !item.getQuestion().isBlank()) {
                    ObjectNode userNode = contentsNode.addObject();
                    userNode.put("role", "user");
                    userNode.putArray("parts").addObject().put("text", "<user_input>" + item.getQuestion().trim() + "</user_input>");
                }
                // Tự gắn role "model" trong code cho câu trả lời trước đó
                if (item.getAnswer() != null && !item.getAnswer().isBlank()) {
                    ObjectNode modelNode = contentsNode.addObject();
                    modelNode.put("role", "model");
                    modelNode.putArray("parts").addObject().put("text", item.getAnswer().trim());
                }
            }
        }

        // Tự gắn role "user" trong code cho tin nhắn hiện tại
        ObjectNode currentMsgNode = contentsNode.addObject();
        currentMsgNode.put("role", "user");
        currentMsgNode.putArray("parts").addObject().put("text", userMessage);

        ObjectNode generationConfig = rootNode.putObject("generationConfig");
        generationConfig.put("temperature", 0.2);
        generationConfig.put("maxOutputTokens", 1024);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> entity = new HttpEntity<>(objectMapper.writeValueAsString(rootNode), headers);

        ResponseEntity<String> response = geminiRestTemplate.postForEntity(requestUrl, entity, String.class);

        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            JsonNode resJson = objectMapper.readTree(response.getBody());
            JsonNode candidates = resJson.path("candidates");
            if (candidates.isArray() && !candidates.isEmpty()) {
                JsonNode textNode = candidates.get(0).path("content").path("parts").get(0).path("text");
                if (!textNode.isMissingNode()) {
                    return textNode.asText();
                }
            }
        }

        log.error("Gemini API trả về response không hợp lệ: {}", response.getBody());
        return "Xin lỗi, hiện tại tôi chưa thể xử lý câu trả lời từ máy chủ AI. Vui lòng thử lại sau giây lát.<suggestions>[\"Tồn kho hiện tại\", \"Mặt hàng sắp hết\"]</suggestions>";
    }

    // Lấy danh sách model khả dụng hỗ trợ sinh văn bản
    public List<String> getAvailableModels() {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            return Collections.emptyList();
        }

        try {
            String requestUrl = baseUrl + "/models?key=" + apiKey.trim();
            ResponseEntity<String> response = geminiRestTemplate.getForEntity(requestUrl, String.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode root = objectMapper.readTree(response.getBody());
                JsonNode modelsArray = root.path("models");
                List<String> result = new ArrayList<>();

                if (modelsArray.isArray()) {
                    for (JsonNode m : modelsArray) {
                        JsonNode methods = m.path("supportedGenerationMethods");
                        boolean supportsGenerate = false;
                        if (methods.isArray()) {
                            for (JsonNode method : methods) {
                                if ("generateContent".equals(method.asText())) {
                                    supportsGenerate = true;
                                    break;
                                }
                            }
                        }
                        if (supportsGenerate) {
                            String name = m.path("name").asText().replace("models/", "");
                            result.add(name);
                        }
                    }
                }
                return result;
            }
        } catch (Exception e) {
            log.error("Lỗi khi truy vấn danh sách model Gemini: {}", e.getMessage());
        }

        return Collections.emptyList();
    }

    // Ưu tiên các model Flash có tốc độ phản hồi nhanh
    private String selectBestModel(List<String> models) {
        if (models == null || models.isEmpty()) return null;

        for (String m : models) {
            if (m.contains("2.5") && m.contains("flash") && !m.contains("audio") && !m.contains("tts") && !m.contains("image")) {
                return m;
            }
        }
        for (String m : models) {
            if (m.contains("3.5") && m.contains("flash") && !m.contains("transcribe")) {
                return m;
            }
        }
        for (String m : models) {
            if (m.contains("flash") && !m.contains("audio") && !m.contains("tts") && !m.contains("image") && !m.contains("transcribe")) {
                return m;
            }
        }
        return models.get(0);
    }
}
