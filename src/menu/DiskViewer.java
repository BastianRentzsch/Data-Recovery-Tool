package menu;

import fat32.Fat32Reader;
import filesystem.DirectoryEntry;
import filesystem.Partition;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

public class DiskViewer {
    // Opens a disk image or drive file
    public static boolean openDriveOrImage(Scanner scanner,
                                           Fat32Reader fat32Reader) throws IOException {
        String imagePath;

        // Input loop for valid path
        do {
            System.out.print("Please enter the Path to the Image you want to open or 'quit' to exit: ");
            imagePath = scanner.nextLine().trim();

            // Checks non-empty input
            if (!imagePath.isEmpty()) {
                // Accepts .img files
                if (imagePath.endsWith(".img")) break;
                    // Exits program if requested
                else if (imagePath.equalsIgnoreCase("quit")) return true;

                // Invalid input message
                System.out.println("Please enter a path to an image file that ends with '.img' or 'quit'.");
            }
        } while (true);

        // Opens the selected image
        fat32Reader.open(imagePath);
        return false;
    }

    // Displays boot sector information for all partitions
    public static void showBootSectorInfos(Fat32Reader fat32Reader) throws IOException {
        System.out.println("\u001b[34m=== Show Boot Sector Infos ===\u001b[0m");

        // Index counter for partitions
        int index = 1;

        for (Partition partition : fat32Reader.partitions) {
            partition.bootSector.printInfo(index);
            index++;
        }
    }

    // Displays all files and directories
    public static void showAllFilesAndDirectories(Fat32Reader fat32Reader,
                                                   Scanner scanner) throws IOException {
        System.out.println("\u001b[34m=== all Files and Directories ===\u001b[0m");

        for (Partition partition : fat32Reader.partitions) {
            // Skips empty partitions
            if (partition.directoryEntries.isEmpty()) {
                System.out.println("No Files and Directories could be found.");
                continue;
            }

            // Prints entries in chunks of 25
            for (int i = 0; i < partition.directoryEntries.size(); i += 25) {
                // Prints one chunk
                for (int j = i; j < i + 25 && j < partition.directoryEntries.size(); j++) {
                    System.out.println(partition.directoryEntries.get(j));
                }

                // Pauses output if more entries exist
                if (i + 25 < partition.directoryEntries.size()) {
                    // Question if user wants to stop, only the input "Y" or "y" matter, else it continues
                    System.out.print("Do you want to stop [ Y ]: ");
                    String choice = scanner.nextLine().trim();

                    if (choice.equalsIgnoreCase("y")) return;
                }
            }
        }
    }

    // Displays all deleted files and directories
    public static void showAllDeletedFilesAndDirectories(Fat32Reader fat32Reader,
                                                          Scanner scanner) throws IOException {
        System.out.println("\u001b[34m=== all deleted Files and Directories ===\u001b[0m");

        // Gets all deleted entries
        List<DirectoryEntry> deleted = fat32Reader.getAllDeletedFilesAndDirectories();

        if (deleted.isEmpty()) {
            System.out.println("No deleted Files and Directories could be found.");
            return;
        }

        // Prints entries in chunks of 25
        for (int i = 0; i < deleted.size(); i+= 25) {
            // Prints one chunk
            for (int j = i; j < i + 25 && j < deleted.size(); j++) {
                System.out.println(deleted.get(j));
            }

            // Pauses output if more entries exist
            if (i + 25 < deleted.size()) {
                // Question if user wants to stop, only the input "Y" or "y" matter, else it continues
                System.out.print("Do you want to stop [ Y ]: ");
                String choice = scanner.nextLine().trim();

                if (choice.equalsIgnoreCase("y")) return;
            }
        }
    }

    // Searches for directories by name and lists their contents
    public static void showAllFilesAndDirectoriesFromDirectory(Fat32Reader fat32Reader,
                                                                Scanner scanner) throws IOException {
        System.out.println("\u001b[34m=== Search for Directories by name ===\u001b[0m");

        // Reads directory name from user
        System.out.print("Please enter the name of the Directory you want to search: ");
        String directoryName = scanner.nextLine().trim();

        // Gets search results mapped by index
        Map<Integer, List<DirectoryEntry>> searched = fat32Reader.getAllFilesAndDirectoriesFromDirectory(directoryName);

        // Checks if results exist
        if (searched.isEmpty()) {
            System.out.println("no directories with the name found.");
            return;
        }

        System.out.println("\u001b[34m=== all Files and Directories in Directories with the " + directoryName
                + " ===\u001b[0m");

        // Iterates through search results
        loopSearch:for (int index : searched.keySet()) {
            // Prints result index header
            System.out.println("\u001b[38;2;145;231;255m" + (index + 1) + ". Search result \u001b[0m" );

            // Gets directory entries for this result
            List<DirectoryEntry> search = searched.get(index);

            // Prints entries in chunks of 25
            for (int i = 0; i < search.size(); i += 25) {
                // Prints one chunk
                for (int j = i; j < i + 25 && j < search.size(); j++) {
                    System.out.println(search.get(j));
                }

                // Pauses output if more entries exist
                if (i + 25 < search.size()) {
                    // Question if user wants to stop, only the input "Y" or "y" matter, else it continues
                    System.out.print("Do you want to stop [ Y ]: ");
                    String choice = scanner.nextLine().trim();

                    if (choice.equalsIgnoreCase("y")) break loopSearch;
                }
            }
        }
    }

    // Gets a name from the user and restores the corresponding deleted file or directory
    public static void restoreDeletedFileOrDirectory(Fat32Reader fat32Reader,
                                                      Scanner scanner) throws IOException {
        String restoreName; // Stores the name of the file or directory to restore

        while (true) { // Loop until a valid name is entered
            System.out.print("Please enter the name of the File or Directory you want to restore: ");
            restoreName = scanner.nextLine().trim();

            if (!restoreName.isEmpty()) break; // Exit loop if input is not empty

            System.out.println("Please enter a name.");
        }

        String username = System.getProperty("user.name"); // Get current system username
        // Extract image filename without extension
        String filename = fat32Reader.imagePath[fat32Reader.imagePath.length - 1].replace(".img", "");
        Path path = Paths.get("C:/Users/" + username + "/DataPhoenix/" + filename); // Build output directory path

        fat32Reader.recoverAllDeletedFileOrDirectory(restoreName, path); // Trigger recovery process for the given name
    }
}