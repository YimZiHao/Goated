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

public class JournalPageController implements Initializable {

    // --- FXML UI Components ---
    @FXML
    private ListView<String> dateList; // The sidebar list
    @FXML
    private Label dateHeader;          // "October 11, 2025"
    @FXML
    private Label weatherLabel;        // "Weather: Sunny"
    @FXML
    private Label moodLabel;           // "Mood: Positive"
    @FXML
    private TextArea journalTextArea;  // Where user types
    @FXML
    private Button saveButton;         // The Save button
    @FXML
    private Label statusLabel;         // "Saved successfully!"

    // --- Data Variables ---
    private LocalDate selectedDate;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        loadDateList();
        
        // Listener: When user clicks a date in the list
        dateList.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                handleDateSelection(newValue);
            }
        });

        // Button Action: When user clicks Save
        saveButton.setOnAction(event -> saveJournalEntry());
    }

    private void loadDateList() {
        // 1. Clear the list
        dateList.getItems().clear();
        
        // 2. Add past dates (Example: Past 3 days + Today)
        LocalDate today = LocalDate.now();
        dateList.getItems().add(today.minusDays(3).toString());
        dateList.getItems().add(today.minusDays(2).toString());
        dateList.getItems().add(today.minusDays(1).toString());
        dateList.getItems().add(today.toString() + " (Today)");
    }

    private void handleDateSelection(String dateString) {
        // Simple logic to check if it is "Today"
        boolean isToday = dateString.contains("Today");
        
        // Update the Header
        dateHeader.setText(dateString);
        
        if (isToday) {
            // EDIT MODE
            journalTextArea.setEditable(true);
            saveButton.setVisible(true);
            statusLabel.setText("");
            // TODO: Load existing text if we saved it before
        } else {
            // READ-ONLY MODE
            journalTextArea.setEditable(false);
            saveButton.setVisible(false);
            statusLabel.setText("");
            journalTextArea.setText("This is a past entry. You cannot edit it."); // Placeholder
        }
    }

    private void saveJournalEntry() {
        String text = journalTextArea.getText();
        
        if (text.isEmpty()) {
            statusLabel.setText("Please write something first!");
            return;
        }

        // TODO: This is where we will add the API Code later
        statusLabel.setText("Saving... (APIs not connected yet)");
        
        // Placeholder for future logic:
        // 1. Save text to file
        // 2. Call Weather API
        // 3. Call Mood API
    }
}