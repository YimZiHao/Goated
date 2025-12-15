package com.mycompany.smartjournaling;

// Import your teammate's User class
import com.mycompany.fopassignment.User; 
// Import Database tools
import com.mysql.cj.jdbc.MysqlDataSource; 

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class LoginController {

    // --- DATABASE CONFIGURATION ---
    // Ensure this matches App.java exactly ("smart_journal")
    private final static String CONN_STRING = "jdbc:mysql://localhost:3306/smart_journal";
    private final static String DB_USER = "root";

    @FXML
    private TextField emailField;      
    @FXML
    private PasswordField passwordField; 
    @FXML
    private Button loginButton;
    @FXML
    private Button backButton;

    // --- NAVIGATION HELPERS ---
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
    
    // --- MAIN LOGIN LOGIC ---
    @FXML
    private void switchtoWelcomePage(ActionEvent event) throws IOException {
        String inputEmail = emailField.getText();
        String inputPass = passwordField.getText();

        // 1. Check if empty
        if (inputEmail.isEmpty() || inputPass.isEmpty()) {
            showAlert("Error", "Please fill in all fields.");
            return;
        }

        // 2. ENCRYPT THE PASSWORD
        // Create temp user to generate the encrypted password string.
        User tempUser = new User(inputEmail, "dummyName", inputPass);
        String encryptedPassword = tempUser.getPassword(); 

        // 3. CHECK DATABASE
        String displayName = validateLogin(inputEmail, encryptedPassword);

        if (displayName != null) {
            // LOGIN SUCCESS!
            
            FXMLLoader loader = new FXMLLoader(getClass().getResource("welcome-page.fxml"));
            Parent root = loader.load();

            // Pass the user's name to the WelcomeController
            WelcomeController welcomeController = loader.getController();
            welcomeController.updateGreeting(displayName); // Use the real name from DB

            Stage stage = (Stage)((Node)event.getSource()).getScene().getWindow();
            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.show();
            
        } else {
            // LOGIN FAILED
            showAlert("Login Failed", "Invalid Email or Password.");
        }
    }

    // Checking the database
    private String validateLogin(String email, String cipheredPassword) {
        MysqlDataSource dataSource = new MysqlDataSource();
        dataSource.setURL(CONN_STRING);
        dataSource.setUser(DB_USER);
        
        // --- CRITICAL FIX ---
        // Use the password stored in App.java!
        dataSource.setPassword(App.dbPassword);
        // --------------------

        String query = "SELECT `Display Name` FROM user WHERE `Email Address` = ? AND `Password` = ?";

        try (Connection connection = dataSource.getConnection();
             PreparedStatement stmt = connection.prepareStatement(query)) {

            stmt.setString(1, email);
            stmt.setString(2, cipheredPassword);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    // Return the Display Name found in the database
                    return rs.getString("Display Name");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Database Error", "Connection failed: " + e.getMessage());
        }
        return null; // Return null if no match found
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}