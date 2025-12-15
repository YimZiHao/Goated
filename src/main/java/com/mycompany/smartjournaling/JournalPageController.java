package com.mycompany.smartjournaling;

import java.io.*;
import java.net.URL;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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
    @FXML private Label dateLabel; 
    @FXML private Label weatherLabel;
    @FXML private Label moodLabel;
    @FXML private TextArea journaltextArea; 
    @FXML private Button saveButton;
    @FXML private Button deleteButton; 
    @FXML private Label statusLabel;

    // --- Constants ---
    private final String TODAY_LABEL = LocalDate.now().toString() + " (Today)";
    private final String WEATHER_API_URL = "https://api.data.gov.my/weather/forecast?contains=Lumpur";
    private final String MOOD_API_URL = "https://router.huggingface.co/hf-inference/models/distilbert/distilbert-base-uncased-finetuned-sst-2-english";
    private final String DATA_FILE = "journal_data.txt";
    
    // NEW: Get the logged-in user's email
    private String currentUser = UserSession.getCurrentUser();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // Fallback for testing if you ran the page directly without login
        if (currentUser == null) currentUser = "test_user"; 

        loadDates();
        
        // Styling
        journaltextArea.setStyle("-fx-control-inner-background: rgba(255, 255, 255, 0.7); " +
                                 "-fx-font-family: 'Book Antiqua'; " +
                                 "-fx-highlight-fill: #00BFFF; " + 
                                 "-fx-highlight-text-fill: white;");

        dateList.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) handleDateSelection(newVal);
        });

        dateList.getSelectionModel().select(TODAY_LABEL);

        // SAVE BUTTON
        saveButton.setOnAction(event -> {
            if (dateLabel.getText().equals(TODAY_LABEL)) {
                saveEntryWithAI();
            } else {
                String cleanDate = cleanDate(dateLabel.getText());
                String weather = weatherLabel.getText().replace("Weather: ", "");
                String mood = moodLabel.getText().replace("Mood: ", "");
                saveOrUpdateFile(cleanDate, weather, mood, journaltextArea.getText());
                statusLabel.setText("Entry Updated.");
            }
        });

        // DELETE BUTTON
        deleteButton.setOnAction(event -> deleteCurrentEntry());
    }

    private void loadDates() {
        dateList.getItems().clear();
        LocalDate today = LocalDate.now();
        dateList.getItems().add(TODAY_LABEL);
        for (int i = 1; i <= 3; i++) {
            dateList.getItems().add(today.minusDays(i).toString());
        }
    }

    private String cleanDate(String label) {
        return label.replace(" (Today)", "").trim();
    }

    private void handleDateSelection(String selectedDateLabel) {
        dateLabel.setText(selectedDateLabel);
        statusLabel.setText(""); 
        String dateOnly = cleanDate(selectedDateLabel);

        // LOAD: Only look for data belonging to 'currentUser'
        String[] savedData = loadFromFile(dateOnly);

        if (savedData != null) {
            // Found MY entry -> Edit Mode
            weatherLabel.setText("Weather: " + savedData[2]); 
            moodLabel.setText("Mood: " + savedData[3]);
            journaltextArea.setText(savedData[4]);
            
            saveButton.setText("Update Entry"); 
            deleteButton.setVisible(true); 
        } else {
            // New Entry -> Create Mode
            journaltextArea.setText("");
            deleteButton.setVisible(false);
            saveButton.setText("Save Entry");

            if (selectedDateLabel.equals(TODAY_LABEL)) {
                fetchWeatherOnly();
                moodLabel.setText("Mood: (Pending Analysis)");
            } else {
                weatherLabel.setText("Weather: N/A");
                moodLabel.setText("Mood: N/A");
                journaltextArea.setText("No entry for this date.");
            }
        }
        journaltextArea.setEditable(true);
        saveButton.setVisible(true);
    }

    // --- UPSERT (User Specific) ---
    private void saveOrUpdateFile(String targetDate, String weather, String mood, String text) {
        String sanitizedText = text.replace("\n", "||"); 
        
        // NEW FORMAT: User;Date;Weather;Mood;Text
        String newRecord = currentUser + ";" + targetDate + ";" + weather + ";" + mood + ";" + sanitizedText;
        
        List<String> allLines = new ArrayList<>();
        boolean found = false;

        try (BufferedReader reader = new BufferedReader(new FileReader(DATA_FILE))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(";", 5);
                
                // Match BOTH User AND Date to find the line to replace
                if (parts.length >= 2 && 
                    parts[0].equals(currentUser) && 
                    parts[1].trim().equals(targetDate.trim())) {
                    
                    allLines.add(newRecord); // Replace my old entry
                    found = true;
                } else {
                    allLines.add(line); // Keep everyone else's entries
                }
            }
        } catch (IOException e) {}

        if (!found) allLines.add(newRecord);

        rewriteFile(allLines);
        
        Platform.runLater(() -> {
             saveButton.setText("Update Entry");
             deleteButton.setVisible(true);
        });
    }

    // --- DELETE (User Specific) ---
    private void deleteCurrentEntry() {
        String targetDate = cleanDate(dateLabel.getText());
        List<String> allLines = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(DATA_FILE))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(";", 5);
                
                // Only delete if it matches MY user AND the selected date
                boolean isMyEntry = parts.length >= 2 && parts[0].equals(currentUser);
                boolean isTargetDate = parts.length >= 2 && parts[1].trim().equals(targetDate.trim());
                
                if (isMyEntry && isTargetDate) {
                    // Skip (Delete) this line
                } else {
                    allLines.add(line); // Keep everything else
                }
            }
        } catch (IOException e) { e.printStackTrace(); }

        rewriteFile(allLines);
        
        // Reset UI
        journaltextArea.setText("");
        weatherLabel.setText("Weather: N/A");
        moodLabel.setText("Mood: N/A");
        statusLabel.setText("Entry Deleted.");
        saveButton.setText("Save Entry"); 
        deleteButton.setVisible(false);   
        
        if (dateLabel.getText().equals(TODAY_LABEL)) fetchWeatherOnly();
    }

    private void rewriteFile(List<String> lines) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(DATA_FILE))) {
            for (String line : lines) {
                writer.write(line);
                writer.newLine();
            }
        } catch (IOException e) {
            statusLabel.setText("File Error!");
        }
    }

    // --- API LOGIC (Unchanged) ---
    private void fetchWeatherOnly() {
        new Thread(() -> {
            try {
                String response = API.get(WEATHER_API_URL);
                String weather = extractValue(response, "summary_forecast"); 
                if (weather.equals("N/A")) weather = extractValue(response, "temperature");
                String finalWeather = weather;
                Platform.runLater(() -> weatherLabel.setText("Weather: " + finalWeather));
            } catch (Exception e) {
                Platform.runLater(() -> weatherLabel.setText("Weather: Unavailable"));
            }
        }).start();
    }

    private void saveEntryWithAI() {
        String journalText = journaltextArea.getText();
        if (journalText.trim().isEmpty()) {
            statusLabel.setText("Please write something!");
            return;
        }

        statusLabel.setText("Analyzing...");
        saveButton.setDisable(true);

        new Thread(() -> {
            String weather = "Unknown";
            try {
                String response = API.get(WEATHER_API_URL);
                weather = extractValue(response, "summary_forecast"); 
            } catch (Exception e) {}

            String mood = "Neutral";
            try {
                String jsonBody = "{\"inputs\": \"" + journalText + "\"}";
                String response = API.post(MOOD_API_URL, jsonBody);
                if (response.contains("error")) mood = "API Error";
                else mood = extractValue(response, "label");
            } catch (Exception e) {}

            String fWeather = weather;
            String fMood = mood;
            
            Platform.runLater(() -> {
                weatherLabel.setText("Weather: " + fWeather);
                moodLabel.setText("Mood: " + fMood);
                statusLabel.setText("Saved!");
                saveButton.setDisable(false);
                
                String todayDate = LocalDate.now().toString();
                saveOrUpdateFile(todayDate, fWeather, fMood, journalText);
            });
        }).start();
    }

    // --- LOAD (User Specific) ---
    private String[] loadFromFile(String dateToFind) {
        try (BufferedReader reader = new BufferedReader(new FileReader(DATA_FILE))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(";", 5); // Now expecting 5 columns
                
                // Match User AND Date
                if (parts.length >= 5 && 
                    parts[0].equals(currentUser) && 
                    parts[1].trim().equals(dateToFind.trim())) {
                    
                    parts[4] = parts[4].replace("||", "\n");
                    return parts;
                }
            }
        } catch (IOException e) {}
        return null;
    }

    private String extractValue(String jsonResponse, String key) {
        Pattern pattern = Pattern.compile("\"" + key + "\":\\s*\"?([^,\"}]+)\"?");
        Matcher matcher = pattern.matcher(jsonResponse);
        if (matcher.find()) return matcher.group(1);
        return "N/A";
    }
}