package com.agri.engine;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * Part 4 - Decision Core
 *
 * GlmClient manages a single HTTP POST to Google's Gemini GenerateContent endpoint.
 * It is intentionally minimal: its only job is to send a prompt and return
 * the raw content string from the GLM's first response choice.
 *
 * All JSON building / parsing of the outer Gemini envelope is done here with Jackson.
 * Parsing the *content* (the agri recommendation) is delegated to ZaiRationaleGenerator.
 *
 * Dependencies: Jackson Databind (already referenced in Part 8 / AppConfig).
 */
public class GlmClient {

    // Low temperature → more deterministic JSON output from the GLM
    
    

    // ── Gemini endpoint & model ───────────────────────────────────────────────────
    private static final String API_URL_TEMPLATE =
            "https://generativelanguage.googleapis.com/v1beta/models/%s:generateContent?key=%s";
    private static final String MODEL   = "gemma-3-27b-it";

    // Low temperature → more deterministic JSON output from the GLM
    private static final double TEMPERATURE     = 0.3;
    // ── Increase the patience for high-quality responses ──────────────────────────
private static final int CONNECT_TIMEOUT = 30_000;  // 30 seconds to connect
private static final int READ_TIMEOUT    = 180_000; // 180 seconds (3 minutes) to wait for the full reply
    private static final int    MAX_TOKENS      = 1024; // <-- Add this constant

    private final String       apiKey;
    private final ObjectMapper mapper;

    /**
     * @param apiKey Your Gemini API key. Retrieve this from AppConfig – never hardcode it.
     */
    public GlmClient(String apiKey) {
        this.apiKey  = apiKey;
        this.mapper  = new ObjectMapper();
    }

    /**
     * Sends {@code prompt} to the GLM and returns the plain-text content of
     * the first choice in the API response.
     *
     * @param prompt The full prompt string produced by PromptBuilder.
     * @return       The raw content string from the AI (expected to be JSON).
     * @throws IOException If the network call fails or the API returns an error status.
     */
    public String call(String prompt) throws IOException {

        // ── 1. Build the request JSON body ────────────────────────────────────────
        ObjectNode requestBody = mapper.createObjectNode();
        ArrayNode contents = mapper.createArrayNode();
        ObjectNode content = mapper.createObjectNode();
        ArrayNode parts = mapper.createArrayNode();
        ObjectNode textPart = mapper.createObjectNode();
        textPart.put("text", prompt);
        parts.add(textPart);
        content.set("parts", parts);
        contents.add(content);
        requestBody.set("contents", contents);

        ObjectNode generationConfig = mapper.createObjectNode();
        generationConfig.put("temperature", TEMPERATURE);
        generationConfig.put("maxOutputTokens", MAX_TOKENS);
        requestBody.set("generationConfig", generationConfig);

        String jsonBody = mapper.writeValueAsString(requestBody);

        // ── 2. Open HTTP connection ───────────────────────────────────────────────
        String apiUrl = String.format(API_URL_TEMPLATE, MODEL, apiKey);
        HttpURLConnection conn = (HttpURLConnection) new URL(apiUrl).openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type",  "application/json");
        conn.setDoOutput(true);
        conn.setConnectTimeout(CONNECT_TIMEOUT);
        conn.setReadTimeout(READ_TIMEOUT);

        // ── 3. Write request body ─────────────────────────────────────────────────
        try (OutputStream os = conn.getOutputStream()) {
            os.write(jsonBody.getBytes(StandardCharsets.UTF_8));
        }

        // ── 4. Read response ──────────────────────────────────────────────────────
        int statusCode = conn.getResponseCode();
        InputStream responseStream = (statusCode >= 200 && statusCode < 300)
                ? conn.getInputStream()
                : conn.getErrorStream();

        StringBuilder rawResponse = new StringBuilder();
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(responseStream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) {
                rawResponse.append(line);
            }
        }

        // ── 5. Handle non-2xx ─────────────────────────────────────────────────────
        if (statusCode < 200 || statusCode >= 300) {
            throw new IOException("[GlmClient] Gemini API returned HTTP " + statusCode + ": " + rawResponse);
        }

        // ── 6. Extract content from Gemini envelope ───────────────────────────────
        // Gemini response shape: { "candidates":[{"content":{"parts":[{"text":"..."}]}}] }
        JsonNode root    = mapper.readTree(rawResponse.toString());
        JsonNode candidates = root.path("candidates");

        if (!candidates.isArray() || candidates.size() == 0) {
            throw new IOException("[GlmClient] No candidates returned in Gemini response: " + rawResponse);
        }

        JsonNode contentText = candidates.get(0).path("content").path("parts").path(0).path("text");

        if (contentText.isMissingNode() || contentText.isNull()) {
            throw new IOException("[GlmClient] 'candidates[0].content.parts[0].text' missing in Gemini response: " + rawResponse);
        }

        log("INFO", "Gemini call successful (HTTP " + statusCode + ").");
        String text = contentText.asText().trim();

        if (text.startsWith("```")) {
            text = text.replaceAll("```json", "")
                    .replaceAll("```", "")
                    .trim();
        }

        return text;
    }

    private void log(String level, String msg) {
        System.out.println("[" + level + "][GlmClient] " + msg);
    }
}
