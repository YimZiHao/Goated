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

    // --- Helper Method to Prompt for Password ---
    private boolean connectToDatabase() {
        // Create a popup dialog asking for input
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Database Authentication");
        dialog.setHeaderText("Database Connection Required");
        dialog.setContentText("Please enter the DB Password:");

        // Show the dialog and wait for the user to press OK/Cancel
        Optional<String> result = dialog.showAndWait();

        if (result.isPresent()) {
            String inputPassword = result.get();
            
            // Attempt to connect to MySQL
            MysqlDataSource dataSource = new MysqlDataSource();
            dataSource.setURL(CONN_STRING);
            dataSource.setUser("root");
            dataSource.setPassword(inputPassword);

            try (Connection conn = dataSource.getConnection()) {
                // Connection Successful!
                dbPassword = inputPassword; // Store it for later use
                System.out.println("Database connected successfully.");
                return true; 
            } catch (SQLException e) {
                // Connection Failed - Show Error Alert
                Alert alert = new Alert(AlertType.ERROR);
                alert.setTitle("Connection Failed");
                alert.setHeaderText("Could not connect to 'smart_journal' database");
                alert.setContentText(e.getMessage());
                alert.showAndWait();
                return false;
            }
        }
        // User clicked Cancel
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