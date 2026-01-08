package com.mycompany.smartjournaling;

import java.io.*;
import java.net.URL;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;
import javafx.event.Event; // CHANGED: Use generic Event to handle both Mouse and Action
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.stage.Stage;

public class WeeklySummaryController implements Initializable {

    @FXML private TextArea weeklySummaryTextArea; 
    @FXML private Button backButton;

    private final String ROOT_DIR = "JournalEntries";
    private String currentUser = UserSession.getCurrentUser();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        if (currentUser == null) currentUser = "test_user";
        generateReport();
    }

    private void generateReport() {
        LocalDate today = LocalDate.now();
        int entriesCount = 0;
        List<String> moodList = new ArrayList<>();
        List<String> weatherList = new ArrayList<>();

        // 1. Loop through past 7 days
        for (int i = 0; i < 7; i++) {
            LocalDate dateToCheck = today.minusDays(i);
            String dateString = dateToCheck.toString();
            File entryFile = new File(ROOT_DIR + File.separator + currentUser, dateString + ".txt");

            if (entryFile.exists()) {
                entriesCount++;
                try (BufferedReader reader = new BufferedReader(new FileReader(entryFile))) {
                    String w = reader.readLine(); // Line 1: Weather
                    if (w != null) weatherList.add(w);
                    
                    String m = reader.readLine(); // Line 2: Mood
                    if (m != null) moodList.add(m);
                } catch (IOException e) { e.printStackTrace(); }
            }
        }

        // 2. Build the Text
        StringBuilder sb = new StringBuilder();
        
        if (entriesCount == 0) {
            sb.append("No entries found for this week.\n");
            sb.append("Try writing a journal today!");
        } else {
            sb.append(" Total Entries: ").append(entriesCount).append("\n\n");
            sb.append("Most Common Mood: ").append(getMostFrequent(moodList)).append("\n\n");
            sb.append("Typical Weather: ").append(getMostFrequent(weatherList)).append("\n");
        }

        weeklySummaryTextArea.setText(sb.toString());
    }

    private String getMostFrequent(List<String> list) {
        if (list.isEmpty()) return "N/A";
        return list.stream()
            .collect(Collectors.groupingBy(s -> s, Collectors.counting()))
            .entrySet().stream()
            .max((e1, e2) -> e1.getValue().compareTo(e2.getValue()))
            .map(entry -> entry.getKey() + " (" + entry.getValue() + "x)")
            .orElse("N/A");
    }

    // --- FIX: Renamed to match FXML & changed to generic Event ---
    @FXML
    private void switchtoFirstPage(Event event) throws IOException {
        // Loads Journal Page (Going "Back")
        Parent root = FXMLLoader.load(getClass().getResource("journal-page.fxml"));
        Stage stage = (Stage)((Node)event.getSource()).getScene().getWindow();
        Scene scene = new Scene(root);
        stage.setScene(scene);
        stage.show();
    }
}