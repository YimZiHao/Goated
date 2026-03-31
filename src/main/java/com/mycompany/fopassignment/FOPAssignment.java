/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 */

package com.mycompany.fopassignment;

/**
 *
 * @author Yim Zi Hao
 */
public class FOPAssignment {

    public static void main(String[] args) {
        User user1 = new User("s100201@student.fop", "Foo Bar", "pw-Stud#1");
        User user2 = new User("s100202@student.fop", "John Doe", "pw-Stud#2");
        System.out.println(user1.getDisplayName());
        System.out.println(user2.getDisplayName());
        System.out.println(user1.toString());
        System.out.println(user2.toString());
    }
}
