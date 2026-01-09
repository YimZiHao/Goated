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
    public static String dbPassword = ""; 
    private final String CONN_STRING = "jdbc:mysql://localhost:3306/smart_journal";

    @Override
    public void start(Stage stage) throws IOException {
        if (!connectToDatabase()) {
            System.exit(0);
        }

        scene = new Scene(loadFXML("first-page"), 1280, 800);
        stage.setScene(scene);
        stage.show();
    }

    private boolean connectToDatabase() {
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Database Authentication");
        dialog.setHeaderText("Database Connection Required");

        ButtonType loginButtonType = new ButtonType("OK", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(loginButtonType, ButtonType.CANCEL);

        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Password");

        VBox content = new VBox();
        content.getChildren().add(passwordField);
        dialog.getDialogPane().setContent(content);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == loginButtonType) {
                return passwordField.getText();
            }
            return null;
        });

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