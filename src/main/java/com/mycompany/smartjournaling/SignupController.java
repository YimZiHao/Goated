package com.mycompany.smartjournaling;

import com.mycompany.fopassignment.User; 
import com.mysql.cj.jdbc.MysqlDataSource;
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

    private final static String CONN_STRING = "jdbc:mysql://localhost:3306/smart_journal";
    private final static String DB_USER = "root";     

    @FXML Button signupButton;
    @FXML Button backButton;
    
    // NEW: Field for the user to type their name
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
        // 1. Get Manual Input
        String displayName = nameField.getText();
        String email = emailField.getText();
        String pass = passwordField.getText();

        // 2. Validate
        if (displayName.isEmpty() || email.isEmpty() || pass.isEmpty()) {
            showAlert("Error", "Please fill in all fields (Name, Email, Password).");
            return;
        }

        // 3. Create User Object
        User newUser = new User(email, displayName, pass);

        // 4. Attempt Registration
        boolean success = registerUserInDB(newUser);

        // 5. Switch Scene if successful
        if (success) {
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

    private boolean registerUserInDB(User user) {
        // Use the new helper class!
        try (Connection connection = DatabaseConnection.getConnection()) {
            
            // 1. Check Display Name (Use backticks for spaces!)
            String checkNameSQL = "SELECT COUNT(*) FROM user WHERE `Display Name` = ?";
            try (PreparedStatement checkStmt = connection.prepareStatement(checkNameSQL)) {
                checkStmt.setString(1, user.getDisplayName());
                ResultSet rs = checkStmt.executeQuery();
                if (rs.next() && rs.getInt(1) > 0) {
                    showAlert("Registration Failed", "Display Name already taken!");
                    return false;
                }
            }

            // 2. Check Email
            String checkEmailSQL = "SELECT COUNT(*) FROM user WHERE `Email Address` = ?";
            try (PreparedStatement checkStmt = connection.prepareStatement(checkEmailSQL)) {
                checkStmt.setString(1, user.getEmailAddress());
                ResultSet rs = checkStmt.executeQuery();
                if (rs.next() && rs.getInt(1) > 0) {
                    showAlert("Registration Failed", "Email already exists!");
                    return false;
                }
            }

            // 3. Insert User (Note the column names!)
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
        // Append to UserData.txt (Legacy/Backup)
        try (PrintWriter outputStream = new PrintWriter(new FileOutputStream("UserData.txt", true))) {
            outputStream.println(user.getEmailAddress());
            outputStream.println(user.getDisplayName());
            outputStream.println(user.getPassword() + "\n");
        } catch (IOException e) {
            System.out.println("Error writing text file: " + e.getMessage());
        }

        // Create Journal Folders
        try {
            // FIX: Use EMAIL for the folder name so it matches JournalPageController logic
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