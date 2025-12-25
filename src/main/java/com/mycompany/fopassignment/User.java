package com.mycompany.fopassignment;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class User {

    private String emailAddress;
    private String password;      
    private String displayName;
    private String[] journals;

    public User(String emailAddress, String displayName, String encryptedPassword) {
        this.emailAddress = emailAddress;
        this.displayName = displayName;
        this.password = encryptedPassword;
    }

    // STATIC encryption method
    public static String cipher(String password) {
        StringBuilder cipherText = new StringBuilder();
        char character;

        for (int i = 0; i < password.length(); i++) {
            character = password.charAt(i);
            if (character >= 32 && character <= 126) {
                character = (char) (((character - 32 + 55) % 95 + 95) % 95 + 32);
            }
            cipherText.append(character);
        }
        return cipherText.toString();
    }

    public String getEmailAddress() {
        return emailAddress;
    }

    public String getPassword() {
        return password; // already encrypted
    }

    public String getDisplayName() {
        return displayName;
    }

    public String[] getJournals() {
        try (BufferedReader inputStream =
                     new BufferedReader(new FileReader(
                             "Journal Entries\\" + displayName + "\\Dates.txt"))) {

            String line = inputStream.readLine();
            if (line != null) {
                journals = line.split(", ");
            }
        } catch (IOException e) {
            System.out.println("Error reading journal dates.");
        }
        return journals;
    }
}
