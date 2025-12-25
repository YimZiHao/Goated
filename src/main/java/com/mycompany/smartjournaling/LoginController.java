package com.mycompany.smartjournaling;

import com.mycompany.fopassignment.User;
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
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class LoginController {

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

        // 🔐 Encrypt explicitly
        String encryptedPassword = User.cipher(rawPassword);
        String displayName = validateLogin(email, encryptedPassword);

        if (displayName != null) {

            FXMLLoader loader =
                    new FXMLLoader(getClass().getResource("welcome-page.fxml"));
            Parent root = loader.load();

            WelcomeController controller = loader.getController();
            controller.updateGreeting(displayName);

            Stage stage =
                    (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();

        } else {
            showAlert("Login Failed", "Invalid email or password.");
        }
    }

    private String validateLogin(String email, String encryptedPassword) {

        MysqlDataSource ds = new MysqlDataSource();
        ds.setURL(CONN_STRING);
        ds.setUser(DB_USER);
        ds.setPassword(App.dbPassword);

        String sql =
                "SELECT `Display Name` FROM user WHERE `Email Address`=? AND `Password`=?";

        try (Connection conn = ds.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, email);
            ps.setString(2, encryptedPassword);

            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getString("Display Name");
            }

        } catch (SQLException e) {
            showAlert("DB Error", e.getMessage());
        }
        return null;
    }

    private void showAlert(String title, String msg) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }
}
