package com.mycompany.UI;

import java.net.URL;
import java.util.ResourceBundle;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import java.time.LocalTime;
import java.time.ZoneId;

public class WelcomeController implements Initializable {

    @FXML
    private Label welcomeLabel;
    @FXML
    private Label welcomeLabel1;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        updateGreeting("User");
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