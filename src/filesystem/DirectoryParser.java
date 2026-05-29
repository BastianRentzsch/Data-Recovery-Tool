package filesystem;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * DirectoryParser provides static utility methods for parsing directory structures
 * and recovering deleted files in FAT32 filesystems.
 *
 * <p>This class contains the core algorithms for:<br>
 * - Recursively reading directory entries from a cluster chain<br>
 * - Detecting and preventing infinite loops with visited cluster tracking<br>
 * - Reconstructing deleted file data by reading cluster sequences<br>
 * - Handling both short 8.3 filenames and long UTF-16 filenames</p>
 *
 * <p>All methods are static as this is a utility class with no instance state.</p>
 *
 * @author Bastian Rentzsch
 * @version 1.0
 */
public class DirectoryParser {
    /**
     * Reads all directory entries starting from a given cluster.
     *
     * <p>Traverses the cluster chain by repeatedly calling ClusterReader.getNextCluster()
     * until reaching an end-of-chain marker (≥ 0x0FFFFFF8). For each cluster:<br>
     * 1. Reads the cluster data with ClusterReader.readCluster()<br>
     * 2. Parses directory entries with ClusterReader.processClusterData()<br>
     * 3. Recursively processes subdirectories found in the entries</p>
     *
     * <p>Uses a Set&lt;Long&gt; visited to prevent infinite loops from corrupted
     * cluster chains that contain cycles.</p>
     *
     * @param startCluster the starting cluster number of the directory
     * @param level the directory depth level for formatted output
     * @param bootSector the FAT32 boot sector with filesystem parameters
     * @param startByte the starting byte offset of the partition
     * @param endByte the ending byte offset of the partition
     * @param disk the RandomAccessFile handle for the disk image
     * @param visited a set tracking already-visited cluster numbers to prevent loops
     * @return a list of all DirectoryEntry objects found in the directory chain
     * @throws IOException if reading cluster data fails
     */
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

        return entries;
    }

    /**
     * Recovers a deleted file by reading its clusters sequentially.
     *
     * <p>Reconstructs file data by:<br>
     * 1. Calculating expectedClusters = ceil(fileSize / clusterSize)<br>
     * 2. Reading each cluster sequentially (assumes contiguous allocation)<br>
     * 3. Stopping at empty clusters after the first non-empty cluster<br>
     * 4. Combining all cluster data into a single byte array<br>
     * 5. Truncating to the actual fileSize to avoid overflow</p>
     *
     * <p>This method assumes contiguous cluster allocation which works for
     * most deleted files but may not recover fragmented files completely.</p>
     *
     * @param directoryEntry the DirectoryEntry containing the deleted file's metadata
     * @param bootSector the FAT32 boot sector with filesystem parameters
     * @param startByte the starting byte offset of the partition
     * @param endByte the ending byte offset of the partition
     * @param disk the RandomAccessFile handle for the disk image
     * @return byte array containing the recovered file data, or empty array if recovery fails
     * @throws IOException if reading cluster data fails
     */
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