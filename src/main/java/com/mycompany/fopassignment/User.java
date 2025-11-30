/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.fopassignment;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

/**
 *
 * @author Yim Zi Hao
 */
public class User {
    
    private String emailAddress, password, displayName;
    private String[] journals;

    User(String emailAddress, String displayName, String password) {
        this.emailAddress = emailAddress;
        this.displayName = displayName;
        this.password = cipher(password);
    }

    @Override
    public String toString() {
        return "User{" + "emailAddress=" + emailAddress + ", password=" + password + ", displayName=" + displayName + '}';
    }
    
    public String cipher(String password){
        StringBuilder cipherText = new StringBuilder(); //using StringBuilder to append characters to ciphered password
        char character;
        for(int i = 0; i < password.length(); i++){
            character = password.charAt(i);
            if (character >= 32 && character <= 126){ 
                //32 to 126 are the minimum to maximum printable ASCII characters (space to tilde ~)
                character = (char) (((character - 32 + 55) % 95 + 95) % 95 + 32); 
                //shift is 55, range is 95, %95 + 95 % 95 to make sure negative value is not returned
                cipherText.append(character);
            } else {
                cipherText.append(character);
            }
        }
        return cipherText.toString();
    }
        
    public String getDisplayName() {
        return displayName;
    }

    public String getEmailAddress() {
        return emailAddress;
    }

    public String getPassword() {
        return password;
    }

    public String[] getJournals() {
        try {
            BufferedReader inputStream = new BufferedReader(new FileReader("Journal Entries\\%s\\Dates.txt".formatted(displayName)));
            String line;
            while ((line = inputStream.readLine()) != null){
                String [] dates = line.split(", ");
                journals = dates;
            }
        } catch (FileNotFoundException e){
            System.out.println("File not found");
        } catch (IOException e){
            System.out.println("Something went wrong with reading date file");
        }
        return journals;
    }
}
