package com.mycompany.smartjournaling;

import com.mycompany.fopassignment.User; // Ensure this matches where your User class is
import com.mysql.cj.jdbc.MysqlDataSource;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
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

public class SignupController {

    // 1. UPDATED: Match the database name used in App.java ("goated")
    private final static String CONN_STRING = "jdbc:mysql://localhost:3306/smart_journal";
    private final static String DB_USER = "root";     

    @FXML
    Button signupButton;
    @FXML
    Button backButton;
    @FXML
    private TextField emailField;
    @FXML
    private PasswordField passwordField;

    @FXML
    private void switchScene(ActionEvent event, String fxmlFileName) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlFileName));
        Parent root = loader.load();
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
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
        String email = emailField.getText();
        String pass = passwordField.getText();

        if (email.isEmpty() || pass.isEmpty()) {
            showAlert("Error", "Please fill in all fields.");
            return;
        }

        // Generate Display Name from Email
        String rawName;
        if (email.contains("@")) {
            rawName = email.split("@")[0];
        } else {
            rawName = email;
        }

        String displayName = rawName;
        if (rawName != null && !rawName.isEmpty()) {
            displayName = rawName.substring(0, 1).toUpperCase() + rawName.substring(1);
        }

        // Create User Object
        User newUser = new User(email, displayName, pass);

        // Attempt Registration
        boolean success = registerUserInDB(newUser);

        // Only switch scene if successful
        if (success) {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("welcome-page.fxml"));
            Parent root = loader.load();

            WelcomeController welcomeController = loader.getController();
            // Pass the email or name to the welcome page
            welcomeController.updateGreeting(email);

            Stage stage = (Stage)((Node)event.getSource()).getScene().getWindow();
            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.show();
        }
    }

    private boolean registerUserInDB(User user) {
        MysqlDataSource dataSource = new MysqlDataSource();
        dataSource.setURL(CONN_STRING);
        dataSource.setUser(DB_USER);
        
        // --- CRITICAL FIX ---
        // Use the password we saved in App.java!
        dataSource.setPassword(App.dbPassword); 
        // --------------------

        try (Connection connection = dataSource.getConnection()) {
            
            // Check if Name Exists
            String checkNameSQL = "SELECT COUNT(*) FROM user WHERE `Display Name` = ?";
            try (PreparedStatement checkStmt = connection.prepareStatement(checkNameSQL)) {
                checkStmt.setString(1, user.getDisplayName());
                try (ResultSet rs = checkStmt.executeQuery()) {
                    if (rs.next() && rs.getInt(1) > 0) {
                        showAlert("Registration Failed", "Display Name already taken!");
                        return false;
                    }
                }
            }

            // Check if Email Exists
            String checkEmailSQL = "SELECT COUNT(*) FROM user WHERE `Email Address` = ?";
            try (PreparedStatement checkStmt = connection.prepareStatement(checkEmailSQL)) {
                checkStmt.setString(1, user.getEmailAddress());
                try (ResultSet rs = checkStmt.executeQuery()) {
                    if (rs.next() && rs.getInt(1) > 0) {
                        showAlert("Registration Failed", "Email already exists!");
                        return false;
                    }
                }
            }

            // Insert User
            String insertSQL = "INSERT INTO user (`Email Address`, `Password`, `Display Name`) VALUES (?, ?, ?)";
            try (PreparedStatement insertStmt = connection.prepareStatement(insertSQL)) {
                insertStmt.setString(1, user.getEmailAddress());
                insertStmt.setString(2, user.getPassword());
                insertStmt.setString(3, user.getDisplayName());

                int rows = insertStmt.executeUpdate();

                if (rows > 0) {
                    createFilesForUser(user);
                    showAlert("Success", "Account created successfully!");
                    return true;
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Database Error", "Connection failed: " + e.getMessage());
            return false;
        }
        return false;
    }

    private void createFilesForUser(User user) {
        // Append to UserData.txt
        try (PrintWriter outputStream = new PrintWriter(new FileOutputStream("UserData.txt", true))) {
            outputStream.println(user.getEmailAddress());
            outputStream.println(user.getDisplayName());
            outputStream.println(user.getPassword() + "\n");
        } catch (IOException e) {
            System.out.println("Error writing text file: " + e.getMessage());
        }

        // Create Journal Folders
        try {
            // Using "Journal Entries" folder as root
            Path path = Path.of("Journal Entries", user.getDisplayName());
            Files.createDirectories(path);
            
            File datesFile = new File(path.toFile(), "Dates.txt");
            datesFile.createNewFile();
            
        } catch (IOException e) {
            System.out.println("Error creating folders: " + e.getMessage());
        }
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}