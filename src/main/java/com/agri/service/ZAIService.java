package com.agri.service;

import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;
import org.springframework.beans.factory.annotation.Value;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import jakarta.annotation.PostConstruct;

import java.util.*;

@Service
public class ZAIService {

    private final String API_URL_TEMPLATE = "https://generativelanguage.googleapis.com/v1beta/models/%s:generateContent";
    private final String MODEL = "gemma-3-27b-it";

    @Value("${GEMINI_API_KEY:}")
    private String API_KEY;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper mapper = new ObjectMapper();

    @PostConstruct
    public void initializeApiKey() {
        // Try to load API key from multiple sources
        if (API_KEY == null || API_KEY.trim().isEmpty()) {
            // Try System property first
            API_KEY = System.getProperty("GEMINI_API_KEY");
            if (API_KEY != null && !API_KEY.isEmpty()) {
                System.out.println("[INFO] Loaded GEMINI_API_KEY from System properties");
                return;
            }

            // Try environment variable
            API_KEY = System.getenv("GEMINI_API_KEY");
            if (API_KEY != null && !API_KEY.isEmpty()) {
                System.out.println("[INFO] Loaded GEMINI_API_KEY from environment variables");
                return;
            }

            System.err.println("[ERROR] GEMINI_API_KEY not found in any source (Spring @Value, System properties, or environment variables)");
        } else {
            System.out.println("[INFO] GEMINI_API_KEY loaded from Spring @Value: " + API_KEY.substring(0, Math.min(10, API_KEY.length())) + "...");
        }
    }

    public String getAIResponse(String userMsg, String crop, String marketData, String newsData) {
        String prompt = "You are an agricultural expert specializing in crop management and optimization.\n\n"
                + "User Crop: " + crop + "\n"
                + "User Question: " + userMsg + "\n\n"
                + "Market Data:\n" + marketData + "\n\n"
                + "Field News:\n" + newsData + "\n\n"
                + "Give clear, practical farming advice.";

        try {
            // Build Gemini API request body
            ObjectNode requestBody = mapper.createObjectNode();
            
            // Gemini uses "contents" instead of "messages"
            ArrayNode contents = mapper.createArrayNode();
            ObjectNode contentObj = mapper.createObjectNode();
            contentObj.put("role", "user");
            
            ArrayNode parts = mapper.createArrayNode();
            ObjectNode part = mapper.createObjectNode();
            part.put("text", prompt);
            parts.add(part);
            
            contentObj.set("parts", parts);
            contents.add(contentObj);
            requestBody.set("contents", contents);

            // Add generation config
            ObjectNode generationConfig = mapper.createObjectNode();
            generationConfig.put("temperature", 0.7);
            generationConfig.put("maxOutputTokens", 1024);
            requestBody.set("generationConfig", generationConfig);

            // Build URL with API key
            String apiUrl = String.format(API_URL_TEMPLATE, MODEL) + "?key=" + API_KEY;

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<String> request = new HttpEntity<>(mapper.writeValueAsString(requestBody), headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(apiUrl, request, Map.class);
            Map<String, Object> responseBody = response.getBody();

            if (responseBody == null) return "Error: Empty response body";

            // Parse Gemini response format: { "candidates": [ { "content": { "parts": [ { "text": "..." } ] } } ] }
            List<Map<String, Object>> candidates = (List<Map<String, Object>>) responseBody.get("candidates");
            
            if (candidates != null && !candidates.isEmpty()) {
                Map<String, Object> candidate = candidates.get(0);
                Map<String, Object> content = (Map<String, Object>) candidate.get("content");
                if (content != null) {
                    List<Map<String, Object>> partsList = (List<Map<String, Object>>) content.get("parts");
                    if (partsList != null && !partsList.isEmpty()) {
                        return partsList.get(0).get("text").toString();
                    }
                }
            }

            return "Error: Could not parse Gemini response. Check API documentation.";

        } catch (HttpClientErrorException e) {
            return "API Error (" + e.getStatusCode() + "): " + e.getResponseBodyAsString();
        } catch (Exception e) {
            e.printStackTrace();
            return "System Error: " + e.getMessage();
        }
    }
}