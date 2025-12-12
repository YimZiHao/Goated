package com.mycompany.smartjournaling;

import java.io.IOException;
import java.net.URL;
import javafx.util.Duration;
import java.util.ResourceBundle;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import java.time.LocalTime;
import java.time.ZoneId;
import javafx.animation.PauseTransition;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class WelcomeController implements Initializable {

    @FXML
    private Label welcomeLabel;
    @FXML
    private Label welcomeLabel1;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        updateGreeting("User");
        
        PauseTransition delay = new PauseTransition(Duration.seconds(5));
        
        delay.setOnFinished(event -> {
            loadNextScene();
        });
        
        delay.play();
    }
    
    private void loadNextScene(){
        try{
            FXMLLoader loader = new FXMLLoader(getClass().getResource("JournalPage.fxml"));
            Parent root = loader.load();
            
            Stage stage = (Stage) welcomeLabel.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            System.out.println("Error loading the next scene.");
            e.printStackTrace();
        }
    }

    public void updateGreeting(String displayName) {
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

        welcomeLabel.setText(greeting + ", " + displayName);
        welcomeLabel1.setText(greeting + ", " + displayName);
    }
}