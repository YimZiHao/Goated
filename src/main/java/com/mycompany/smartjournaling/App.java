package com.mycompany.smartjournaling;

import com.mysql.cj.jdbc.MysqlDataSource;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Optional;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.PasswordField;
import javafx.scene.layout.VBox;

public class App extends Application {

    private static Scene scene;

    // 1. Add a global variable to store the password
    public static String dbPassword = ""; 
    // 2. Define your database URL
    private final String CONN_STRING = "jdbc:mysql://localhost:3306/smart_journal";

    @Override
    public void start(Stage stage) throws IOException {
        // 3. Check Database Connection BEFORE loading the first page
        if (!connectToDatabase()) {
            // If the user clicks cancel or gets the password wrong, close the app
            System.exit(0);
        }

        // 4. If successful, load the main application
        scene = new Scene(loadFXML("first-page"), 1280, 800);
        stage.setScene(scene);
        stage.show();
    }

    // --- Helper Method to Prompt for Password (MASKED) ---
    private boolean connectToDatabase() {
        // 1. Create a custom Dialog instead of TextInputDialog
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Database Authentication");
        dialog.setHeaderText("Database Connection Required");

        // 2. Set the button types (OK and Cancel)
        ButtonType loginButtonType = new ButtonType("OK", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(loginButtonType, ButtonType.CANCEL);

        // 3. Create the PasswordField
        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Password");

        // 4. Layout the password field
        VBox content = new VBox();
        content.getChildren().add(passwordField);
        dialog.getDialogPane().setContent(content);

        // 5. Convert the result to the password string when OK is clicked
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == loginButtonType) {
                return passwordField.getText();
            }
            return null;
        });

        // 6. Show the dialog and handle the result
        Optional<String> result = dialog.showAndWait();

        if (result.isPresent()) {
            String inputPassword = result.get();

            MysqlDataSource dataSource = new MysqlDataSource();
            dataSource.setURL(CONN_STRING);
            dataSource.setUser("root");
            dataSource.setPassword(inputPassword);

            try (Connection conn = dataSource.getConnection()) {
                dbPassword = inputPassword; 
                System.out.println("Database connected successfully.");
                return true; 
            } catch (SQLException e) {
                Alert alert = new Alert(AlertType.ERROR);
                alert.setTitle("Connection Failed");
                alert.setHeaderText("Could not connect to database");
                alert.setContentText(e.getMessage());
                alert.showAndWait();
                return false;
            }
        }
        return false; 
    }

    static void setRoot(String fxml) throws IOException {
        scene.setRoot(loadFXML(fxml));
    }

    private static Parent loadFXML(String fxml) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(App.class.getResource(fxml + ".fxml"));
        return fxmlLoader.load();
    }

    public static void main(String[] args) {
        launch();
    }
}