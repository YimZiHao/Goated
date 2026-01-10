package com.mycompany.smartjournaling;

import java.io.*;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextArea;
import javafx.stage.Stage;

public class WeeklySummaryController implements Initializable {

    @FXML
    private TextArea weeklySummaryTextArea;
    @FXML
    private Button backButton;
    @FXML
    private Label happyLabel;
    @FXML
    private Label sadLabel;
    @FXML
    private ProgressBar moodBar;
    @FXML
    private Label sunnyLabel;
    @FXML
    private Label rainyLabel;
    @FXML
    private ProgressBar weatherBar;

    private final String ROOT_DIR = "JournalEntries";
    private String currentUserEmail = UserSession.getCurrentUser();
    private String currentDisplayName = "User";

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        if (currentUserEmail == null) {
            currentUserEmail = "test_user";
        }

        currentDisplayName = getUserNameFromDB(currentUserEmail);
        System.out.println("DEBUG Summary: User is " + currentDisplayName);
        generateReport();
    }

    private void generateReport() {
        LocalDate today = LocalDate.now();

        List<String> validDates = getValidDatesFromDB();

        int entriesCount = 0;
        int happyCount = 0;
        int sadCount = 0;
        int sunnyCount = 0;
        int rainyCount = 0;

        // loop through past 7 days
        for (int i = 0; i < 7; i++) {
            LocalDate dateToCheck = today.minusDays(i);
            String dateString = dateToCheck.toString();

            // File Path: JournalEntries/email/date.txt
            File entryFile = new File(ROOT_DIR + File.separator + currentUserEmail + File.separator + dateString + ".txt");

            if (entryFile.exists() && validDates.contains(dateString)) {
                entriesCount++;
                try (BufferedReader reader = new BufferedReader(new FileReader(entryFile))) {
                    String weather = reader.readLine();
                    if (weather != null) {
                        String w = weather.toLowerCase();

                        if (w.contains("hujan") || w.contains("ribut") || w.contains("petir")) {
                            rainyCount++;
                        } else if (w.contains("tiada hujan") || w.contains("cerah") || w.contains("baik")) {
                            sunnyCount++;
                        } else {
                            sunnyCount++;
                        }
                    }

                    String mood = reader.readLine();
                    if (mood != null) {
                        String m = mood.toUpperCase();
                        if (m.contains("POSITIVE") || m.contains("HAPPY") || m.contains("JOY")) {
                            happyCount++;
                        } else {
                            sadCount++;
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        StringBuilder sb = new StringBuilder();
        if (entriesCount == 0) {
            sb.append("No entries found for this week.\nTry writing a journal today!");
            moodBar.setProgress(0.5);
            weatherBar.setProgress(0.5);
        } else {
            sb.append("\n\n\n📅 Past 7 Days Recap\n\n");
            if (entriesCount == 1) {
                sb.append("You wrote only ").append(entriesCount).append(" entry this week.\n\n");
            } else {
                sb.append("You wrote ").append(entriesCount).append(" entries this week.\n\n");
            }

            // Mood Summary
            if (happyCount > sadCount) {
                sb.append("Overall, you had a pleasant week! 😄\n");
            } else if (sadCount > happyCount) {
                sb.append("It was a bit of a tough week. 😞\n");
            } else {
                sb.append("Your mood was balanced this week.\n");
            }

            // Weather Summary
            if (rainyCount > sunnyCount) {
                sb.append("It was a rainy week. ⛈\n");
            } else {
                sb.append("The weather was mostly dry. ☀\n");
            }
        }
        weeklySummaryTextArea.setText(sb.toString());

        // MOOD BAR
        happyLabel.setText("😊 " + happyCount);
        sadLabel.setText(sadCount + " 😞");

        double totalMood = happyCount + sadCount;
        if (totalMood > 0) {
            moodBar.setProgress(happyCount / totalMood);
        } else {
            moodBar.setProgress(0.5);
        }

        // WEATHER BAR
        sunnyLabel.setText("☀ " + sunnyCount);
        rainyLabel.setText(rainyCount + " ⛈");

        double totalWeather = sunnyCount + rainyCount;
        if (totalWeather > 0) {
            weatherBar.setProgress(sunnyCount / totalWeather);
        } else {
            weatherBar.setProgress(0.5);
        }
    }

    private List<String> getValidDatesFromDB() {
        List<String> dates = new ArrayList<>();

        String query = "SELECT Dates FROM dates WHERE `Display Name` = ?";

        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, currentDisplayName);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                String d = rs.getString("Dates");
                if (d != null && !d.isEmpty()) {
                    dates = Arrays.asList(d.split(","));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return dates;
    }

    @FXML
    private void switchtoFirstPage(Event event) throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource("journal-page.fxml"));
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        Scene scene = new Scene(root);
        stage.setScene(scene);
        stage.show();
    }

    private String getUserNameFromDB(String email) {
        String name = "User";
        String query = "SELECT `Display Name` FROM user WHERE `Email Address` = ?";
        try (Connection connect = DatabaseConnection.getConnection();
                PreparedStatement stmt = connect.prepareStatement(query)) {
            stmt.setString(1, email);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                name = rs.getString("Display Name");
        
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return name;
    }
}

