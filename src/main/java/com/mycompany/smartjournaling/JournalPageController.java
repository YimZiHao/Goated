package com.mycompany.smartjournaling;

import java.io.*;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.util.ResourceBundle;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
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
    @FXML private Label displayName; // Make sure your FXML label has fx:id="displayName"
    @FXML private TextArea journaltextArea; 
    @FXML private Button saveButton;
    @FXML private Button deleteButton; 
    @FXML private Label statusLabel;

    // --- Constants ---
    private final String TODAY_LABEL = LocalDate.now().toString() + " (Today)";
    private final String WEATHER_API_URL = "https://api.data.gov.my/weather/forecast?contains=Lumpur";
    private final String MOOD_API_URL = "https://router.huggingface.co/hf-inference/models/distilbert/distilbert-base-uncased-finetuned-sst-2-english";
    
    // NEW: We use a folder instead of one big file
    private final String ROOT_DIR = "JournalEntries";

    // Get the logged-in user's email
    private String currentUser = UserSession.getCurrentUser();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        if (currentUser == null) currentUser = "test_user"; 
        
        // 1. FETCH REAL NAME from DB for the Welcome Message
        String realName = getUserNameFromDB(currentUser);
        displayName.setText("Welcome, " + realName);

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
                saveToFolder(cleanDate, weather, mood, journaltextArea.getText());
                statusLabel.setText("Entry Updated.");
            }
        });

        // DELETE BUTTON
        deleteButton.setOnAction(event -> deleteCurrentEntry());
    }

    // --- HELPER: Get Name from DB ---
    // --- HELPER: Get Name from DB ---
    private String getUserNameFromDB(String email) {
        String name = "User";
        
        // UPDATED QUERY: Table is 'user', Column is 'Display Name', User ID is 'Email Address'
        String query = "SELECT `Display Name` FROM user WHERE `Email Address` = ?"; 

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            
            stmt.setString(1, email);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                name = rs.getString("Display Name"); // Match the column name exactly
            }
        } catch (Exception e) {
            e.printStackTrace();
            if (email.contains("@")) name = email.split("@")[0];
        }
        return name;
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

        // LOAD FROM FOLDER
        String[] savedData = loadFromFolder(dateOnly);

        if (savedData != null) {
            // Found File -> Edit Mode
            weatherLabel.setText("Weather: " + savedData[0]); 
            moodLabel.setText("Mood: " + savedData[1]);
            journaltextArea.setText(savedData[2]);
            
            saveButton.setText("Update Entry"); 
            deleteButton.setVisible(true); 
        } else {
            // No File -> Create Mode
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

    // --- FOLDER SYSTEM: SAVE ---
    private void saveToFolder(String targetDate, String weather, String mood, String text) {
        // Path: JournalEntries / user@email.com /
        File userFolder = new File(ROOT_DIR + File.separator + currentUser);
        if (!userFolder.exists()) {
            userFolder.mkdirs(); 
        }

        // File: 2025-12-30.txt
        File entryFile = new File(userFolder, targetDate + ".txt");

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(entryFile))) {
            writer.write(weather);
            writer.newLine();
            writer.write(mood);
            writer.newLine();
            writer.write(text); 
        } catch (IOException e) {
            statusLabel.setText("Save Failed!");
            e.printStackTrace();
        }
        
        Platform.runLater(() -> {
             saveButton.setText("Update Entry");
             deleteButton.setVisible(true);
        });
    }

    // --- FOLDER SYSTEM: LOAD ---
    private String[] loadFromFolder(String dateToFind) {
        File entryFile = new File(ROOT_DIR + File.separator + currentUser, dateToFind + ".txt");

        if (entryFile.exists()) {
            try (BufferedReader reader = new BufferedReader(new FileReader(entryFile))) {
                String weather = reader.readLine();
                String mood = reader.readLine();
                String text = reader.lines().collect(Collectors.joining("\n"));
                return new String[]{weather, mood, text};
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null; 
    }

    // --- FOLDER SYSTEM: DELETE ---
    private void deleteCurrentEntry() {
        String targetDate = cleanDate(dateLabel.getText());
        File entryFile = new File(ROOT_DIR + File.separator + currentUser, targetDate + ".txt");

        if (entryFile.exists()) {
            entryFile.delete(); 
        }
        
        // Reset UI
        journaltextArea.setText("");
        weatherLabel.setText("Weather: N/A");
        moodLabel.setText("Mood: N/A");
        statusLabel.setText("Entry Deleted.");
        saveButton.setText("Save Entry"); 
        deleteButton.setVisible(false);   
        
        if (dateLabel.getText().equals(TODAY_LABEL)) fetchWeatherOnly();
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
                saveToFolder(todayDate, fWeather, fMood, journalText);
            });
        }).start();
    }

    private String extractValue(String jsonResponse, String key) {
        Pattern pattern = Pattern.compile("\"" + key + "\":\\s*\"?([^,\"}]+)\"?");
        Matcher matcher = pattern.matcher(jsonResponse);
        if (matcher.find()) return matcher.group(1);
        return "N/A";
    }
}