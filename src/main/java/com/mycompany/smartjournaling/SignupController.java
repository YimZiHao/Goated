package com.mycompany.smartjournaling;

import com.mycompany.fopassignment.User; 
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

    @FXML Button signupButton;
    @FXML Button backButton;
    @FXML private TextField nameField; 
    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;

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
        String displayName = nameField.getText();
        String email = emailField.getText();
        String pass = passwordField.getText();

        if (displayName.isEmpty() || email.isEmpty() || pass.isEmpty()) {
            showAlert("Error", "Please fill in all fields (Name, Email, Password).");
            return;
        }

        User newUser = new User(email, displayName, pass);

        boolean success = registerUserInDB(newUser);

        if (success) {
            UserSession.setCurrentUser(email); 
            System.out.println("DEBUG: Session updated to new user: " + email);

            FXMLLoader loader = new FXMLLoader(getClass().getResource("welcome-page.fxml"));
            Parent root = loader.load();

            WelcomeController welcomeController = loader.getController();
            welcomeController.updateGreeting(displayName);

            Stage stage = (Stage)((Node)event.getSource()).getScene().getWindow();
            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.show();
        }
    }
    // db user table
    private boolean registerUserInDB(User user) {
        try (Connection connection = DatabaseConnection.getConnection()) {
            String checkNameSQL = "SELECT COUNT(*) FROM user WHERE `Display Name` = ?";
            try (PreparedStatement checkStmt = connection.prepareStatement(checkNameSQL)) {
                checkStmt.setString(1, user.getDisplayName());
                ResultSet rs = checkStmt.executeQuery();
                if (rs.next() && rs.getInt(1) > 0) {
                    showAlert("Registration Failed", "Display Name already taken!");
                    return false;
                }
            }
            String checkEmailSQL = "SELECT COUNT(*) FROM user WHERE `Email Address` = ?";
            try (PreparedStatement checkStmt = connection.prepareStatement(checkEmailSQL)) {
                checkStmt.setString(1, user.getEmailAddress());
                ResultSet rs = checkStmt.executeQuery();
                if (rs.next() && rs.getInt(1) > 0) {
                    showAlert("Registration Failed", "Email already exists!");
                    return false;
                }
            }
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
        }
        return false;
    }

    private void createFilesForUser(User user) {
        try (PrintWriter outputStream = new PrintWriter(new FileOutputStream("UserData.txt", true))) {
            outputStream.println(user.getEmailAddress());
            outputStream.println(user.getDisplayName());
            outputStream.println(user.getPassword() + "\n");
        } catch (IOException e) {
            System.out.println("Error writing text file: " + e.getMessage());
        }
        try {
            Path path = Path.of("JournalEntries", user.getEmailAddress());
            Files.createDirectories(path);
            System.out.println("Created folder for: " + user.getEmailAddress());
            
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