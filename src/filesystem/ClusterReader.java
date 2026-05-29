package filesystem;

import utilities.LittleEndianParser;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * ClusterReader provides static utility methods for reading and processing
 * clusters in FAT32 filesystems.
 *
 * <p>This class handles:<br>
 * - Reading complete clusters from disk images at calculated byte offsets<br>
 * - Navigating the FAT table to find the next cluster in a chain<br>
 * - Parsing 32-byte directory entries from cluster data<br>
 * - Decoding long UTF-16 filenames from multiple directory entries<br>
 * - Detecting deleted entries (first byte = 0xE5) and directories (attribute 0x10)</p>
 *
 * <p>All methods are static as this is a utility class with no instance state.</p>
 *
 * @author Bastian Rentzsch
 * @version 1.0
 */
public class ClusterReader {
    /**
     * Parses all directory entries inside a cluster's data.
     *
     * <p>Iterates through the cluster data in 32-byte increments:<br>
     * 1. Checks first byte for entry status (0x00 = unused, 0xE5 = deleted)<br>
     * 2. Detects long filename entries (attribute = 0x0F) and accumulates name parts<br>
     * 3. For regular entries, combines long filename parts or uses 8.3 short name<br>
     * 4. Restores first character for deleted entries (0xE5 → '_')<br>
     * 5. Reads startCluster from high (offset 20) and low (offset 26) 16-bit values<br>
     * 6. Reads fileSize from offset 28<br>
     * 7. Creates DirectoryEntry objects and adds them to the entries list<br>
     * 8. Recursively processes subdirectories if isDirectory is true</p>
     *
     * @param fileData the byte array containing cluster data
     * @param bootSector the FAT32 boot sector with filesystem parameters
     * @param startByte the starting byte offset of the partition
     * @param endByte the ending byte offset of the partition
     * @param disk the RandomAccessFile handle for the disk image
     * @param entries the list to add parsed DirectoryEntry objects to
     * @param level the directory depth level for formatted output
     * @param visited a set tracking visited clusters to prevent infinite loops
     * @throws IOException if reading subdirectory clusters fails
     */
    public static void processClusterData(byte[] fileData,
                                           BootSector bootSector,
                                           long startByte,
                                           long endByte,
                                           RandomAccessFile disk,
                                           List<DirectoryEntry> entries,
                                           int level,
                                           Set<Long> visited) throws IOException {
        // Stores parts of long file names
        List<String> longFileNameParts = new ArrayList<>();

        for (int i = 0; i < fileData.length; i += 32) {
            // Reads the first byte of the entry
            int firstByte = fileData[i] & 0xFF;

            // Unused entry -> stop parsing this cluster
            if (firstByte == 0x00) break;

            // Reads the attribute byte
            int attribute = fileData[i + 11] & 0xFF;

            // Checks if the entry is a long filename entry
            if (attribute == 0x0F) {
                // Stores part of the long filename
                StringBuilder part = new StringBuilder();

                // Reads UTF-16 filename characters
                LittleEndianParser.readUTF16LEChars(fileData, i + 1, 10, part);   // chars 1-5
                LittleEndianParser.readUTF16LEChars(fileData, i + 14, 12, part);  // chars 6-11
                LittleEndianParser.readUTF16LEChars(fileData, i + 28, 4, part);   // chars 12-13

                // Inserts filename parts in reverse order
                longFileNameParts.add(0, part.toString());

                // Continues with the next entry
                continue;
            }

            // Checks if the entry is deleted
            boolean isDeleted = firstByte == 0xE5;

            // Checks if the entry is a directory
            boolean isDirectory = (attribute & 0x10) != 0;

            // Combines all long filename parts
            String name = String.join("", longFileNameParts);

            // Uses the short 8.3 name if no long name exists
            if (name.isEmpty()) {
                // Reads the short filename
                name = new String(fileData, i, 8, StandardCharsets.US_ASCII).trim();

                // Reads the extension
                String ext = new String(fileData, i + 8, 3, StandardCharsets.US_ASCII).trim();

                // Appends the extension if present
                if (!ext.isEmpty()) name += "." + ext;
            }

            // Clears the long filename buffer
            longFileNameParts.clear();

            // Skip current directory and parent directory
            if (name.equals(".") || name.equals("..")) continue;

            // Restores the first character marker for deleted entries
            if (isDeleted && !name.isEmpty()) name = "_" + name.substring(1);

            // Reads the high 16 bits of the cluster number
            long high = LittleEndianParser.readUInt16LE(fileData, i + 20) & 0xFFFFL;

            // Reads the low 16 bits of the cluster number
            long low  = LittleEndianParser.readUInt16LE(fileData, i + 26) & 0xFFFFL;

            // Combines high and low cluster values
            long startCluster = (high << 16) | low;

            // Reads the file size
            long fileSize = LittleEndianParser.readUInt32LE(fileData, i + 28);

            // Adds the entry to the result list
            entries.add(new filesystem.DirectoryEntry(
                    name, startCluster, fileSize, isDeleted, isDirectory, level
            ));

            // Recursively reads subdirectories
            if (isDirectory && startCluster >= 2 && startCluster < bootSector.clusterCount + 2) {
                entries.addAll(DirectoryParser.readDirectory(startCluster, level + 1, bootSector, startByte,
                        endByte, disk, visited));
            }
        }
    }

