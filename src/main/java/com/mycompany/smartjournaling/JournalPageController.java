package com.mycompany.smartjournaling;

import java.io.*;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
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
import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class JournalPageController implements Initializable {

    @FXML
    private ListView<String> dateList;
    @FXML
    private Label dateLabel;
    @FXML
    public Label weatherLabel;
    @FXML
    private Label moodLabel;
    @FXML
    private Label displayName;
    @FXML
    private TextArea journaltextArea;
    @FXML
    private Button saveButton;
    @FXML
    private Button deleteButton;
    @FXML
    private Label statusLabel;
    @FXML
    private Button weeklySummaryButton;
    @FXML
    private Button logoutButton;

    private final String TODAY_LABEL = LocalDate.now().toString() + " (Today)";
    private final String WEATHER_API_URL = "https://api.data.gov.my/weather/forecast?contains=Lumpur@location__location_name";
    private final String MOOD_API_URL = "https://router.huggingface.co/hf-inference/models/distilbert/distilbert-base-uncased-finetuned-sst-2-english";
    private final String ROOT_DIR = "JournalEntries";
    private String currentUserEmail;
    private String currentDisplayName = "User";

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        currentUserEmail = UserSession.getCurrentUser();

        if (currentUserEmail == null) {
            System.out.println("DEBUG: No user in session. Defaulting to test_user.");
            currentUserEmail = "test_user";
        } else {
            System.out.println("DEBUG: Journal loaded for: " + currentUserEmail);
        }

        currentDisplayName = getUserNameFromDB(currentUserEmail);
        displayName.setText("Welcome, " + currentDisplayName);

        loadDatesFromDB();

        journaltextArea.setStyle("-fx-control-inner-background: rgba(255, 255, 255, 0.7); "
                + "-fx-font-family: 'Book Antiqua'; "
                + "-fx-highlight-fill: #00BFFF; "
                + "-fx-highlight-text-fill: white;");

        dateList.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                handleDateSelection(newVal);
            }
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
        if (weeklySummaryButton != null) {
            weeklySummaryButton.setOnAction(event -> switchtoWeeklySummary());
        }
    }

    private void switchtoWeeklySummary() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("weeklySummary-page.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) weeklySummaryButton.getScene().getWindow();
            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            statusLabel.setText("Error loading page!");
        }
    }

    @FXML
    private void switchtoFirstPage(ActionEvent event) {
        UserSession.setCurrentUser(null);
        System.out.println("DEBUG: Session cleared. Logging out.");

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("first-page.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            statusLabel.setText("Error loading page!");
        }
    }

    private void loadDatesFromDB() {
        dateList.getItems().clear();
        dateList.getItems().add(TODAY_LABEL);

        File userFolder = new File(ROOT_DIR + File.separator + currentUserEmail);
        List<String> validDates = new ArrayList<>();

        if (userFolder.exists()) {
            File[] files = userFolder.listFiles((dir, name) -> name.endsWith(".txt"));
            if (files != null) {
                for (File f : files) {
                    String dateOnly = f.getName().replace(".txt", "");
                    validDates.add(dateOnly);

                    if (!dateOnly.equals(LocalDate.now().toString())) {
                        dateList.getItems().add(dateOnly);
                    }
                }
            }
        }
        if (!dateList.getItems().contains(TODAY_LABEL)) {
            dateList.getItems().add(0, TODAY_LABEL);
        }
        if (!validDates.isEmpty()) {
            Collections.sort(validDates);
            String datesString = String.join(",", validDates);

            String query = "INSERT INTO dates (`Display Name`, `Dates`) VALUES (?, ?) "
                    + "ON DUPLICATE KEY UPDATE `Dates` = ?";

            try (Connection conn = DatabaseConnection.getConnection();
                    PreparedStatement stmt = conn.prepareStatement(query)) {
                stmt.setString(1, currentDisplayName);
                stmt.setString(2, datesString);
                stmt.setString(3, datesString);
                stmt.executeUpdate();
                System.out.println("DEBUG: Auto-updated DB from files: " + datesString);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    

    

    private void updateDateInDB(String dateToUpdate, boolean isAdding) {
        List<String> dates = new ArrayList<>();

        String querySelect = "SELECT Dates FROM dates WHERE `Display Name` = ?";

        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(querySelect)) {
            stmt.setString(1, currentDisplayName);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                String raw = rs.getString("Dates");
                if (raw != null && !raw.isEmpty()) {
                    dates.addAll(Arrays.asList(raw.split(",")));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (isAdding) {
            if (!dates.contains(dateToUpdate)) {
                dates.add(dateToUpdate);
            }
        } else {
            dates.remove(dateToUpdate);
        }

        String newString = String.join(",", dates);

        String queryUpsert = "INSERT INTO dates (`Display Name`, Dates) VALUES (?, ?) "
                + "ON DUPLICATE KEY UPDATE Dates = ?";

        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(queryUpsert)) {
            stmt.setString(1, currentDisplayName);
            stmt.setString(2, newString);
            stmt.setString(3, newString);
            stmt.executeUpdate();
            System.out.println("DEBUG: DB updated for " + currentDisplayName + " -> " + newString);
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Failed to update database!");
        }
    }

    private String getUserNameFromDB(String email) {
        String name = "User";
        String query = "SELECT `Display Name` FROM user WHERE `Email Address` = ?";
        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, email);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                name = rs.getString("Display Name");
            }
        } catch (Exception e) {
            if (email.contains("@")) {
                name = email.split("@")[0];
            }
        }
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

    private void saveToFolder(String targetDate, String weather, String mood, String text) {
        File userFolder = new File(ROOT_DIR + File.separator + currentUserEmail);
        if (!userFolder.exists()) {
            userFolder.mkdirs();
        }

        File entryFile = new File(userFolder, targetDate + ".txt");

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(entryFile))) {
            writer.write(weather);
            writer.newLine();
            writer.write(mood);
            writer.newLine();
            writer.write(text);

            updateDateInDB(targetDate, true);

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
        File entryFile = new File(ROOT_DIR + File.separator + currentUserEmail, dateToFind + ".txt");
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

    private void deleteCurrentEntry() {
        String targetDate = cleanDate(dateLabel.getText());
        File entryFile = new File(ROOT_DIR + File.separator + currentUserEmail, targetDate + ".txt");

        if (entryFile.exists()) {
            entryFile.delete();
            updateDateInDB(targetDate, false);
            dateList.getItems().remove(targetDate);
        }

        journaltextArea.setText("");
        weatherLabel.setText("Weather: N/A");
        moodLabel.setText("Mood: N/A");
        statusLabel.setText("Entry Deleted.");
        saveButton.setText("Save Entry");
        deleteButton.setVisible(false);

        if (dateLabel.getText().equals(TODAY_LABEL)) {
            fetchWeatherOnly();
        }
    }

    //API
    private void fetchWeatherOnly() {
        new Thread(() -> {
            try {
                String response = API.get(WEATHER_API_URL);
                String weather = extractValue(response, "summary_forecast");
                if (weather.equals("N/A")) {
                    weather = extractValue(response, "temperature");
                }
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
            } catch (Exception e) {
            }

            String mood = "Neutral";
            try {
                String jsonBody = "{\"inputs\": \"" + journalText + "\"}";
                String response = API.post(MOOD_API_URL, jsonBody);
                if (response.contains("error")) {
                    mood = "API Error";
                } else {
                    mood = extractValue(response, "label");
                }
            } catch (Exception e) {
            }

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
        if (matcher.find()) {
            return matcher.group(1);
        }
        return "N/A";
    }
}
