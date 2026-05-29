package main;

import fat32.Fat32Reader;
import menu.DiskViewer;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.*;

/**
 * Main class serving as the entry point for the FAT32 Data Recovery Tool.
 *
 * <p>Implements the main program loop with a menu-driven interface offering
 * seven options:<br>
 * 1. Change Image - Open a different disk image<br>
 * 2. Show Boot Sector Information - Display FAT32 parameters<br>
 * 3. Show all Files and Directories - List all entries<br>
 * 4. Show all deleted Files and Directories - List deleted entries<br>
 * 5. Show Files/Directories from specific Directory - Search by name<br>
 * 6. Restore deleted File or Directory - Recover by name<br>
 * 7. Quit - Exit the program</p>
 *
 * <p>Handles exceptions gracefully:<br>
 * - FileNotFoundException: Shows user-friendly message for missing images<br>
 * - IOException: Prints stack trace and error details for debugging</p>
 *
 * @author Bastian Rentzsch
 * @version 1.0
 */
public class Main {
    /**
     * Main entry point of the FAT32 Data Recovery Tool.
     *
     * <p>Creates a Scanner for console input and enters the main program loop:<br>
     * 1. Creates a new Fat32Reader instance<br>
     * 2. Opens a disk image via DiskViewer.openDriveOrImage()<br>
     * 3. Enters the menu loop until user selects "Quit" (option 7)<br>
     * 4. Handles exceptions and displays error messages<br>
     * 5. Closes the Scanner before exiting</p>
     *
     * @param args command line arguments (not used)
     */
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        // main program loop
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