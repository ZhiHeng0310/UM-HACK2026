package com.agri.Controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class HealthController {

    /**
     * Health check endpoint - verify API key is loaded
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> response = new HashMap<>();
        
        String apiKey = System.getProperty("GEMINI_API_KEY");
        if (apiKey == null || apiKey.isEmpty()) {
            apiKey = System.getenv("GEMINI_API_KEY");
        }
        
        boolean isApiKeyLoaded = apiKey != null && !apiKey.isEmpty() && !apiKey.equals("mock_key_for_hackathon");
        
        response.put("status", "ok");
        response.put("service", "Agriwise Backend");
        response.put("timestamp", System.currentTimeMillis());
        response.put("apiKeyLoaded", isApiKeyLoaded);
        
        if (isApiKeyLoaded) {
            response.put("message", "✅ Gemini API is properly configured and ready");
            response.put("apiKeyPreview", apiKey.substring(0, Math.min(10, apiKey.length())) + "...");
        } else {
            response.put("message", "❌ ERROR: GEMINI_API_KEY is not set or is using mock value");
            response.put("apiKeyStatus", "NOT CONFIGURED");
        }
        
        return ResponseEntity.ok(response);
    }

    /**
     * System status endpoint - check environment variables
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus() {
        Map<String, Object> response = new HashMap<>();
        
        // Check various API key sources
        String fromProperty = System.getProperty("GEMINI_API_KEY");
        String fromEnv = System.getenv("GEMINI_API_KEY");
        
        response.put("timestamp", System.currentTimeMillis());
        response.put("javaVersion", System.getProperty("java.version"));
        response.put("userDir", System.getProperty("user.dir"));
        response.put("geminiApiKeyFromProperty", fromProperty != null && !fromProperty.isEmpty() ? "SET" : "NOT SET");
        response.put("geminiApiKeyFromEnvironment", fromEnv != null && !fromEnv.isEmpty() ? "SET" : "NOT SET");
        
        if (fromProperty != null && !fromProperty.isEmpty()) {
            response.put("activeApiKeySource", "System Property");
        } else if (fromEnv != null && !fromEnv.isEmpty()) {
            response.put("activeApiKeySource", "Environment Variable");
        } else {
            response.put("activeApiKeySource", "NOT CONFIGURED");
        }
        
        return ResponseEntity.ok(response);
    }
}
