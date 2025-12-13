package com.mycompany.smartjournaling;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.time.LocalDate;
import java.util.ResourceBundle;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;

public class JournalPageController implements Initializable {

    // --- FXML IDs ---
    @FXML private ListView<String> dateList;
    @FXML private Label dateHeader;
    @FXML private Label weatherLabel;
    @FXML private Label moodLabel;
    @FXML private TextArea journalTextArea;
    @FXML private Button saveButton;
    @FXML private Label statusLabel;

    // --- Constants ---
    private final String TODAY_LABEL = LocalDate.now().toString() + " (Today)";
    private final String WEATHER_API_URL = "https://api.data.gov.my/weather/forecast?contains=St009"; 
    private final String MOOD_API_URL = "https://api-inference.huggingface.co/models/distilbert-base-uncased-finetuned-sst-2-english";
    private final String DATA_FILE = "journal_data.txt";

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        loadDates();

        // Listener for date selection
        dateList.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) handleDateSelection(newVal);
        });

        // SAVE BUTTON ACTION
        saveButton.setOnAction(event -> {
            try {
                saveEntryWithAI();
            } catch (Exception e) {
                statusLabel.setText("Error: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    private void loadDates() {
        dateList.getItems().clear();
        LocalDate today = LocalDate.now();
        // Showing past 3 days + Today
        for (int i = 3; i > 0; i--) {
            dateList.getItems().add(today.minusDays(i).toString());
        }
        dateList.getItems().add(TODAY_LABEL);
    }

    private void handleDateSelection(String dateLabel) {
        dateHeader.setText(dateLabel);
        statusLabel.setText(""); 

        // 1. Extract clean date string (e.g. "2025-10-11")
        String cleanDate = dateLabel.replace(" (Today)", "").trim();

        // 2. Try to load existing data for this date
        String[] savedData = loadFromFile(cleanDate);

        if (savedData != null) {
            // --- DATA FOUND ---
            String savedWeather = savedData[1];
            String savedMood = savedData[2];
            String savedText = savedData[3];

            weatherLabel.setText("Weather: " + savedWeather);
            moodLabel.setText("Mood: " + savedMood);
            journalTextArea.setText(savedText);
        } else {
            // --- NO DATA FOUND ---
            weatherLabel.setText("Weather: --");
            moodLabel.setText("Mood: --");
            journalTextArea.setText("");
        }

        // 3. Set Editable State
        if (dateLabel.equals(TODAY_LABEL)) {
            // Allow editing ONLY if it's today
            journalTextArea.setEditable(true);
            saveButton.setVisible(true);
            
            // Special case: If we haven't saved today yet, show instructions
            if (savedData == null) {
                weatherLabel.setText("Weather: (Click Save to Fetch)");
                moodLabel.setText("Mood: (Click Save to Analyze)");
            }
        } else {
            // Past dates are read-only
            journalTextArea.setEditable(false);
            saveButton.setVisible(false);
            if (savedData == null) {
                journalTextArea.setText("No entry found for this past date.");
            }
        }
    }

    // --- MAIN LOGIC ---
    private void saveEntryWithAI() {
        String journalText = journalTextArea.getText();
        if (journalText.trim().isEmpty()) {
            statusLabel.setText("Please write something first!");
            return;
        }

        statusLabel.setText("Connecting to AI...");

        // 1. GET Weather
        String weather = "Unknown";
        try {
            String response = API.get(WEATHER_API_URL);
            weather = extractValue(response, "summary_forecast");
        } catch (Exception e) {
            System.out.println("Weather API Failed: " + e.getMessage());
        }

        // 2. POST Mood Analysis
        String mood = "Unknown";
        try {
            String jsonBody = "{\"inputs\": \"" + journalText + "\"}";
            String response = API.post(MOOD_API_URL, jsonBody);
            mood = extractValue(response, "label");
        } catch (Exception e) {
            System.out.println("Mood API Failed: " + e.getMessage());
        }

        // 3. Update UI
        weatherLabel.setText("Weather: " + weather);
        moodLabel.setText("Mood: " + mood);
        statusLabel.setText("Entry Saved & Analyzed!");

        // 4. Save to File
        String todayDate = LocalDate.now().toString();
        saveToFile(todayDate, weather, mood, journalText);
    }

    // --- FILE I/O HELPERS ---
    private void saveToFile(String date, String weather, String mood, String text) {
        // Format: Date;Weather;Mood;Text
        // We replace newlines with "||" so the entry stays on one line in the text file
        String sanitizedText = text.replace("\n", "||"); 
        String record = date + ";" + weather + ";" + mood + ";" + sanitizedText;

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(DATA_FILE, true))) {
            writer.write(record);
            writer.newLine();
            System.out.println("Saved: " + record);
        } catch (IOException e) {
            System.out.println("Save Error: " + e.getMessage());
        }
    }

    private String[] loadFromFile(String dateToFind) {
        try (BufferedReader reader = new BufferedReader(new FileReader(DATA_FILE))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(";", 4);
                if (parts.length == 4 && parts[0].equals(dateToFind)) {
                    // Restore newlines
                    parts[3] = parts[3].replace("||", "\n");
                    return parts;
                }
            }
        } catch (IOException e) {
            // File might not exist yet, which is fine
        }
        return null;
    }

    // --- UTILITY ---
    private String extractValue(String jsonResponse, String key) {
        String searchKey = "\"" + key + "\":";
        int startIndex = jsonResponse.indexOf(searchKey);
        if (startIndex != -1) {
            startIndex += searchKey.length();
            char firstChar = jsonResponse.charAt(startIndex);
            if (firstChar == '"') {
                startIndex++; 
                int endIndex = jsonResponse.indexOf("\"", startIndex);
                return jsonResponse.substring(startIndex, endIndex);
            } else {
                int endIndex = jsonResponse.indexOf(",", startIndex);
                if (endIndex == -1) endIndex = jsonResponse.indexOf("}", startIndex);
                return jsonResponse.substring(startIndex, endIndex).trim();
            }
        }
        return "N/A";
    }
}