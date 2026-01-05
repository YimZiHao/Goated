package com.mycompany.smartjournaling;

import java.io.*;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
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
    @FXML private Label displayName; 
    @FXML private TextArea journaltextArea; 
    @FXML private Button saveButton;
    @FXML private Button deleteButton; 
    @FXML private Label statusLabel;

    // --- Constants ---
    private final String TODAY_LABEL = LocalDate.now().toString() + " (Today)";
    private final String WEATHER_API_URL = "https://api.data.gov.my/weather/forecast?contains=Lumpur";
    private final String MOOD_API_URL = "https://router.huggingface.co/hf-inference/models/distilbert/distilbert-base-uncased-finetuned-sst-2-english";
    private final String ROOT_DIR = "JournalEntries";

    private String currentUser = UserSession.getCurrentUser();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        if (currentUser == null) currentUser = "test_user"; 
        
        String realName = getUserNameFromDB(currentUser);
        displayName.setText("Welcome, " + realName);

        // 1. LOAD DATES FROM DB (Instead of dummy dates)
        loadDatesFromDB();

        // Styling
        journaltextArea.setStyle("-fx-control-inner-background: rgba(255, 255, 255, 0.7); " +
                                 "-fx-font-family: 'Book Antiqua'; " +
                                 "-fx-highlight-fill: #00BFFF; " + 
                                 "-fx-highlight-text-fill: white;");

        dateList.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) handleDateSelection(newVal);
        });

        dateList.getSelectionModel().select(TODAY_LABEL);

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

        deleteButton.setOnAction(event -> deleteCurrentEntry());
    }

    // --- HELPER: DATABASE DATES ---
    // This method reads the comma-separated string from DB and fills the ListView
    private void loadDatesFromDB() {
        dateList.getItems().clear();
        dateList.getItems().add(TODAY_LABEL); // Always show Today first

        String query = "SELECT date_list FROM user_dates WHERE email = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            
            stmt.setString(1, currentUser);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                String dates = rs.getString("date_list");
                if (dates != null && !dates.isEmpty()) {
                    String[] dateArray = dates.split(",");
                    for (String d : dateArray) {
                        // Don't duplicate "Today" if it's already in the list
                        if (!d.equals(LocalDate.now().toString())) {
                            dateList.getItems().add(d);
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // This method adds or removes a date from the DB string
    private void updateDateInDB(String dateToUpdate, boolean isAdding) {
        // 1. Get current list
        List<String> dates = new ArrayList<>();
        String querySelect = "SELECT date_list FROM user_dates WHERE email = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(querySelect)) {
            stmt.setString(1, currentUser);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                String raw = rs.getString("date_list");
                if (raw != null && !raw.isEmpty()) {
                    dates.addAll(Arrays.asList(raw.split(",")));
                }
            }
        } catch (Exception e) { e.printStackTrace(); }

        // 2. Modify list
        if (isAdding) {
            if (!dates.contains(dateToUpdate)) dates.add(dateToUpdate);
        } else {
            dates.remove(dateToUpdate);
        }

        // 3. Save back to DB
        String newString = String.join(",", dates);
        // Use "INSERT ... ON DUPLICATE KEY UPDATE" to handle both cases
        String queryUpsert = "INSERT INTO user_dates (email, date_list) VALUES (?, ?) " +
                             "ON DUPLICATE KEY UPDATE date_list = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(queryUpsert)) {
            stmt.setString(1, currentUser);
            stmt.setString(2, newString);
            stmt.setString(3, newString);
            stmt.executeUpdate();
        } catch (Exception e) { e.printStackTrace(); }
    }

    // --- HELPER: GET NAME ---
    private String getUserNameFromDB(String email) {
        String name = "User";
        String query = "SELECT `Display Name` FROM user WHERE `Email Address` = ?"; 
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, email);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) name = rs.getString("Display Name");
        } catch (Exception e) { if (email.contains("@")) name = email.split("@")[0]; }
        return name;
    }

    private String cleanDate(String label) {
        return label.replace(" (Today)", "").trim();
    }

    private void handleDateSelection(String selectedDateLabel) {
        dateLabel.setText(selectedDateLabel);
        statusLabel.setText(""); 
        String dateOnly = cleanDate(selectedDateLabel);

        String[] savedData = loadFromFolder(dateOnly);

        if (savedData != null) {
            weatherLabel.setText("Weather: " + savedData[0]); 
            moodLabel.setText("Mood: " + savedData[1]);
            journaltextArea.setText(savedData[2]);
            saveButton.setText("Update Entry"); 
            deleteButton.setVisible(true); 
        } else {
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

    // --- FOLDER I/O ---
    private void saveToFolder(String targetDate, String weather, String mood, String text) {
        File userFolder = new File(ROOT_DIR + File.separator + currentUser);
        if (!userFolder.exists()) userFolder.mkdirs();

        File entryFile = new File(userFolder, targetDate + ".txt");

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(entryFile))) {
            writer.write(weather);
            writer.newLine();
            writer.write(mood);
            writer.newLine();
            writer.write(text);
            
            // NEW: Update DB whenever we save!
            updateDateInDB(targetDate, true); 
            
            // Refresh list to show new date immediately if needed
            if (!dateList.getItems().contains(targetDate) && !targetDate.equals(LocalDate.now().toString())) {
                dateList.getItems().add(targetDate);
            }

        } catch (IOException e) {
            statusLabel.setText("Save Failed!");
            e.printStackTrace();
        }
        
        Platform.runLater(() -> {
             saveButton.setText("Update Entry");
             deleteButton.setVisible(true);
        });
    }

    private String[] loadFromFolder(String dateToFind) {
        File entryFile = new File(ROOT_DIR + File.separator + currentUser, dateToFind + ".txt");
        if (entryFile.exists()) {
            try (BufferedReader reader = new BufferedReader(new FileReader(entryFile))) {
                String weather = reader.readLine();
                String mood = reader.readLine();
                String text = reader.lines().collect(Collectors.joining("\n"));
                return new String[]{weather, mood, text};
            } catch (IOException e) { e.printStackTrace(); }
        }
        return null; 
    }

    private void deleteCurrentEntry() {
        String targetDate = cleanDate(dateLabel.getText());
        File entryFile = new File(ROOT_DIR + File.separator + currentUser, targetDate + ".txt");

        if (entryFile.exists()) {
            entryFile.delete();
            
            // NEW: Remove from DB
            updateDateInDB(targetDate, false);
            
            // Remove from UI List
            dateList.getItems().remove(targetDate);
        }
        
        journaltextArea.setText("");
        weatherLabel.setText("Weather: N/A");
        moodLabel.setText("Mood: N/A");
        statusLabel.setText("Entry Deleted.");
        saveButton.setText("Save Entry"); 
        deleteButton.setVisible(false);   
        
        if (dateLabel.getText().equals(TODAY_LABEL)) fetchWeatherOnly();
    }

    // --- API LOGIC ---
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
                saveToFolder(LocalDate.now().toString(), fWeather, fMood, journalText);
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