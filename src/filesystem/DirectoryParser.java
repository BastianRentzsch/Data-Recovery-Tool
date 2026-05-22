package filesystem;

import partitionTable.BootSector;
import utilities.Fat32Util;
import utilities.LittleEndianParser;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class DirectoryParser {
    public static List<DirectoryEntry> readDirectory(long cluster, BootSector bootSector, long startByte, RandomAccessFile disk)
            throws IOException {
        // Cluster can not be lower than the root directory cluster
        if (cluster < bootSector.rootCluster) return new ArrayList<>();

        return new ArrayList<>(readDirectory(cluster, 0, bootSector, startByte, disk));
    }

    public static List<filesystem.DirectoryEntry> readDirectory(long cluster, int level, BootSector bootSector,
                                                                long startByte, RandomAccessFile disk)
            throws IOException {
        List<filesystem.DirectoryEntry> entries = new ArrayList<>();

        if (cluster < 2 || cluster >= bootSector.clusterCount + 2) return entries;

        // Get all clusters that belong together with that cluster
        List<byte[]> clusters = readClusterChain(cluster, bootSector, startByte, disk);

        for (byte[] fileData : clusters) {
            List<String> longFileNameParts = new ArrayList<>();

            for (int i = 0; i < fileData.length; i += 32) {
                int firstByte = fileData[i] & 0xFF;

                // Unused entry
                if (firstByte == 0x00) break;

                // For looking if it is a directory and if it has a long name
                int attribute = fileData[i + 11] & 0xFF;

                // Has entry a long Filename
                if (attribute == 0x0F) {
                    StringBuilder part = new StringBuilder();
                    // Chars 1-5
                    LittleEndianParser.readUTF16LEChars(fileData, i + 1, 10, part);
                    // Chars 6-11
                    LittleEndianParser.readUTF16LEChars(fileData, i + 14, 12, part);
                    // Chars 12-13
                    LittleEndianParser.readUTF16LEChars(fileData, i + 28, 4, part);

                    longFileNameParts.add(0, part.toString());
                    continue;
                }


                // Is entry deleted
                boolean isDeleted = firstByte == 0xE5;

                // Is entry a Directory
                boolean isDirectory = (attribute & 0x10) != 0;

                String name = String.join("", longFileNameParts);
                if (name.isEmpty()) {
                    // Fallback to 8.3 short name
                    name = new String(fileData, i, 8, StandardCharsets.US_ASCII).trim();
                    String ext = new String(fileData, i + 8, 3, StandardCharsets.US_ASCII).trim();

                    // Add extensions to file names
                    if (!ext.isEmpty()) name += "." + ext;
                }

                longFileNameParts.clear();

                // Skip current directory and parent directory
                if (name.equals(".") || name.equals("..")) continue;

                // Replace first char with _ if the entry has been deleted
                if (isDeleted && !name.isEmpty()) name = "_" + name.substring(1);

                // Calculate Cluster
                long high = LittleEndianParser.readUInt16LE(fileData, i + 20) & 0xFFFFL;
                long low = LittleEndianParser.readUInt16LE(fileData, i + 26) & 0xFFFFL;
                long startCluster = (high << 16) | low;

                // Calculate size of File
                long fileSize = LittleEndianParser.readUInt32LE(fileData, i + 28);

                if (startCluster != 0 && (startCluster < 2 || startCluster >= bootSector.clusterCount + 2)) continue;

                // Add directory entry to list of all entries
                entries.add(new filesystem.DirectoryEntry(
                        name, startCluster, fileSize, isDeleted, isDirectory, level
                ));

                // Enter subdirectory
                if (isDirectory && startCluster >= 2 && startCluster < bootSector.clusterCount + 2) entries.addAll(
                        readDirectory(startCluster, (level + 1), bootSector, startByte, disk)
                );
            }
        }

        return entries;
    }

    private static boolean isValidCluster(long cluster, BootSector bootSector) {
        return cluster >= 2 && cluster < bootSector.clusterCount + 2;
    }

    private static boolean isEoc(long cluster) {
        return cluster >= 0x0FFFFFF8L && cluster <= 0x0FFFFFFFL;
    }

    private static boolean isBadCluster(long cluster) {
        return cluster == 0x0FFFFFF7L;
    }

    public static List<byte[]> readClusterChain(long startCluster, BootSector bootSector,  long startByte,
                                                RandomAccessFile disk) throws IOException {
        List<byte[]> clusters = new ArrayList<>();

        long currentCluster = startCluster;

        System.out.println("cluster = " + currentCluster);


        // Until End of Chain
        while (true) {
//            if (currentCluster < 2 || currentCluster >= bootSector.clusterCount + 2) break;
//            if (currentCluster >= 0x0FFFFFF8L) break;
            if (!isValidCluster(currentCluster, bootSector)) break;
            if (isEoc(currentCluster)) break;
            if (isBadCluster(currentCluster)) break;

            clusters.add(readCluster(currentCluster, bootSector, startByte, disk));

            long nextCluster = getNextCluster(currentCluster, bootSector, startByte, disk);

            // Protection against corrupted FAT
            if (nextCluster == 0 || nextCluster == currentCluster) break;
            if (nextCluster < 2 || nextCluster >= bootSector.clusterCount + 2) break;

            currentCluster = nextCluster;
        }
        return clusters;
    }

    public static byte[] readCluster(long cluster, BootSector bootSector,  long startByte, RandomAccessFile disk)
            throws IOException {
        long firstDataSector = bootSector.reservedSectorsCount + (bootSector.fatsCount * bootSector.fatSize);
        long firstSectorOfCluster = ((cluster - 2) * bootSector.sectorsPerCluster) + firstDataSector;

        long offset = startByte + (firstSectorOfCluster * bootSector.bytesPerSector);

        System.out.println("firstDataSector = " + firstDataSector);
        System.out.println("firstSectorOfCluster = " + firstSectorOfCluster);
        System.out.println("offset = " + offset);
        System.out.println("file length = " + disk.length());

        disk.seek(offset);

        byte[] data = new byte[bootSector.clusterSize];
        disk.readFully(data);

        return data;
    }

    public static long getNextCluster(long cluster, BootSector bootSector,  long startByte, RandomAccessFile disk)
            throws IOException {
        long fatStartByte = startByte + ((long) bootSector.reservedSectorsCount * bootSector.bytesPerSector);

        long offset = fatStartByte + (cluster * 4L);

        if (offset + 4 > disk.length()) return 0;

        disk.seek(offset);
        byte[] bytes = new byte[4];
        disk.readFully(bytes);

        long value = ((bytes[3] & 0xFFL) << 24) |
                ((bytes[2] & 0xFFL) << 16) |
                ((bytes[1] & 0xFFL) << 8)  |
                (bytes[0] & 0xFFL);

        // FAT32 uses only 28 bits


        return value & 0x0FFFFFFFL;
    }
}
