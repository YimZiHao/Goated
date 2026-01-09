package com.mycompany.smartjournaling;

import com.mycompany.fopassignment.User; 
import java.io.IOException;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ResourceBundle;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class LoginController implements Initializable {

    @FXML private TextField emailField;      
    @FXML private PasswordField passwordField; 
    @FXML private Button loginButton;
    @FXML private Button backButton;
    @FXML private TextField passwordTextField; 
    @FXML private CheckBox showPasswordCheckBox;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        if (passwordTextField != null) {
            
            passwordTextField.textProperty().bindBidirectional(passwordField.textProperty());
            passwordTextField.setVisible(false);
            passwordField.setVisible(true);
        } else {
            System.out.println("WARNING: passwordTextField is missing from FXML. Show Password feature disabled.");
        }
    }

    // NAVIGATION
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
    private void switchtoFirstPage(ActionEvent event) throws IOException {
        switchScene(event, "first-page.fxml");
    }
    
    @FXML
    private void switchtoWelcomePage(ActionEvent event) throws IOException {
        String inputEmail = emailField.getText();
        String inputPass = passwordField.getText();

        if (inputEmail.isEmpty() || inputPass.isEmpty()) {
            showAlert("Error", "Please fill in all fields.");
            return;
        }

        User tempUser = new User(inputEmail, "dummyName", inputPass);
        String encryptedPassword = tempUser.getPassword(); 

        String displayName = validateLogin(inputEmail, encryptedPassword);

        if (displayName != null) {
            UserSession.setCurrentUser(inputEmail); 
            
            FXMLLoader loader = new FXMLLoader(getClass().getResource("welcome-page.fxml"));
            Parent root = loader.load();

            WelcomeController welcomeController = loader.getController();
            welcomeController.updateGreeting(displayName); 

            Stage stage = (Stage)((Node)event.getSource()).getScene().getWindow();
            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.show();
            
        } else {
            showAlert("Login Failed", "Invalid Email or Password.");
        }
    }

    private String validateLogin(String email, String cipheredPassword) {
        // Query looks for 'Display Name' inside the 'user' table
        String query = "SELECT `Display Name` FROM user WHERE `Email Address` = ? AND `Password` = ?";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement stmt = connection.prepareStatement(query)) {

            stmt.setString(1, email);
            stmt.setString(2, cipheredPassword);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("Display Name");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Database Error", "Connection failed: " + e.getMessage());
        }
        return null; 
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}