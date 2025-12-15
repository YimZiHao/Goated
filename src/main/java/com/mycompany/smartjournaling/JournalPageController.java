package com.mycompany.smartjournaling;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.time.LocalDate;
import java.util.ResourceBundle;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;

public class JournalPageController implements Initializable {

    // --- FXML IDs ---
    @FXML private ListView<String> dateList;
    @FXML private Label dateLabel; // Matches your FXML fx:id="dateLabel"
    @FXML private Label weatherLabel;
    @FXML private Label moodLabel;
    @FXML private TextArea journaltextArea; // Matches your FXML fx:id="journaltextArea"
    @FXML private Button saveButton;
    @FXML private Label statusLabel;

    // --- Constants ---
    private final String TODAY_LABEL = LocalDate.now().toString() + " (Today)";
    private final String WEATHER_API_URL = "https://api.data.gov.my/weather/forecast?contains=Kuala%20Lumpur";
    private final String MOOD_API_URL = "https://router.huggingface.co/hf-inference/models/distilbert/distilbert-base-uncased-finetuned-sst-2-english";
    private final String DATA_FILE = "journal_data.txt";

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        loadDates();

        // Listener for date selection
        dateList.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) handleDateSelection(newVal);
        });
        
        journaltextArea.setStyle("-fx-control-inner-background: rgba(255, 255, 255, 0.7); " +
                             "-fx-font-family: 'Book Antiqua'; " +
                             "-fx-highlight-fill: #00BFFF; " + 
                             "-fx-highlight-text-fill: white;");

        // Select "Today" by default when page loads
        dateList.getSelectionModel().select(TODAY_LABEL);

        // SAVE BUTTON ACTION
        saveButton.setOnAction(event -> {
            System.out.println("BUTTON CLICKED!");
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
        
        // Add "Today" at the top
        dateList.getItems().add(TODAY_LABEL);

        // Show past 3 days
        for (int i = 1; i <= 3; i++) {
            dateList.getItems().add(today.minusDays(i).toString());
        }
    }

    private void handleDateSelection(String selectedDateLabel) {
        dateLabel.setText(selectedDateLabel);
        statusLabel.setText(""); 

        // 1. Clean the date string for file searching
        String cleanDate = selectedDateLabel.replace(" (Today)", "").trim();

        // 2. Try to load existing data
        String[] savedData = loadFromFile(cleanDate);

        if (savedData != null) {
            // --- ENTRY EXISTS ---
            // Format in file: Date;Weather;Mood;Text
            weatherLabel.setText("Weather: " + savedData[1]);
            moodLabel.setText("Mood: " + savedData[2]);
            journaltextArea.setText(savedData[3]);
        } else {
            // --- NO ENTRY YET ---
            journaltextArea.setText("");
            
            if (selectedDateLabel.equals(TODAY_LABEL)) {
                // If it's today, fetch live weather immediately
                fetchWeatherOnly();
                moodLabel.setText("Mood: (Pending Analysis)");
            } else {
                weatherLabel.setText("Weather: N/A");
                moodLabel.setText("Mood: N/A");
                journaltextArea.setText("No entry for this date.");
            }
        }

        // 3. Set Editable State (Only allow editing Today)
        boolean isToday = selectedDateLabel.equals(TODAY_LABEL);
        journaltextArea.setEditable(isToday);
        saveButton.setVisible(isToday);
    }

    // --- API LOGIC ---
    
    // Helper to just get weather when page opens
    private void fetchWeatherOnly() {
        new Thread(() -> {
            try {
                String response = API.get(WEATHER_API_URL);
                String weather = extractValue(response, "summary_forecast");
                
                // Update UI on JavaFX thread
                Platform.runLater(() -> weatherLabel.setText("Weather: " + weather));
            } catch (Exception e) {
                Platform.runLater(() -> weatherLabel.setText("Weather: Unavailable"));
            }
        }).start();
    }

    private void saveEntryWithAI() {
        String journalText = journaltextArea.getText();
        if (journalText.trim().isEmpty()) {
            statusLabel.setText("Please write something first!");
            return;
        }

        statusLabel.setText("Analyzing Mood...");
        saveButton.setDisable(true); // Prevent double clicking

        // Run in background thread to avoid freezing UI
        new Thread(() -> {
            // 1. GET Weather (Refresh it just in case)
            String weather = "Unknown";
            try {
                String response = API.get(WEATHER_API_URL);
                weather = extractValue(response, "summary_forecast");
            } catch (Exception e) {
                weather = "Unavailable";
            }

            // 2. POST Mood Analysis
            String mood = "Neutral";
            try {
                String jsonBody = "{\"inputs\": \"" + journalText + "\"}";
                String response = API.post(MOOD_API_URL, jsonBody);
                mood = extractValue(response, "label");
            } catch (Exception e) {
                mood = "Unknown";
            }

            // 3. Update UI & Save
            String finalWeather = weather;
            String finalMood = mood;
            
            Platform.runLater(() -> {
                weatherLabel.setText("Weather: " + finalWeather);
                moodLabel.setText("Mood: " + finalMood);
                statusLabel.setText("Saved!");
                saveButton.setDisable(false);

                // Save to text file
                String todayDate = LocalDate.now().toString();
                saveToFile(todayDate, finalWeather, finalMood, journalText);
            });
            
        }).start();
    }

    // --- FILE I/O HELPERS ---
    private void saveToFile(String date, String weather, String mood, String text) {
        // Replace newlines with special code so it fits on one line in text file
        String sanitizedText = text.replace("\n", "||"); 
        String record = date + ";" + weather + ";" + mood + ";" + sanitizedText;

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(DATA_FILE, true))) {
            writer.write(record);
            writer.newLine();
        } catch (IOException e) {
            System.out.println("Save Error: " + e.getMessage());
        }
    }

    private String[] loadFromFile(String dateToFind) {
        try (BufferedReader reader = new BufferedReader(new FileReader(DATA_FILE))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(";", 4);
                // Check if the date matches
                if (parts.length >= 4 && parts[0].equals(dateToFind)) {
                    // Restore newlines
                    parts[3] = parts[3].replace("||", "\n");
                    return parts;
                }
            }
        } catch (IOException e) {
            // File doesn't exist yet, ignore
        }
        return null;
    }

    // --- UTILITY ---
    private String extractValue(String jsonResponse, String key) {
        // Simple JSON parser for basic responses
        String searchKey = "\"" + key + "\":";
        int startIndex = jsonResponse.indexOf(searchKey);
        if (startIndex != -1) {
            startIndex += searchKey.length();
            
            // Skip quotes if present
            if (jsonResponse.charAt(startIndex) == '"') startIndex++;
            
            int endIndex = jsonResponse.indexOf("\"", startIndex);
            if (endIndex == -1) endIndex = jsonResponse.indexOf(",", startIndex);
            if (endIndex == -1) endIndex = jsonResponse.indexOf("}", startIndex);
            
            return jsonResponse.substring(startIndex, endIndex).trim();
        }
        return "N/A";
    }
}