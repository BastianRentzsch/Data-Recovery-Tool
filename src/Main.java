import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

// testData.img
//Bytes per Sector: 512
//Sectors per Cluster: 32
//Reserved Sectors: 36
//FAT Count: 2
//FAT Size: 29326
//Root Cluster: 2

// testData2.img
// ?

public class Main {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        loop:do {
            // H:\Kursmaterial\Java\Data Recovery Tool\src\testdata\testData.img
            // H:\Kursmaterial\Java\Data Recovery Tool\src\testdata\testData2.img

            Fat32Reader fat32Reader = new Fat32Reader();
            try {
                if (openDriveOrImage(scanner, fat32Reader)) break;

                while (true) {
                    System.out.println("\u001b[31m=== FAT32 Data Recovery Tool ===\u001b[0m");
                    System.out.println("1. Change Drive or Image");
                    System.out.println("2. Show Boot Sector Information");
                    System.out.println("3. Show all Files and Directories");
                    System.out.println("4. Show all deleted Files and Directories");
                    System.out.println("5. Show all Files and Directories from a specific Directory");

                    System.out.println("7. Quit");
                    System.out.print("Please enter the number of what you want to do: ");

                    String choice = scanner.nextLine().trim();

                    switch (choice) {
                        case "1" -> openDriveOrImage(scanner, fat32Reader);
                        case "2" -> fat32Reader.bootSector.printInfo();
                        case "3" -> showAllFilesAndDirectories(fat32Reader, scanner);
                        case "4" -> showAllDeletedFilesAndDirectories(fat32Reader, scanner);
                        case "5" -> showAllFilesAndDirectoriesFromDirectory(fat32Reader, scanner);

                        case "7" -> {
                            break loop;
                        }
                        default -> System.out.println("Please enter one of the numbers shown.");
                    }
                }
            } catch (IOException e) {
                System.out.println("File not found.");
            }

        } while (true);

        scanner.close();
    }

    private static void showAllFilesAndDirectories(Fat32Reader fat32Reader, Scanner scanner) throws IOException {
        System.out.println("\u001b[34m=== all Files and Directories ===\u001b[0m");

        // output of directories entries in increments of 25
        loop:for (int i = 0; i < fat32Reader.directoryEntries.size(); i+= 25) {
            for (int j = i; j < i + 25; j++) {
                // loop ending if the list of directories entries is not modulo 25 == 0
                if (j >= fat32Reader.directoryEntries.size()) break loop;
                // output of directories entry's information
                System.out.println(fat32Reader.directoryEntries.get(j));
            }

            // question if user wants to stop, only the input "Y" or "y" matter, else it continues
            System.out.print("Do you want to stop [ Y ]: ");
            String choice = scanner.nextLine().trim();
            if (choice.equalsIgnoreCase("y")) break;
        }
    }

    private static void showAllDeletedFilesAndDirectories(Fat32Reader fat32Reader, Scanner scanner) throws IOException {
        System.out.println("\u001b[34m=== all deleted Files and Directories ===\u001b[0m");

        // get a list of all deleted directory entries
        List<DirectoryEntry> deleted = fat32Reader.getAllDeletedFilesAndDirectories();

        // output of directories entries in increments of 25
        loop:for (int i = 0; i < deleted.size(); i+= 25) {
            for (int j = i; j < i + 25; j++) {
                // loop ending if the list of directories entries is not modulo 25 == 0
                if (j >= deleted.size()) break loop;

                // output of directories entry's information
                System.out.println(deleted.get(j));
            }

            // question if user wants to stop, only the input "Y" or "y" matter, else it continues
            System.out.print("Do you want to stop [ Y ]: ");
            String choice = scanner.nextLine().trim();
            if (choice.equalsIgnoreCase("y")) break;
        }
    }

    private static void showAllFilesAndDirectoriesFromDirectory(Fat32Reader fat32Reader, Scanner scanner) throws IOException {
        System.out.println("\u001b[34m=== Search for Directories by name ===\u001b[0m");

        System.out.print("Please enter the name of the Directory you want to search: ");
        String directoryName = scanner.nextLine().trim();

        // get a list of all deleted directory entries
        Map<Integer, List<DirectoryEntry>> searched = fat32Reader.getAllFilesAndDirectoriesFromDirectory(directoryName);

        if (searched.isEmpty()) {
            System.out.println("no Directories with the name found.");
            return;
        }

        System.out.println("\u001b[34m=== all Files and Directories in Directories with the " + directoryName + " ===\u001b[0m");

        loopSearch:for (int index : searched.keySet()) {
            System.out.println("\u001b[38;2;145;231;255m" + (index + 1) + ". Search result \u001b[0m" );
            List<DirectoryEntry> search = searched.get(index);
            for (int j = 0; j < search.size(); j += 25) {
                for (int k = j; k < j + 25; k++) {
                    // loop ending if the list of directories entries is not modulo 25 == 0
                    if (k >= search.size()) break;

                    // output of directories entry's information
                    System.out.println(search.get(k));
                }

                // question if user wants to stop, only the input "Y" or "y" matter, else it continues
                System.out.print("Do you want to stop [ Y ]: ");
                String choice = scanner.nextLine().trim();
                if (choice.equalsIgnoreCase("y")) break loopSearch;
            }
        }
    }

    private static boolean openDriveOrImage(Scanner scanner, Fat32Reader fat32Reader) throws IOException {
        String drivePath;
        do {
//            // Unter Windows z.B.: "\\\\.\\D:"(als Admin ausführen!)
//            // Unter Linux z.B.: "/dev/sdb1"
//              String drivePath = "\\\\\\\\.\\\\"+ args[0] +":";
            System.out.print("Please enter the Path to the Drive or Image you want to open or 'quit' to exit: ");
            drivePath = scanner.nextLine().trim();

            if (!drivePath.isEmpty()) {
                // Openig Drive
                if (drivePath.endsWith(":")) {
                    drivePath = "\\\\\\\\.\\\\" + drivePath;
                    break;
                }
                // Opening Image file
                else if (drivePath.endsWith(".img")) {
                    break;
                }
                // Exiting programm
                else if (drivePath.equalsIgnoreCase("quit")) {
                    return true;
                }
                System.out.println("Please enter either an Drive name with ':' at the end or a Path to an Image file that ends with '.img'");
            }
        } while (true);

        fat32Reader.open(drivePath);
        return false;
    }
}
