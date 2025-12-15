package com.mycompany.smartjournaling;

import com.mycompany.fopassignment.User; // IMPORT YOUR USER CLASS
import com.mysql.cj.jdbc.MysqlDataSource; // IMPORT MYSQL LIBRARY
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

    // --- DATABASE CONFIGURATION ---
    private final static String CONN_STRING = "jdbc:mysql://localhost:3306/goated";
    private final static String DB_USER = "root";     
    private final static String DB_PASSWORD = ""; // <--- WRITE YOUR DB PASSWORD HERE (Leave empty if none)

    @FXML
    Button signupButton;
    Button backButton;
    @FXML
    private TextField emailbox;
    @FXML
    private PasswordField password;

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
        String email = emailbox.getText();
        String pass = password.getText();

        if (email.isEmpty() || pass.isEmpty()) {
            showAlert("Error", "Please fill in all fields.");
            return;
        }

        // 1. Generate a Display Name from Email (since we don't have a name field yet)
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

        // 2. Create the User Object
        // (The User class constructor automatically ciphers the password!)
        User newUser = new User(email, displayName, pass);

        // 3. Attempt to Register in Database
        boolean success = registerUserInDB(newUser);

        // 4. Only switch scene if registration was successful
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

    // --- MERGED LOGIC FROM SignUp.java ---
    private boolean registerUserInDB(User user) {
        MysqlDataSource dataSource = new MysqlDataSource();
        dataSource.setURL(CONN_STRING);
        dataSource.setUser(DB_USER);
        dataSource.setPassword(DB_PASSWORD);

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
                insertStmt.setString(2, user.getPassword()); // Ciphered password
                insertStmt.setString(3, user.getDisplayName());

                int rows = insertStmt.executeUpdate();

                if (rows > 0) {
                    // Success! Create the files now
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
        // 1. Append to UserData.txt
        // I simplified the path to be in the project root folder so it works on all PCs
        try (PrintWriter outputStream = new PrintWriter(new FileOutputStream("UserData.txt", true))) {
            outputStream.println(user.getEmailAddress());
            outputStream.println(user.getDisplayName());
            outputStream.println(user.getPassword() + "\n");
        } catch (IOException e) {
            System.out.println("Error writing text file: " + e.getMessage());
        }

        // 2. Create Journal Folders
        try {
            Path path = Path.of("Journal Entries", user.getDisplayName());
            Files.createDirectories(path);
            
            // Create Dates.txt inside that folder
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