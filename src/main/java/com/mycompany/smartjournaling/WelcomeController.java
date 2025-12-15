package com.mycompany.smartjournaling;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;
import javafx.animation.PauseTransition; // 1. Import for the timer
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.stage.Stage;
import javafx.util.Duration; // 2. Import for time duration
import java.time.LocalTime;
import java.time.ZoneId;

public class WelcomeController implements Initializable {

    @FXML
    private Label welcomeLabel;
    @FXML
    private Label welcomeLabel1;

    // Variable to hold the name so we can pass it to the next page if needed
    private String currentDisplayName; 

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // Standard initialization
    }

    public void updateGreeting(String displayName) {
        this.currentDisplayName = displayName;

        ZoneId zone = ZoneId.of("GMT+8");
        LocalTime now = LocalTime.now(zone);
        int hour = now.getHour();
        String greeting;

        if (hour < 12) {
            greeting = "Good Morning";
        } else if (hour < 17) {
            greeting = "Good Afternoon";
        } else {
            greeting = "Good Evening";
        }

        // Update labels
        welcomeLabel.setText(greeting + ", " + displayName);

        // 3. Start the timer immediately after updating the greeting
        startTransitionTimer();
    }

    private void startTransitionTimer() {
        // Create a 5-second pause
        PauseTransition delay = new PauseTransition(Duration.seconds(3));
        
        // Define what happens when the 5 seconds represent up
        delay.setOnFinished(event -> {
            try {
                switchToJournalPage();
            } catch (IOException e) {
                e.printStackTrace();
                System.out.println("Could not load journal-page.fxml");
            }
        });
        
        // Start the timer
        delay.play();
    }

    private void switchToJournalPage() throws IOException {
        // Load the next FXML file (Make sure the name matches your file exactly!)
        FXMLLoader loader = new FXMLLoader(getClass().getResource("journal-page.fxml"));
        Parent root = loader.load();

        // OPTIONAL: Pass the username to the JournalController here if needed
        // JournalController controller = loader.getController();
        // controller.setUserName(currentDisplayName);

        // Get the current window (Stage) using one of the labels
        Stage stage = (Stage) welcomeLabel.getScene().getWindow();
        Scene scene = new Scene(root);
        
        stage.setScene(scene);
        stage.show();
    }
}