package com.mycompany.fopassignment;

import java.sql.Connection;
import java.sql.SQLException;
import com.mysql.cj.jdbc.MysqlDataSource;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Scanner;
import javax.swing.JOptionPane;
import javax.swing.JPasswordField;

public class SignUp {

    private final static String CONN_STRING = "jdbc:mysql://localhost:3306/goated";

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        // Prompt password
        JPasswordField pf = new JPasswordField();
        int okCxl = JOptionPane.showConfirmDialog(null, pf, "Enter DB Password", JOptionPane.OK_CANCEL_OPTION);
        final char[] passwordDB = (okCxl == JOptionPane.OK_OPTION) ? pf.getPassword() : null;

        MysqlDataSource dataSource = new MysqlDataSource();
        dataSource.setURL(CONN_STRING);

        // YOUR DB credentials
        dataSource.setUser("root");
        dataSource.setPassword(new String(passwordDB));

        try (Connection connection = dataSource.getConnection()) {
            JOptionPane.showMessageDialog(null, "Success! Connection made to 'goated' database.");

            System.out.print("Enter Your Name: ");
            String displayName = sc.nextLine();
            System.out.print("Enter Your Email: ");
            String email = sc.nextLine();
            JPasswordField jk = new JPasswordField();
            okCxl = JOptionPane.showConfirmDialog(null, jk, "Enter Your Password", JOptionPane.OK_CANCEL_OPTION);
            final char[] password = (okCxl == JOptionPane.OK_OPTION) ? jk.getPassword() : null;
            insertUser(new User(email, displayName, new String(password)), connection);

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "Connection failed:\n" + e.getMessage());
        }
    }

    public static void insertUser(User user, Connection connection) {
        try {
            // -------------------------------
            // 1. Input validation
            // -------------------------------
            if (user.getEmailAddress().isEmpty()
                    || user.getDisplayName().isEmpty()
                    || user.getPassword().isEmpty()) {

                JOptionPane.showMessageDialog(null, "All fields are required!");
                return;
            }

            if (!user.getEmailAddress().contains("@") || !user.getEmailAddress().contains(".")) {
                JOptionPane.showMessageDialog(null, "Please enter a valid email address!");
                return;
            }

            // -------------------------------
            // 2. Check if Display Name exists
            // -------------------------------
            String checkNameSQL = "SELECT COUNT(*) FROM user WHERE `Display Name` = ?";
            try (PreparedStatement checkStmt = connection.prepareStatement(checkNameSQL)) {

                checkStmt.setString(1, user.getDisplayName());

                try (ResultSet rs = checkStmt.executeQuery()) {
                    if (rs.next() && rs.getInt(1) > 0) {
                        JOptionPane.showMessageDialog(null, "Display Name already taken!");
                        return;
                    }
                }
            }

            // -------------------------------
            // 3. Check if Email exists
            // -------------------------------
            String checkEmailSQL = "SELECT COUNT(*) FROM user WHERE `Email Address` = ?";
            try (PreparedStatement checkStmt = connection.prepareStatement(checkEmailSQL)) {

                checkStmt.setString(1, user.getEmailAddress());

                try (ResultSet rs = checkStmt.executeQuery()) {
                    if (rs.next() && rs.getInt(1) > 0) {
                        JOptionPane.showMessageDialog(null, "Email already exists!");
                        return;
                    }
                }
            }

            // -------------------------------
            // 4. Insert user into MySQL database and txt file
            // -------------------------------
            String insertSQL = "INSERT INTO user (`Email Address`, `Password`, `Display Name`) VALUES (?, ?, ?)";

            try (PreparedStatement insertStmt = connection.prepareStatement(insertSQL)) {

                insertStmt.setString(1, user.getEmailAddress());
                insertStmt.setString(2, user.getPassword());
                insertStmt.setString(3, user.getDisplayName());

                int rows = insertStmt.executeUpdate();

                if (rows > 0) {
                    JOptionPane.showMessageDialog(null,
                            "User registered successfully!\n"
                            + "Email: " + user.getEmailAddress() + "\n"
                            + "Display Name: " + user.getDisplayName());

                    //Write new user into UserData.txt
                    try {
                        PrintWriter outputStream = new PrintWriter(new FileOutputStream("src\\main\\java\\com\\mycompany\\fopassignment\\UserData.txt", true));
                        outputStream.println(user.getEmailAddress());
                        outputStream.println(user.getDisplayName());
                        outputStream.println(user.getPassword() + "\n");
                        outputStream.close();
                    } catch (IOException e) {
                        System.out.print("Something went wrong with output!!!");
                    }
                      
                    //Create new folder to store journal entries
                    Path path = Path.of("Journal Entries\\%s".formatted(user.getDisplayName()));
                    try {
                        Files.createDirectory(path);
                        System.out.println("Folder created!");
                    } catch (IOException e) {
                        System.out.println("Error: " + e.getMessage());
                    } asfds

                    }else {
                    JOptionPane.showMessageDialog(null, "Failed to register user!");
                }
            }

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "Database error:\n" + e.getMessage());
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Unexpected error:\n" + e.getMessage());
        }
    }
}
