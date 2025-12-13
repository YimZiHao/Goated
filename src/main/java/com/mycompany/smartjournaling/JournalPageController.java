package com.mycompany.smartjournaling;

import java.net.URL;
import java.time.LocalDate;
import java.util.ResourceBundle;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;


public class JournalPageController implements Initializable {

    @FXML private ListView<String> dateList;
    @FXML private Label dateHeader;
    @FXML private Label weatherLabel;
    @FXML private Label moodLabel;
    @FXML private TextArea journaltextArea;
    @FXML private Button saveButton;
    @FXML private Label statusLabel;
    private final String TODAY_LABEL = LocalDate.now().toString() + " (Today)";
    private final String WEATHER_API_URL = "https://api.data.gov.my/weather/forecast?contains=St009";
    private final String MOOD_API_URL = "https://api-inference.huggingface.co/models/distilbert-base-uncased-finetuned-sst-2-english";

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        loadDates();
        
        dateList.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) handleDateSelection(newVal);
        });
        saveButton.setOnAction(event -> {
            try {
                saveEntryWithAI();
            } catch (Exception e){
                statusLabel.setText("Error: " +e.getMessage());
                e.printStackTrace();
            }
        });
        
    }
    
    private void loadDates(){
        dateList.getItems().clear();
        LocalDate today = LocalDate.now();
        
        for(int i=3; i>0; i--){
            dateList.getItems().add(today.minusDays(i).toString());
        }
        dateList.getItems().add(TODAY_LABEL);
    }
    private void handleDateSelection(String dateLabel) {
        dateHeader.setText(dateLabel);
        statusLabel.setText("");

        // Extract just the date part (remove " (Today)" if present)
        String cleanDate = dateLabel.replace(" (Today)", "").trim();
        
        // Try to load data from file
        String[] savedData = loadFromFile(cleanDate);
        
        if (savedData != null) {
            // ENTRY FOUND: Show the saved data
            String savedWeather = savedData[1];
            String savedMood = savedData[2];
            String savedText = savedData[3];
            
            weatherLabel.setText("Weather: " + savedWeather);
            moodLabel.setText("Mood: " + savedMood);
            journaltextArea.setText(savedText);
            
            // If it's a past date, lock it. If it's today, allow editing? 
            // (Assignment says past dates are view-only [cite: 71])
            boolean isToday = dateLabel.contains("Today");
            journaltextArea.setEditable(isToday); 
            saveButton.setVisible(isToday);
            
        } else {
            // NO ENTRY FOUND
            weatherLabel.setText("Weather: --");
            moodLabel.setText("Mood: --");
            journaltextArea.setText("");
            
            if (dateLabel.contains("Today")) {
                journaltextArea.setEditable(true);
                saveButton.setVisible(true);
                journaltextArea.setPromptText("Write your thoughts for today...");
            } else {
                journaltextArea.setEditable(false);
                saveButton.setVisible(false);
                journaltextArea.setText("No entry found for this date.");
            }
        }
    }

    // --- THE CORE LOGIC ---
    private void saveEntryWithAI() {
        String journalText = journaltextArea.getText();
        if (journalText.trim().isEmpty()) {
            statusLabel.setText("Please write something first!");
            return;
        }

        statusLabel.setText("Connecting to AI...");

        // 1. GET Weather
        String weather = "Unknown";
        try {
            // Call the provided API class
            String response = API.get(WEATHER_API_URL);
            // Extract specific field "summary_forecast"
            weather = extractValue(response, "summary_forecast");
        } catch (Exception e) {
            System.out.println("Weather API Failed: " + e.getMessage());
        }

        // 2. POST Mood Analysis
        String mood = "Unknown";
        try {
            // Create JSON Body: {"inputs": "User text here"}
            String jsonBody = "{\"inputs\": \"" + journalText + "\"}";
            String response = API.post(MOOD_API_URL, jsonBody);
            // Extract specific field "label" (POSITIVE / NEGATIVE)
            mood = extractValue(response, "label");
        } catch (Exception e) {
            System.out.println("Mood API Failed: " + e.getMessage());
        }

        // 3. Update UI
        weatherLabel.setText("Weather: " + weather);
        moodLabel.setText("Mood: " + mood);
        statusLabel.setText("Entry Saved & Analyzed!");
        
        // TODO: Save 'journalText', 'weather', and 'mood' to a .txt or .csv file here
        // Get the current date in YYYY-MM-DD format
        String todayDate = LocalDate.now().toString();
        
        // Save to text file
        saveToFile(todayDate, weather, mood, journalText);
    }

    // Helper method to extract values from JSON string manually (Simple parsing)
    private String extractValue(String jsonResponse, String key) {
        // Looks for "key":"value" pattern
        String searchKey = "\"" + key + "\":";
        int startIndex = jsonResponse.indexOf(searchKey);
        
        if (startIndex != -1) {
            startIndex += searchKey.length();
            
            // Handle if value is in quotes (String) or not (Number)
            char firstChar = jsonResponse.charAt(startIndex);
            if (firstChar == '"') {
                startIndex++; // Skip starting quote
                int endIndex = jsonResponse.indexOf("\"", startIndex);
                return jsonResponse.substring(startIndex, endIndex);
            } else {
                // It might be a number or boolean, read until comma or bracket
                int endIndex = jsonResponse.indexOf(",", startIndex);
                if (endIndex == -1) endIndex = jsonResponse.indexOf("}", startIndex);
                return jsonResponse.substring(startIndex, endIndex).trim();
            }
        }
        return "N/A";
        
        
    }
    
    // --- FILE I/O HELPERS ---

    // 1. SAVING DATA
    private void saveToFile(String date, String weather, String mood, String text) {
        // We use a specific format: Date;Weather;Mood;Text
        // We replace newlines in the text with a special placeholder "||" so it fits on one line
        String sanitizedText = text.replace("\n", "||");
        String record = date + ";" + weather + ";" + mood + ";" + sanitizedText;

        try (BufferedWriter writer = new BufferedWriter(new FileWriter("journal_data.txt", true))) {
            writer.write(record);
            writer.newLine(); // Move to next line for next entry
            System.out.println("Saved to file: " + record);
        } catch (IOException e) {
            System.out.println("Error saving file: " + e.getMessage());
        }
    }

    // 2. LOADING DATA
    private String[] loadFromFile(String dateToFind) {
        // Returns an array [Weather, Mood, Text] if found, or null if not found
        try (BufferedReader reader = new BufferedReader(new FileReader("journal_data.txt"))) {
            String line;
            while ((line = reader.readLine()) != null) {
                // Split the line by ";"
                String[] parts = line.split(";", 4); 
                // parts[0] = Date, parts[1] = Weather, parts[2] = Mood, parts[3] = Text
                
                if (parts.length == 4 && parts[0].equals(dateToFind)) {
                    // Found the date!
                    // Restore newlines (replace "||" back to "\n")
                    parts[3] = parts[3].replace("||", "\n");
                    return parts; 
                }
            }
        } catch (IOException e) {
            // File might not exist yet, which is fine
        }
        return null; // Entry not found
    }
}
