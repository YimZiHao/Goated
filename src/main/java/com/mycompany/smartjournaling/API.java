package com.mycompany.smartjournaling;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class API {

   public static String get(String urlString) {
    StringBuilder response = new StringBuilder();
    HttpURLConnection conn = null;
    try {
        URL url = new URL(urlString);
        conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Accept", "application/json");
        
        int responseCode = conn.getResponseCode();
        if (responseCode == HttpURLConnection.HTTP_OK) {
            try (BufferedReader in = new BufferedReader(
                    new InputStreamReader(conn.getInputStream()))) {
                String line;
                while ((line = in.readLine()) != null) {
                    response.append(line);
                }
            }
        } else {
            System.out.println("GET Request Failed. Response Code: " + responseCode);
            return "{}";
        }
    } catch (Exception e) {
        System.out.println("Error in GET request: " + e.getMessage());
        return "{}";
    } finally {
        if (conn != null) {
            conn.disconnect();
        }
    }
    return response.toString();
}

    // post method for mood analysis
    public static String post(String urlString, String jsonInputString) {
        StringBuilder response = new StringBuilder();
        Map<String, String> env = EnvLoader.loadEnv(".env");
        
        String token = env.get("BEARER_TOKEN");
        
        if (token == null || token.isEmpty()) {
            System.out.println("Error: BEARER_TOKEN not found in .env file.");
            return "{}";
        }

        try {
            URL url = new URL(urlString);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            
            conn.setRequestProperty("Authorization", "Bearer " + token); 
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Accept", "application/json");
            conn.setDoOutput(true);

            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = jsonInputString.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            int responseCode = conn.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                try (BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                    String line;
                    while ((line = in.readLine()) != null) {
                        response.append(line);
                    }
                }
            } else {
                System.out.println("POST Request Failed. Response Code: " + responseCode);
                try (BufferedReader errorReader = new BufferedReader(new InputStreamReader(conn.getErrorStream()))) {
                    String line;
                    while ((line = errorReader.readLine()) != null) {
                        System.out.println("Error Details: " + line);
                    }
                }
                return "{}";
            }
        } catch (Exception e) {
            System.out.println("Error in POST request: " + e.getMessage());
            return "{}";
        }
        return response.toString();
    }
}