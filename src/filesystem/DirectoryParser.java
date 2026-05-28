package filesystem;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class DirectoryParser {
    // Reads all directory entries starting from a cluster
    public static List<DirectoryEntry> readDirectory(long startCluster,
                                                     int level,
                                                     BootSector bootSector,
                                                     long startByte,
                                                     long endByte,
                                                     RandomAccessFile disk,
                                                     Set<Long> visited) throws IOException {
        // Stores all found directory entries
        List<DirectoryEntry> entries = new ArrayList<>();

        // Prevents infinite loops by checking visited clusters
        if (!visited.add(startCluster)) return entries;

        // Starts with the first cluster of the directory
        long currentCluster = startCluster;

        // Iterates through the cluster chain
        while (currentCluster >= 2 && currentCluster < bootSector.clusterCount + 2) {
            // Reads the current cluster data
            byte[] clusterData = ClusterReader.readCluster(currentCluster, bootSector, startByte, endByte, disk);

            // Parses all directory entries inside the cluster
            ClusterReader.processClusterData(clusterData, bootSector, startByte, endByte, disk, entries, level, visited);

            // Gets the next cluster from the FAT
            long nextCluster = ClusterReader.getNextCluster(currentCluster, bootSector, startByte, disk);

            // Stops if the next cluster is invalid
            if (nextCluster < 2 || nextCluster >= bootSector.clusterCount + 2) break;

            // Continues with the next cluster
            currentCluster = nextCluster;
        }

        // Returns all found entries
        return entries;
    }

    // Recovers a deleted file by reading its clusters sequentially and reconstructing its data into a byte array
    public static byte[] recoverFile(DirectoryEntry directoryEntry,
                                     BootSector bootSector,
                                     long startByte,
                                     long endByte,
                                     RandomAccessFile disk) throws IOException {
        // Skip if entry is a directory or invalid cluster
        if (directoryEntry.isDirectory || directoryEntry.startCluster < 2) return new byte[0];

        // Stores data chunks read from clusters
        List<byte[]> dataChunks = new ArrayList<>();
        // Start from the file's first cluster
        long currentCluster = directoryEntry.startCluster;

        // Calculate expected number of clusters based on file size, rounded up
        int expectedClusters = (int) ((directoryEntry.fileSize + bootSector.clusterSize - 1) / bootSector.clusterSize);

        for (int i = 0; i < expectedClusters; i++) { // Iterate through expected clusters
            // Read cluster data
            byte[] clusterData = ClusterReader.readCluster(currentCluster, bootSector, startByte, endByte, disk);

            if (clusterData.length == 0) break; // Stop if no data could be read

            boolean isEmpty = true; // Flag to detect empty clusters
            for (byte b : clusterData) { // Check if cluster contains non-zero data
                if (b != 0) {
                    isEmpty = false;
                    break;
                }
            }

            if (isEmpty && i > 0) break; // Stop if an empty cluster is found after the first one

            dataChunks.add(clusterData); // Store valid cluster data

            currentCluster++; // Move to next cluster (assumes contiguous allocation)
        }

        // Calculate total available data size
        long totalSize = 0;
        for (byte[] chunk : dataChunks) { // Sum sizes of all collected chunks
            totalSize += chunk.length;
        }

        // Use the smaller of file size or actual data available
        long fileSize = Math.min(directoryEntry.fileSize, totalSize); // Prevent overflow beyond available data
        byte[] result = new byte[(int) fileSize];

        int offset = 0; // Tracks write position in result array
        for (byte[] chunk : dataChunks) { // Copy chunk data into result buffer
            int copyLength = (int) Math.min(chunk.length, fileSize - offset); // Ensure not exceeding file size
            if (copyLength <= 0) break; // Stop if nothing left to copy
            System.arraycopy(chunk, 0, result, offset, copyLength); // Copy bytes into result
            offset += copyLength;
        }

        return result; // Return reconstructed file data
    }
}