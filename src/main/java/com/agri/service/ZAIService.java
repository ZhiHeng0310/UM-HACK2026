package com.agri.service;

import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;
import org.springframework.beans.factory.annotation.Value;

import java.util.*;

@Service
public class ZAIService {

    private static final String MODEL = "gemma-3-27b-it";
    private static final String API_URL_TEMPLATE =
            "https://generativelanguage.googleapis.com/v1beta/models/%s:generateContent?key=%s";

    @Value("${GEMINI_API_KEY:${ZAI_API_KEY:}}")
    private String API_KEY;

    private final RestTemplate restTemplate = new RestTemplate();

    public String getAIResponse(String userMsg, String crop, String marketData, String newsData) {
        String prompt = "You are an agricultural expert.\n\n"
                + "User Crop: " + crop + "\n"
                + "User Question: " + userMsg + "\n\n"
                + "Market Data:\n" + marketData + "\n\n"
                + "Field News:\n" + newsData + "\n\n"
                + "Give clear, practical farming advice.";

        Map<String, Object> body = new HashMap<>();
        Map<String, Object> part = new HashMap<>();
        part.put("text", prompt);
        Map<String, Object> content = new HashMap<>();
        content.put("parts", List.of(part));
        body.put("contents", List.of(content));
        body.put("generationConfig", Map.of("maxOutputTokens", 1024, "temperature", 0.3));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        try {
            String apiUrl = String.format(API_URL_TEMPLATE, MODEL, API_KEY);
            ResponseEntity<Map> response = restTemplate.postForEntity(apiUrl, request, Map.class);
            Map<String, Object> responseBody = response.getBody();

            if (responseBody == null) return "Error: Empty response body";

            List<Map<String, Object>> candidates = (List<Map<String, Object>>) responseBody.get("candidates");
            if (candidates != null && !candidates.isEmpty()) {
                Map<String, Object> firstCandidate = candidates.get(0);
                Map<String, Object> candidateContent = (Map<String, Object>) firstCandidate.get("content");
                if (candidateContent != null) {
                    List<Map<String, Object>> parts = (List<Map<String, Object>>) candidateContent.get("parts");
                    if (parts != null && !parts.isEmpty() && parts.get(0).get("text") != null) {
                        return parts.get(0).get("text").toString();
                    }
                }
            }

            return "Error: Could not parse Gemini response.";

        } catch (HttpClientErrorException e) {
            return "API Error (" + e.getStatusCode() + "): " + e.getResponseBodyAsString();
        } catch (Exception e) {
            e.printStackTrace();
            return "System Error: " + e.getMessage();
        }
    }
}
