/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.fopassignment;

/**
 *
 * @author Yim Zi Hao
 */
public class User {
    
    private String emailAddress, password, displayName;

    User(String emailAddress, String displayName, String password) {
        this.emailAddress = emailAddress;
        this.displayName = displayName;
        this.password = password;
    }

    @Override
    public String toString() {
        return "User{" + "emailAddress=" + emailAddress + ", password=" + password + ", displayName=" + displayName + '}';
    }

    

    String getDisplayName() {
        return displayName;
    }
    
    
}
