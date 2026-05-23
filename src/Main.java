import filesystem.DirectoryEntry;
import filesystem.Partition;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.*;

// testData.img
//Bytes per Sector: 512
//Sectors per Cluster: 32
//Reserved Sectors: 36
//FAT Count: 2
//FAT Size: 29326
//Root Cluster: 2

// testData2.img
// ?

// C:\Users\bastianr\Desktop\test\testData.img
// C:\Users\bastianr\Desktop\test\empty.img

public class Main {

    public static void main(String[] args) {

        Scanner scanner = new Scanner(System.in);

        // Main program loop
        loop:while (true) {

            Fat32Reader fat32Reader = new Fat32Reader();

            try {

                // Opens a drive or image file, exits if requested
                if (openDriveOrImage(scanner, fat32Reader)) break;

                // Menu loop for user actions
                while (true) {

                    // Displays main menu
                    System.out.println("\u001b[31m=== FAT32 Data Recovery Tool ===\u001b[0m");
                    System.out.println("1. Change Drive or Image");
                    System.out.println("2. Show Boot Sector Information");
                    System.out.println("3. Show all Files and Directories");
                    System.out.println("4. Show all deleted Files and Directories");
                    System.out.println("5. Show all Files and Directories from a specific Directory");

                    System.out.println("7. Quit");

                    // Reads user choice
                    System.out.print("Please enter the number of what you want to do: ");
                    String choice = scanner.nextLine().trim();

                    // Executes selected menu option
                    switch (choice) {

                        case "1" -> openDriveOrImage(scanner, fat32Reader);
                        case "2" -> showBootSectorInfos(fat32Reader);
                        case "3" -> showAllFilesAndDirectories(fat32Reader, scanner);
                        case "4" -> showAllDeletedFilesAndDirectories(fat32Reader, scanner);
                        case "5" -> showAllFilesAndDirectoriesFromDirectory(fat32Reader, scanner);

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
                System.out.println("Datei konnte nicht geöffnet werden.");
                System.out.println(e.getClass().getName() + ": " + e.getMessage());

            } catch (IOException e) {

                // Handles general I/O errors
                e.printStackTrace();
                System.out.println("I/O-Fehler beim Lesen.");
                System.out.println(e.getClass().getName() + ": " + e.getMessage());

            }
        }

        scanner.close();
    }

    // Opens a disk image or drive file
    private static boolean openDriveOrImage(Scanner scanner,
                                            Fat32Reader fat32Reader) throws IOException {

        String drivePath;

        // Input loop for valid path
        do {

            System.out.print("Please enter the Path to the Image you want to open or 'quit' to exit: ");
            drivePath = scanner.nextLine().trim();

            // Checks non-empty input
            if (!drivePath.isEmpty()) {

                // Accepts .img files
                if (drivePath.endsWith(".img")) break;

                // Exits program if requested
                else if (drivePath.equalsIgnoreCase("quit")) return true;

                // Invalid input message
                System.out.println("Please enter a path to an image file that ends with '.img' or 'quit'.");

            }

        } while (true);

        // Opens the selected image
        fat32Reader.open(drivePath);

        return false;
    }

    // Displays boot sector information for all partitions
    private static void showBootSectorInfos(Fat32Reader fat32Reader) throws IOException {

        System.out.println("\u001b[34m=== Show Boot Sector Infos ===\u001b[0m");

        // Index counter for partitions
        int index = 1;

        for (Partition partition : fat32Reader.partitions) {

            partition.bootSector.printInfo(index);
            index++;

        }

    }

    // Displays all files and directories
    private static void showAllFilesAndDirectories(Fat32Reader fat32Reader,
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
    private static void showAllDeletedFilesAndDirectories(Fat32Reader fat32Reader,
                                                          Scanner scanner) throws IOException {

        System.out.println("\u001b[34m=== all deleted Files and Directories ===\u001b[0m");

        // Gets all deleted entries
        List<DirectoryEntry> deleted = fat32Reader.getAllDeletedFilesAndDirectories();

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
    private static void showAllFilesAndDirectoriesFromDirectory(Fat32Reader fat32Reader,
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

}
