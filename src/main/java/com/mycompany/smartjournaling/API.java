package com.mycompany.smartjournaling;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;

public class API {

    public String get(String apiURL) throws Exception {
        URL url = new URL(apiURL);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();

        conn.setRequestMethod("GET");
        conn.setRequestProperty("Accept", "application/json");

        if (conn.getResponseCode() != 200) {
            throw new RuntimeException("GET failed. HTTP error code: " + conn.getResponseCode());
        }

        BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        StringBuilder sb = new StringBuilder();
        String output;
        while ((output = br.readLine()) != null) {
            sb.append(output);
        }

        conn.disconnect();
        return sb.toString();
    }

    public String post(String apiURL, String bearerToken, String jsonBody) throws Exception {
        URL url = new URL(apiURL);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();

        conn.setRequestMethod("POST");
        conn.setRequestProperty("Accept", "application/json");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("Authorization", "Bearer " + bearerToken);

        conn.setDoOutput(true);

        try (OutputStream os = conn.getOutputStream()) {
            byte[] input = jsonBody.getBytes("utf-8");
            os.write(input, 0, input.length);
        }

        int responseCode = conn.getResponseCode();
        if (responseCode != 200 && responseCode != 201) {
            throw new RuntimeException("POST failed. HTTP error code: " + responseCode);
        }

        BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), "utf-8"));
        StringBuilder sb = new StringBuilder();
        String responseLine;
        while ((responseLine = br.readLine()) != null) {
            sb.append(responseLine.trim());
        }

        conn.disconnect();
        return sb.toString();
    }

    public static void main(String[] args) {
        API api = new API();

        Map<String, String> env = EnvLoader.loadEnv(".env");

        try {
            String getUrl = "https://api.data.gov.my/weather/forecast/?contains=WP%20Kuala%20Lumpur@location__location_name&sort=date&limit=1";
            String getResponse = api.get(getUrl);
            System.out.println("GET Response:\n" + getResponse);

            String journalInput = "I spent my free time with my friends today. We had a great time at the park and enjoyed the sunny weather.";
            String postUrl = "https://router.huggingface.co/hf-inference/models/distilbert/distilbert-base-uncased-finetuned-sst-2-english";

            String bearerToken = env.get("BEARER_TOKEN");
            if (bearerToken == null || bearerToken.isEmpty()) {
                System.err.println("Error: BEARER_TOKEN is not set in the environment.");
                return;
            }

            String jsonBody = "{\"inputs\": \"" + journalInput + "\"}";

            String postResponse = api.post(postUrl, bearerToken, jsonBody);
            System.out.println("\nSentiment Analysis Response:\n" + postResponse);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
