/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.fopassignment;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Scanner;

/**
 *
 * @author Yim Zi Hao
 */
public class Journal {
    public static void main(String[] args) {
        LocalDate today = LocalDate.now();
        LocalDate twentyFourthNovember = LocalDate.of(2025, 11, 24);
        
        User user = new User("test@gmail.com", "test", "1234");
//        System.out.print("Press Enter to go back.\n>");
    }
    
    public static void createJournal(User user, LocalDate date){
        Scanner scanner = new Scanner(System.in);
        String fileName = "Journal Entries\\%s\\%s.txt".formatted(user.getDisplayName(), date);
        try {
            PrintWriter outputStream = new PrintWriter(new FileOutputStream(fileName));
            
            System.out.printf("Enter your journal entry for %s:\n> ", date);
            String entry = scanner.nextLine();
            outputStream.print(entry);
            
            outputStream.close();
        } catch(IOException e){
            System.out.println("Something went wrong with creating journals");
        }
    }
    
    public static void viewJournal(User user, LocalDate date){
        String fileName = "Journal Entries\\%s\\%s.txt".formatted(user.getDisplayName(), date);
        try {
            System.out.printf("=== Journal Entry for %s ===\n", date);
            BufferedReader inputStream = new BufferedReader(new FileReader(fileName));
            String line;
            while ((line =inputStream.readLine()) != null){
                System.out.println(line);
            }
            inputStream.close();
        } catch (FileNotFoundException e){
            System.out.println("File not found.");
        } catch (IOException e){
            System.out.println("Something went wrong with viewing journals.");
        }
    }
    
    
    public static void editJournal(User user, LocalDate date){
        Scanner scanner = new Scanner(System.in);
        String fileName = "Journal Entries\\%s\\%s.txt".formatted(user.getDisplayName(), date);
        try {
            System.out.printf("Edit your journal entry for %s:\n>", date);
            PrintWriter outputStream = new PrintWriter(new FileOutputStream(fileName));
            
            System.out.printf("Enter your journal entry for %s:\n> ", date);
            String entry = scanner.nextLine();
            outputStream.print(entry);
            
            outputStream.close();
        } catch(IOException e){
            System.out.println("Something went wrong with editing journals");
        }
    }
    
    public static void menu(User user){
        Scanner scanner = new Scanner(System.in);
        boolean inputIncorrect = true;
        while(inputIncorrect){
            System.out.println("""
                           Would you like to: 
                           1. View Journal
                           2. Edit Journal
                           3. Back to Dates
                           
                           >
                           """);
            int result = scanner.nextInt();
            switch(result){
                case 1, 2, 3 -> {
                    Date(user);
                    inputIncorrect =false;
                }
                default -> {
                    System.out.println("Please enter correct action number");
                }
            }
        }
    }
    
    public static String Date(User user){
        String[] journalEntries = user.getJournals();
        Scanner scanner = new Scanner(System.in);
        LocalDate today = LocalDate.now();
        
        System.out.print("=== Journal Dates === ");
        int i = 0;
        while (i < journalEntries.length){
            if(journalEntries[i].equals(today.toString())){
                System.out.printf("&d. %s (Today)\n", (i+ 1), journalEntries[i]);
                continue;
            }
            System.out.printf("&d. %s\n", (i+ 1), journalEntries[i]);
            i++;
        }
        if(journalEntries[journalEntries.length - 1].equals(today.toString())){
                System.out.printf("&d. %s (Today)\n", (i+ 1), today.toString());
        }
        
        System.out.print("Select a date to view journal, or create a new journal for today:\n>");
        int date = scanner.nextInt() - 1;
        
        return journalEntries[date];
    }
}
