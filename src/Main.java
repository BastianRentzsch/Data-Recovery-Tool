import fat32.Fat32Reader;
import menu.DiskViewer;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.*;

public class Main {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        // Main program loop
        loop:while (true) {
            Fat32Reader fat32Reader = new Fat32Reader();

            try {
                // Opens a drive or image file, exits if requested
                if (DiskViewer.openDriveOrImage(scanner, fat32Reader)) break;

                // Menu loop for user actions
                while (true) {
                    // Displays main menu
                    System.out.println("\u001b[31m=== FAT32 Data Recovery Tool ===\u001b[0m");
                    System.out.println("1. Change Image");
                    System.out.println("2. Show Boot Sector Information");
                    System.out.println("3. Show all Files and Directories");
                    System.out.println("4. Show all deleted Files and Directories");
                    System.out.println("5. Show all Files and Directories from a specific Directory");
                    System.out.println("6. Restore deleted File or Directory");
                    System.out.println("7. Quit");

                    // Reads user choice
                    System.out.print("Please enter the number of what you want to do: ");
                    String choice = scanner.nextLine().trim();

                    // Executes selected menu option
                    switch (choice) {
                        case "1" -> DiskViewer.openDriveOrImage(scanner, fat32Reader);
                        case "2" -> DiskViewer.showBootSectorInfos(fat32Reader);
                        case "3" -> DiskViewer.showAllFilesAndDirectories(fat32Reader, scanner);
                        case "4" -> DiskViewer.showAllDeletedFilesAndDirectories(fat32Reader, scanner);
                        case "5" -> DiskViewer.showAllFilesAndDirectoriesFromDirectory(fat32Reader, scanner);
                        case "6" -> DiskViewer.restoreDeletedFileOrDirectory(fat32Reader, scanner);
                        // Exits the program loop
                        case "7" -> {
                            break loop;
                        }
                        // Handles invalid input
                        default -> System.out.println("Please enter one of the numbers shown.");
                    }
                }
            } catch (FileNotFoundException e) {
                // Handles missing file errors
                System.out.println("Image File could not be opened.");
                System.out.println(e.getClass().getName() + ": " + e.getMessage());
            } catch (IOException e) {
                // Handles general I/O errors
                e.printStackTrace();
                System.out.println("I/O-Error during reading.");
                System.out.println(e.getClass().getName() + ": " + e.getMessage());
            }
        }

        scanner.close();
    }
}