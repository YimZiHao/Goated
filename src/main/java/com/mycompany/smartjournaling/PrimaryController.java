package com.mycompany.smartjournaling;

import java.io.IOException;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;

public class PrimaryController {
    @FXML
    ImageView myBackground;
    Button signupButton;
    Button loginButton;

    @FXML
    private void switchScene(ActionEvent event, String fxmlFileName) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlFileName));
        Parent root = loader.load();
        Stage stage = (Stage)((Node)event.getSource()).getScene().getWindow();
        Scene scene = new Scene(root);
        stage.setScene(scene);
        stage.show();
    }
    
    @FXML
    private void switchtoSignupPage(ActionEvent event) throws IOException {
        switchScene(event, "signup-page.fxml");
    }
    
    @FXML
    private void switchtoLoginPage(ActionEvent event) throws IOException {
        switchScene(event, "login-page.fxml");
    }
}
