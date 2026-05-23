import filesystem.DirectoryEntry;
import masterBootRecord.MasterBootRecord;
import masterBootRecord.PartitionEntry;
import filesystem.Partition;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.*;

public class Fat32Reader {

    public RandomAccessFile disk; // File handle for the opened disk image
    public List<Partition> partitions; // List of detected partitions on the disk

    // Opens a disk image and parses its partitions
    public void open(String drivePath) throws IOException {

        // Validates the input path
        if (drivePath == null || drivePath.isEmpty()) throw new IOException("Invalid Path");

        // Opens the disk image in read-only mode
        this.disk = new RandomAccessFile(drivePath, "r");

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
}
