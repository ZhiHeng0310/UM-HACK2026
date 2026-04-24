package com.agri.service;

import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;
import org.springframework.beans.factory.annotation.Value;

import java.util.*;

@Service
public class ZAIService {

    private final String API_URL = "https://api.ilmu.ai/anthropic/v1/messages";

    @Value("${ZAI_API_KEY}")
    private String API_KEY;

    private final RestTemplate restTemplate = new RestTemplate();

    public String getAIResponse(String userMsg, String crop, String marketData, String newsData) {
        String prompt = "You are Z.AI, an agricultural expert.\n\n"
                + "User Crop: " + crop + "\n"
                + "User Question: " + userMsg + "\n\n"
                + "Market Data:\n" + marketData + "\n\n"
                + "Field News:\n" + newsData + "\n\n"
                + "Give clear, practical farming advice.";

        Map<String, Object> body = new HashMap<>();
        body.put("model", "ilmu-glm-5.1"); // Ensure this is a valid model name
        body.put("max_tokens", 1024);      // Anthropic-style APIs often require this

        List<Map<String, String>> messages = new ArrayList<>();
        Map<String, String> msg = new HashMap<>();
        msg.put("role", "user");
        msg.put("content", prompt);
        messages.add(msg);
        body.put("messages", messages);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + API_KEY);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(API_URL, request, Map.class);
            Map<String, Object> responseBody = response.getBody();

            if (responseBody == null) return "Error: Empty response body";

            // ANTHROPIC PARSING LOGIC:
            // The response usually looks like: { "content": [ { "text": "...", "type": "text" } ] }
            List<Map<String, Object>> contentList = (List<Map<String, Object>>) responseBody.get("content");
            
            if (contentList != null && !contentList.isEmpty()) {
                return contentList.get(0).get("text").toString();
            }

            // FALLBACK: If they are using OpenAI-style mapping over an Anthropic URL
            List<Map<String, Object>> choices = (List<Map<String, Object>>) responseBody.get("choices");
            if (choices != null && !choices.isEmpty()) {
                Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
                return message.get("content").toString();
            }

            return "Error: Could not parse response. Check API documentation for 'ilmu.ai'.";

        } catch (HttpClientErrorException e) {
            return "API Error (" + e.getStatusCode() + "): " + e.getResponseBodyAsString();
        } catch (Exception e) {
            e.printStackTrace();
            return "System Error: " + e.getMessage();
        }
    }
}