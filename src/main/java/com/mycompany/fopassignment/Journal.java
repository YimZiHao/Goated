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
import java.util.Scanner;

/**
 *
 * @author Yim Zi Hao
 */
public class Journal {
    public static void main(String[] args) {
        LocalDate today = LocalDate.now();
        System.out.println("=== Journal Dates ===");
        
//        System.out.print("Select a date to view journal, or create a new journal for today:\n>");
//        System.out.print("Press Enter to go back.\n>");
//        System.out.println("""
//                           Would you like to: 
//                           1. View Journal
//                           2. Edit Journal
//                           3. Back to Dates
//                           
//                           >
//                           """);
    }
    
    public static void createJournal(String date){
        Scanner scanner = new Scanner(System.in);
        String fileName = "%s.txt".formatted(date);
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
    
    public static void viewJournal(String date){
        String fileName = "%s.txt".formatted(date);
        try {
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
}