    /**
     * Reads a complete cluster from the disk image.
     *
     * <p>Calculates the byte offset using the formula:<br>
     * offset = startByte + ((cluster - 2) × sectorsPerCluster + firstDataSector) × bytesPerSector</p>
     *
     * <p>Cluster numbers start at 2 (clusters 0 and 1 are reserved).
     * Returns an empty byte array if the offset is outside the partition boundaries.</p>
     *
     * @param cluster the cluster number to read (≥ 2)
     * @param bootSector the FAT32 boot sector with filesystem parameters
     * @param startByte the starting byte offset of the partition
     * @param endByte the ending byte offset of the partition
     * @param disk the RandomAccessFile handle for the disk image
     * @return byte array containing the cluster data (size = clusterSize)
     * @throws IOException if reading from disk fails
     */
    public static byte[] readCluster(long cluster,
                                     BootSector bootSector,
                                     long startByte,
                                     long endByte,
                                     RandomAccessFile disk) throws IOException {
        // Calculates the first sector of the cluster
        long firstSectorOfCluster = ((cluster - 2) * bootSector.sectorsPerCluster) + bootSector.firstDataSector;

        // Calculates the byte offset of the cluster
        long offset = startByte + (firstSectorOfCluster * bootSector.bytesPerSector);

        // Stops if the cluster is outside the partition
        if (offset > endByte) return new byte[0];

        disk.seek(offset);
        byte[] data = new byte[bootSector.clusterSize];
        disk.readFully(data);

        return data;
    }

    /**
     * Reads the next cluster number from the FAT table.
     *
     * <p>Calculates the FAT entry offset: fatStartByte + (cluster × 4)
     * since FAT32 entries are 4 bytes each. Reads 4 bytes and combines
     * them into a 32-bit value, then masks with 0x0FFFFFFF to extract
     * the lower 28 bits used by FAT32.</p>
     *
     * <p>FAT32 cluster chain markers:<br>
     * - 0x00000000: Free cluster<br>
     * - 0x00000002 to 0x0FFFFFF6: Valid cluster, continue chain<br>
     * - 0x0FFFFFF7: Bad cluster<br>
     * - 0x0FFFFFF8 to 0x0FFFFFFF: End of chain</p>
     *
     * @param cluster the current cluster number
     * @param bootSector the FAT32 boot sector with filesystem parameters
     * @param startByte the starting byte offset of the partition
     * @param disk the RandomAccessFile handle for the disk image
     * @return the next cluster number, or 0 if the FAT entry is invalid/out of bounds
     * @throws IOException if reading from disk fails
     */
    public static long getNextCluster(long cluster,
                                      BootSector bootSector,
                                      long startByte,
                                      RandomAccessFile disk) throws IOException {
        // Calculates the FAT start offset
        long fatStartByte = startByte + ((long) bootSector.reservedSectorsCount * bootSector.bytesPerSector);

        // Calculates the FAT entry offset (entries are 4 bytes large)
        long offset = fatStartByte + (cluster * 4L);

        // Stops if the FAT entry is outside the disk image
        if (offset + 4 > disk.length()) return 0;

        disk.seek(offset);

        // Reads the FAT entry bytes
        byte[] bytes = new byte[4];
        disk.readFully(bytes);

        // Converts the FAT entry to a 32-bit value
        long value = ((bytes[3] & 0xFFL) << 24) |
                ((bytes[2] & 0xFFL) << 16) |
                ((bytes[1] & 0xFFL) << 8)  |
                (bytes[0] & 0xFFL);

        // FAT32 only uses the lower 28 bits
        return value & 0x0FFFFFFFL;
    }
}