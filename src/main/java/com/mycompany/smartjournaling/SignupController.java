package com.mycompany.smartjournaling;

import com.mycompany.fopassignment.User;
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
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class SignupController {

    private static final String CONN_STRING =
            "jdbc:mysql://localhost:3306/smart_journal";
    private static final String DB_USER = "root";

    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;

    @FXML
    private void switchtoWelcomePage(ActionEvent event) throws IOException {

        String email = emailField.getText().trim();
        String rawPassword = passwordField.getText().trim();

        if (email.isEmpty() || rawPassword.isEmpty()) {
            showAlert("Error", "Please fill in all fields.");
            return;
        }

        String rawName = email.split("@")[0];
        String displayName =
                rawName.substring(0, 1).toUpperCase() + rawName.substring(1);

        // 🔐 Encrypt explicitly
        String encryptedPassword = User.cipher(rawPassword);
        User newUser = new User(email, displayName, encryptedPassword);

        if (registerUserInDB(newUser)) {
            FXMLLoader loader =
                    new FXMLLoader(getClass().getResource("welcome-page.fxml"));
            Parent root = loader.load();

            WelcomeController controller = loader.getController();
            controller.updateGreeting(displayName);

            Stage stage =
                    (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();
        }
    }

    private boolean registerUserInDB(User user) {

        MysqlDataSource dataSource = new MysqlDataSource();
        dataSource.setURL(CONN_STRING);
        dataSource.setUser(DB_USER);
        dataSource.setPassword(App.dbPassword);

        try (Connection conn = dataSource.getConnection()) {

            String checkEmail =
                    "SELECT COUNT(*) FROM user WHERE `Email Address`=?";
            try (PreparedStatement ps = conn.prepareStatement(checkEmail)) {
                ps.setString(1, user.getEmailAddress());
                ResultSet rs = ps.executeQuery();
                if (rs.next() && rs.getInt(1) > 0) {
                    showAlert("Error", "Email already exists.");
                    return false;
                }
            }

            String insert =
                    "INSERT INTO user (`Email Address`,`Password`,`Display Name`) VALUES (?,?,?)";
            try (PreparedStatement ps = conn.prepareStatement(insert)) {
                ps.setString(1, user.getEmailAddress());
                ps.setString(2, user.getPassword());
                ps.setString(3, user.getDisplayName());
                ps.executeUpdate();
            }

            createUserFiles(user);
            showAlert("Success", "Account created!");
            return true;

        } catch (SQLException e) {
            showAlert("DB Error", e.getMessage());
            return false;
        }
    }

    private void createUserFiles(User user) {
        try {
            Path path =
                    Path.of("Journal Entries", user.getDisplayName());
            Files.createDirectories(path);
            new File(path.toFile(), "Dates.txt").createNewFile();

            try (PrintWriter out =
                         new PrintWriter(new FileOutputStream("UserData.txt", true))) {
                out.println(user.getEmailAddress());
                out.println(user.getDisplayName());
                out.println(user.getPassword());
                out.println();
            }
        } catch (IOException e) {
            System.out.println("File creation failed.");
        }
    }

    private void showAlert(String title, String msg) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }
}
