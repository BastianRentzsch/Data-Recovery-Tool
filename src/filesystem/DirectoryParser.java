package filesystem;

import utilities.LittleEndianParser;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
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
        if (!visited.add(startCluster)) {
            return entries;
        }

        // Starts with the first cluster of the directory
        long currentCluster = startCluster;

        // Iterates through the cluster chain
        while (currentCluster >= 2 && currentCluster < bootSector.clusterCount + 2) {

            // Reads the current cluster data
            byte[] clusterData = readCluster(currentCluster, bootSector, startByte, endByte, disk);

            // Parses all directory entries inside the cluster
            processClusterData(clusterData, bootSector, startByte, endByte, disk, entries, level, visited);

            // Gets the next cluster from the FAT
            long nextCluster = getNextCluster(currentCluster, bootSector, startByte, disk);

            // Stops if the next cluster is invalid
            if (nextCluster < 2 || nextCluster >= bootSector.clusterCount + 2) break;

            // Continues with the next cluster
            currentCluster = nextCluster;
        }

        // Returns all found entries
        return entries;
    }

    // Parses all directory entries inside a cluster
    private static void processClusterData(byte[] fileData,
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
            if (isDirectory && !isDeleted && startCluster >= 2 && startCluster < bootSector.clusterCount + 2) {
                entries.addAll(readDirectory(startCluster, level + 1, bootSector, startByte, endByte, disk, visited));
            }
        }
    }

    // Reads a complete cluster from the disk image
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

    // Reads the next cluster number from the FAT
    public static long getNextCluster(long cluster,
                                      BootSector bootSector,
                                      long startByte,
                                      RandomAccessFile disk) throws IOException {

        // Calculates the FAT start offset
        long fatStartByte = startByte + ((long) bootSector.reservedSectorsCount * bootSector.bytesPerSector);

        // Calculates the FAT entry offset
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


    public static byte[] recoverFile(DirectoryEntry directoryEntry,
                                     BootSector bootSector,
                                     long startByte,
                                     long endByte,
                                     RandomAccessFile disk) throws IOException {

//        if (!directoryEntry.isDirectory || directoryEntry.startCluster < 2) return new byte[0];

        List<byte[]> chunks = new ArrayList<>();
        long currentCluster = directoryEntry.startCluster;

        while (currentCluster >= 2 && currentCluster < bootSector.clusterCount + 2) {
            byte[] clusterData = readCluster(currentCluster, bootSector, startByte, endByte, disk);

            if (clusterData.length == 0) break;

            chunks.add(clusterData);

            long nextCluster = getNextCluster(currentCluster, bootSector, startByte, disk);

            if (nextCluster < 2 || nextCluster >= bootSector.clusterCount + 2) break;
            currentCluster = nextCluster;
        }

        long totalSize = 0;
        for (byte[] chunk : chunks) {
            totalSize += chunk.length;
        }

        long fileSize = Math.min(directoryEntry.fileSize, totalSize);
        byte[] result = new byte[(int) fileSize];

        int offset = 0;
        for (byte[] chunk : chunks) {
            int copyLength = (int) Math.min(chunk.length, fileSize - offset);
            if (copyLength <= 0) break;
            System.arraycopy(chunk, 0, result, offset, copyLength);
            offset += copyLength;
        }

        return result;
    }


}
