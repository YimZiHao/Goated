package com.mycompany.fopassignment;

import java.sql.Connection;
import java.sql.SQLException;
import com.mysql.cj.jdbc.MysqlDataSource;
import java.sql.PreparedStatement;
import javax.swing.JOptionPane;
import javax.swing.JPasswordField;

public class SignUp {

    private final static String CONN_STRING = "jdbc:mysql://localhost:3306/goated";

    public static void main(String[] args) {
//        String username = JOptionPane.showInputDialog(null, "Enter DB Username");
//
//        JPasswordField pf = new JPasswordField();
//        int okCxl = JOptionPane.showConfirmDialog(null, pf, "Enter DB Password", JOptionPane.OK_CANCEL_OPTION);
//        final char[] password = (okCxl == JOptionPane.OK_OPTION) ? pf.getPassword() : null;
//
//        if (username == null || username.isEmpty() || password == null || password.length == 0) {
//            JOptionPane.showMessageDialog(null, "Username or password cannot be empty!");
//            return;
//        }

        MysqlDataSource dataSource = new MysqlDataSource();
        dataSource.setURL(CONN_STRING);
//        dataSource.setUser(username);
//        dataSource.setPassword(new String(password));

        //Only use these two lines while coding and set your username and password to skip login everytime.
        //Remember to delete your username and password before pushing.
        //Line 15 to 24 and 28 to 29 are used to prompt user
//        dataSource.setUser("root");
//        dataSource.setPassword();

        try (Connection connection = dataSource.getConnection()) {
            JOptionPane.showMessageDialog(null, "Success! Connection made to 'goated' database.");
            
//            Already stored these 2 sample users in database
//            User user1 = new User("s100201@student.fop", "Foo Bar", "pw-stud#1");   
//            User user2 = new User("s100202@student.fop", "John Doe", "pw-stud#2");

//            insertUser(user1, connection);
//            insertUser(user2, connection);

        
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "Connection failed:\n" + e.getMessage());
        }
        
        
    }
    
    public static void insertUser(User user , Connection connection) {
        try { 
            // Validate input
            if (user.getEmailAddress() == null || user.getEmailAddress().isEmpty() || 
                user.getDisplayName() == null || user.getDisplayName().isEmpty() || 
                user.getPassword() == null || user.getPassword().isEmpty()) {
                JOptionPane.showMessageDialog(null, "All fields are required!");
                return;
            }
            
            // Basic email validation
            if (!user.getEmailAddress().contains("@") || !user.getEmailAddress().contains(".")) {
                JOptionPane.showMessageDialog(null, "Please enter a valid email address!");
                return;
            }
            
            // SQL INSERT statement with placeholders
            String insertSQL = "INSERT INTO user (`Email Address`,  `Password`, `Display Name`) VALUES (?, ?, ?)";
            
            try (PreparedStatement preparedStatement = connection.prepareStatement(insertSQL)) {
                // Set parameters
                preparedStatement.setString(1, user.getEmailAddress());
                preparedStatement.setString(2, user.getPassword());
                preparedStatement.setString(3, user.getDisplayName());
                
                // Execute the insert
                int rowsAffected = preparedStatement.executeUpdate();
                
                if (rowsAffected > 0) {
                    JOptionPane.showMessageDialog(null, "User registered successfully!\n" +
                        "Email: " + user.getEmailAddress() + "\n" +
                        "Display Name: " + user.getDisplayName());
                } else {
                    JOptionPane.showMessageDialog(null, "Failed to register user!");
                }
                
            }
            
        } catch (SQLException e) {
            // Handle specific SQL errors
            if (e.getErrorCode() == 1062) { // MySQL duplicate entry error code
                JOptionPane.showMessageDialog(null, "Email already exists! Please use a different email.");
            } else {
                JOptionPane.showMessageDialog(null, "Database error:\n" + e.getMessage());
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Unexpected error:\n" + e.getMessage());
        }
    }
}
