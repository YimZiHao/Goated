package com.mycompany.smartjournaling;

public class UserSession {
    private static String currentUserEmail;

    public static void setCurrentUser(String email) {
        currentUserEmail = email;
    }

    public static String getCurrentUser() {
        return currentUserEmail;
    }
}