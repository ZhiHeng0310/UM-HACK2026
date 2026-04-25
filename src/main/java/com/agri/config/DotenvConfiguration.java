package com.agri.config;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Loads environment variables from .env file into System properties
 * This allows Spring @Value annotations to access them
 */
@Configuration
public class DotenvConfiguration {

    @Autowired(required = false)
    private Environment environment;

    @PostConstruct
    public void loadDotenv() {
        try {
            Dotenv dotenv = Dotenv.configure()
                    .directory(System.getProperty("user.dir"))
                    .ignoreIfMissing()
                    .load();

            // Load all .env variables into System properties
            dotenv.entries().forEach(entry -> {
                String key = entry.getKey();
                String value = entry.getValue();
                if (!key.isEmpty() && !value.isEmpty()) {
                    System.setProperty(key, value);
                }
            });

            System.out.println("[INFO] Loaded .env file into System properties");
            String apiKey = System.getProperty("GEMINI_API_KEY");
            if (apiKey != null && !apiKey.isEmpty()) {
                System.out.println("[INFO] GEMINI_API_KEY loaded: " + apiKey.substring(0, Math.min(10, apiKey.length())) + "...");
            }

        } catch (Exception e) {
            System.err.println("[WARN] Failed to load .env file: " + e.getMessage());
        }
    }
}
