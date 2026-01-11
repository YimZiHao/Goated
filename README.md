This is the GitHub repository for WIX1002 FOP assignment OCC11 G10 Topic 3: Smart Journaling. 

Before running the smart journal, please ensure MySQL is installed and clone the user database first. 
Steps to clone MySQL database:
In git bash
1. git clone https://github.com/YimZiHao/Goated.git
2. cd Goated
3. git checkout master
4. ⁠mysql -u root -p
5. ⁠CREATE DATABASE smart_journal;
6. ⁠EXIT;
4. mysql -u root -p smart_journal < path/to/goated.sql 
In mysql workshop, check if smart_journal schema exists.

Users have to remember their MySQL root account password as it will be asked of them for the program to run. 

In Netbeans, 
After opening the project, do these few steps:
1. Right click on the project.
2. Click properties.
3. Go to run.
4. Add this as the main class 'com.mycompany.smartjournaling.Launcher'. It will not show up when you choose browse main class.
5. Run the launcher and enjoy the program. :)
