package fat32;

import filesystem.DirectoryEntry;
import filesystem.DirectoryParser;
import masterBootRecord.MasterBootRecord;
import masterBootRecord.PartitionEntry;
import filesystem.Partition;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class Fat32Reader {
    public RandomAccessFile disk; // File handle for the opened disk image
    public List<Partition> partitions; // List of detected partitions on the disk
    public String[] imagePath; // Stores the split path of the disk image
    private Set<String> recoveredFiles; // Tracks already recovered files to avoid duplicates

    // Opens a disk image and parses its partitions
    public void open(String imagePath) throws IOException {
        // Validates the input path
        if (imagePath == null || imagePath.isEmpty()) throw new IOException("Invalid Path");

        // Split path into components
        this.imagePath = imagePath.split("\\\\");

        // Opens the disk image in read-only mode
        this.disk = new RandomAccessFile(imagePath, "r");

        // Seeks to the beginning of the disk
        disk.seek(0);

        // Reads the Master Boot Record bytes
        byte[] mbrBytes = new byte[(512)];
        disk.readFully(mbrBytes);

        // Parses the MBR structure
        MasterBootRecord masterBootRecord = new MasterBootRecord(disk);

        // Initializes the partition list
        this.partitions = new ArrayList<>();

        // Iterates through all MBR partition entries
        for (PartitionEntry partitionEntry : masterBootRecord.partitionEntries) {
            // Skips empty partitions
            if (partitionEntry.size == 0) continue;

            // Creates a Partition object for valid entries
            this.partitions.add(new Partition(disk, partitionEntry.startLBA, partitionEntry.size));
        }

        // Initialize recovered files tracker
        this.recoveredFiles = new HashSet<>();
    }

    // Collects all deleted files and directories from all partitions
    public List<DirectoryEntry> getAllDeletedFilesAndDirectories() {
        List<DirectoryEntry> deletedEntries = new ArrayList<>();

        // Iterates through all partitions
        for (Partition partition : partitions) {
            for (DirectoryEntry directoryEntry : partition.directoryEntries) {
                // Adds entry if it is marked as deleted
                if (directoryEntry.isDeleted) deletedEntries.add(directoryEntry);
            }
        }

        return deletedEntries;
    }

    // Searches for directories by name and returns their contents
    public Map<Integer, List<DirectoryEntry>> getAllFilesAndDirectoriesFromDirectory(String name) throws IOException {
        // Stores clusters of matching directory names
        List<Long> clusters = new ArrayList<>();

        // Stores search results indexed by occurrence
        Map<Integer, List<DirectoryEntry>> searched = new HashMap<>();

        // Iterates through all partitions
        for (Partition partition : partitions) {
            // Iterates through all directory entries
            for (DirectoryEntry directoryEntry : partition.directoryEntries) {
                // Checks for matching directory names
                if (directoryEntry.isDirectory && directoryEntry.fileName.equals(name)) {
                    // Stores the starting cluster of matching directories
                    clusters.add(directoryEntry.startCluster);
                }
            }

            // Reads each found directory cluster
            for (int i = 0; i < clusters.size(); i++) {
                // Reads directory contents and stores them in the result map
                searched.put(i, partition.readDirectory(clusters.get(i), 0, this.disk));
            }
        }

        return searched;
    }

    // Recursively recovers deleted files or directories by name
    public void recoverAllDeletedFileOrDirectory(String name, Path path) throws IOException {
        if (recoveredFiles.contains(name)) return; // Skip if already recovered
        recoveredFiles.add(name); // Mark as recovered

        Path savePath = path; // Store base save path
        Files.createDirectories(path); // Ensure output directory exists

        for (Partition partition : this.partitions) { // Iterate through partitions
            for (DirectoryEntry entry : partition.directoryEntries) { // Iterates through all directory entries
                if (entry.fileName.equals(name) && entry.isDeleted) { // Match deleted entry by name
                    if (entry.isDirectory) {
                        // Build subdirectory path
                        Path subDirectoryPath = Paths.get(path.toString(), entry.fileName);
                        Files.createDirectories(subDirectoryPath); // Create subdirectory

                        // Read contents of directory
                        List<DirectoryEntry> subEntries = DirectoryParser.readDirectory(entry.startCluster, 0,
                                partition.bootSector, partition.startByte, partition.endByte, disk, new HashSet<>());

                        for (DirectoryEntry subEntry : subEntries) {
                            // Recursively recover
                            recoverAllDeletedFileOrDirectory(subEntry.fileName, subDirectoryPath);
                        }

                        // Log success
                        System.out.println("Directory: " + entry.fileName + " successfully restored.");
                        System.out.println("The Directory is stored in " + savePath); // Print location
                    }
                    else { // If entry is a file
                        Path filePath  = Paths.get(path.toString(), entry.fileName); // Build file path

                        File restoredFile = new File(filePath.toUri()); // Create file object

                        try {
                            if (!restoredFile.exists()) { // Check if file already exists
                                byte[] data = DirectoryParser.recoverFile(entry, partition.bootSector,
                                        partition.startByte, partition.endByte, disk); // Recover file data

                                if (data.length == 0) continue; // Skip empty data

                                Files.write(filePath, data); // Write recovered data to file

                                // Log success
                                System.out.println("File: " + entry.fileName + " successfully restored.");
                                System.out.println("The File is stored in " + savePath); // Print location
                            } else {
                                System.out.println("File already exists please delete the old one first and than "
                                        + "try again."); // Warn if file exists
                            }
                        } catch (IOException e) {
                            // Handle I/O errors
                            e.printStackTrace();
                            System.out.println("I/O-Error during creating the File.");
                            System.out.println(e.getClass().getName() + ": " + e.getMessage());
                        }
                    }
                }
            }
        }
    }
}