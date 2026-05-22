import directory.DirectoryEntry;
import masterBootRecord.MasterBootRecord;
import masterBootRecord.PartitionEntry;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.*;

public class Fat32Reader {
    public RandomAccessFile disk;
    public List<BootSector> bootSectors;
    public List<DirectoryEntry> directoryEntries;

//    public static class LfnEntry {
//        final int ordinal;
//        final boolean last;
//        final int checksum;
//        final String namePart;
//
//        LfnEntry(int ordinal, boolean last, int checksum, String namePart) {
//            this.ordinal = ordinal;
//            this.last = last;
//            this.checksum = checksum;
//            this.namePart = namePart;
//        }
//    }

    public void open(String drivePath) throws IOException {
        if (drivePath == null || drivePath.isEmpty()) throw new IOException("Invalid Path");

        // Able to read
        this.disk = new RandomAccessFile(drivePath, "r");

        // Start the reading of the image
        disk.seek(0);

        // Read the Master Boot Record
        byte[] mbrBytes = new byte[(512)];
        disk.readFully(mbrBytes);
        MasterBootRecord masterBootRecord = new MasterBootRecord(disk);

        // Create a new ArrayList for the bootSectors
        this.bootSectors = new ArrayList<>();

        // MBR has only 4 partitions
        for (PartitionEntry partitionEntry : masterBootRecord.partitionEntries) {
            // Is the partition bootable, if not than ignore that entry
            if (partitionEntry.flag == 0) continue;

            // Jump to the start of the partition
            long pos = 512 * partitionEntry.startLBA;
            disk.seek(pos);

            // Read the boot sector of this partition
            byte[] bootSectorBytes = new byte[512];
            this.disk.readFully(bootSectorBytes);
            this.bootSectors.add(new BootSector(bootSectorBytes));
        }
    }
    


//    public int lfnChecksum(byte[] shortName11) {
//        int sum = 0;
//        for (int i = 0; i < 11; i++) {
//            sum = ((sum & 1) != 0 ? 0x80 : 0) + (sum >> 1) + (shortName11[i] & 0xFF);
//            sum &= 0xFF;
//        }
//        return sum;
//    }
//    public String readLfnPart(byte[] fileData, int i) {
//        StringBuilder part = new StringBuilder();
//        readUTF16LEChars(fileData, i + 1, 10, part);
//        readUTF16LEChars(fileData, i + 14, 12, part);
//        readUTF16LEChars(fileData, i + 28, 4, part);
//        return part.toString();
//    }

//    private List<directory.DirectoryEntry> readDirectory(long cluster) throws IOException {
//        // Cluster can not be lower than the root directory cluster
//        if (cluster < this.bootSectors.rootCluster) return new ArrayList<>();
//
//        return new ArrayList<>(readDirectory(cluster, 0));
//    }

//    public List<directory.DirectoryEntry> readDirectory(long cluster, int level) throws IOException {
//        // Cluster can not be lower than the root directory cluster
////        if (cluster < 2 ) return new ArrayList<>();
//
//        // Get all clusters that belong together with that cluster
//        List<byte[]> clusters = readClusterChain(cluster);
//        List<directory.DirectoryEntry> entries = new ArrayList<>();
////----
////        List<LfnEntry> pendingLfn = new ArrayList<>();
////----
//
//        for (byte[] fileData : clusters) {
//            List<String> longFileNameParts = new ArrayList<>();
//
//            for (int i = 0; i < fileData.length; i += 32) {
//                int firstByte = fileData[i] & 0xFF;
//
//                // Unused entry
//                if (firstByte == 0x00) {
////                    pendingLfn.clear();
//                    break;
//                }
//
//                // For looking if it is a directory and if it has a long name
//                int attribute = fileData[i + 11] & 0xFF;
//
//                // Has entry a long Filename
//                if (attribute == 0x0F) {
//                    StringBuilder part = new StringBuilder();
//
//                    // Chars 1-5
//                    LittleEndianParser.readUTF16LEChars(fileData, i + 1, 10, part);
//
//                    // Chars 6-11
//                    LittleEndianParser.readUTF16LEChars(fileData, i + 14, 12, part);
//
//                    // Chars 12-13
//                    LittleEndianParser.readUTF16LEChars(fileData, i + 28, 4, part);
//
//                    longFileNameParts.add(0, part.toString());
//                    continue;
////                    int ord = fileData[i] & 0xFF;
////                    boolean last = (ord & 0x40) != 0;
////                    int ordinal = ord & 0x1F;
////                    int checksum = fileData[i + 13] & 0xFF;
////                    String part = readLfnPart(fileData, i);
////
////                    pendingLfn.add(new LfnEntry(ordinal, last, checksum, part));
////                    continue;
//                }
//
//                // Is entry deleted
//                boolean isDeleted = firstByte == 0xE5;
//
//                // Is entry a Directory
//                boolean isDirectory = (attribute & 0x10) != 0;
//
////                byte[] shortNameBytes = Arrays.copyOfRange(fileData, i, i + 11);
////                int expectedChecksum = lfnChecksum(shortNameBytes);
//
////                String name = buildValidatedLfnName(pendingLfn, expectedChecksum);
//
//                String name = String.join("", longFileNameParts);
////                name == null ||
//                if (name.isEmpty()) {
//                    // Fallback to 8.3 short name
//                    name = new String(fileData, i, 8, StandardCharsets.US_ASCII).trim();
//                    String ext = new String(fileData, i + 8, 3, StandardCharsets.US_ASCII).trim();
//
//                    // Add extensions to file names
//                    if (!ext.isEmpty()) name += "." + ext;
//                }
//
//                longFileNameParts.clear();
////                pendingLfn.clear();
//
//                // Skip current directory and parent directory
//                if (name.equals(".") || name.equals("..")) continue;
//
//                // Replace first char with _ if the entry has been deleted
//                if (isDeleted && !name.isEmpty()) name = "_" + name.substring(1);
//
//                // Calculate Cluster
////                long high = ((fileData[i + 21] & 0xFFFF) << 8) | (fileData[i + 20] & 0xFFFF);
////                long low = ((fileData[i + 27] & 0xFFFF) << 8) | (fileData[i + 26] & 0xFFFF);
//
////                long high = ((fileData[i + 21] & 0xFFL) << 8) |
////                        (fileData[i + 20] & 0xFFL);
////                long low = ((fileData[i + 27] & 0xFFL) << 8) |
////                        (fileData[i + 26] & 0xFFL);
////                long startCluster = (high << 16) | low;
//
//                long high = LittleEndianParser.readUInt16LE(fileData, i + 20) & 0xFFFFL;
//                long low = LittleEndianParser.readUInt16LE(fileData, i + 26) & 0xFFFFL;
//                long startCluster = (high << 16) | low;
//
//                // Calculate size of File
////                long fileSize = ((fileData[i + 31] & 0xFFL) << 24) |
////                        ((fileData[i + 30] & 0xFFL) << 16) |
////                        ((fileData[i + 29] & 0xFFL) << 8) |
////                        (fileData[i + 28] & 0xFFL);
//                long fileSize = LittleEndianParser.readUInt32LE(fileData, i + 28);
//
//                // Add directory entry to list of all entries
//                entries.add(new directory.DirectoryEntry(
//                        name, startCluster, fileSize, isDeleted, isDirectory, level
//                ));
//
//                // Enter subdirectory
//                if (isDirectory && startCluster >= 2) entries.addAll(
//                        readDirectory(startCluster, (level + 1))
//                );
//            }
//        }
//
//        return entries;
//    }
//---
//public String buildValidatedLfnName(List<LfnEntry> pendingLfn, int expectedChecksum) {
//    if (pendingLfn.isEmpty()) return null;
//
//    int n = pendingLfn.size();
//    LfnEntry lastEntry = pendingLfn.get(0);
//    if (!lastEntry.last || lastEntry.ordinal != n) return null;
//
//    for (int idx = 0; idx < n; idx++) {
//        LfnEntry entry = pendingLfn.get(idx);
//        int expectedOrdinal = n - idx;
//        if (entry.ordinal != expectedOrdinal) return null;
//        if (entry.checksum != expectedChecksum) return null;
//    }
//
//    StringBuilder name = new StringBuilder();
//    for (int idx = 0; idx < n; idx++) {
//        name.append(pendingLfn.get(idx).namePart);
//    }
//    return name.toString();
//}
//---

//    public long getNextCluster(long cluster) throws IOException {
//        long fatOffset = offsetToBootSector + ((long) this.bootSectors.reservedSectorsCount * this.bootSectors.bytesPerSector);
//
//        long fatEntryOffset = fatOffset + (cluster * 4L);
//
//        disk.seek(fatEntryOffset);
//        System.out.println("fatOffset = " + fatOffset);
//        System.out.println("fatEntryOffset = " + fatEntryOffset);
//        byte[] bytes = new byte[4];
//        disk.readFully(bytes);
//
//        long value = ((bytes[3] & 0xFFL) << 24) |
//                ((bytes[2] & 0xFFL) << 16) |
//                ((bytes[1] & 0xFFL) << 8)  |
//                (bytes[0] & 0xFFL);
//
//        // FAT32 uses only 28 bits
////        value & 0x0FFFFFFFL;
//
//        return value & 0x0FFFFFFFL;
//    }

//    public byte[] readCluster(long cluster) throws IOException {
//        long firstDataSector = this.bootSectors.reservedSectorsCount + (this.bootSectors.fatsCount * this.bootSectors.fatSize);
//        long firstSectorOfCluster = ((cluster - 2) * this.bootSectors.sectorsPerCluster) + firstDataSector;
//
//        long offset = offsetToBootSector + (firstSectorOfCluster * this.bootSectors.bytesPerSector);
//
//
//
//        System.out.println("firstDataSector = " + firstDataSector);
//        System.out.println("firstSectorOfCluster = " + firstSectorOfCluster);
//        System.out.println("offset = " + offset);
//        System.out.println("file length = " + disk.length());
//
//        this.disk.seek(offset);
//
//        byte[] data = new byte[this.bootSectors.clusterSize];
//        this.disk.readFully(data);
//
//        return data;
//    }

//    public List<byte[]> readClusterChain(long startCluster) throws IOException {
//        List<byte[]> clusters = new ArrayList<>();
//
//        long currentCluster = startCluster;
//        System.out.println("cluster = " + currentCluster);
//        // Until End of Chain
//        while (!(currentCluster >= 0x0FFFFFF8L)) {
//            if (currentCluster < 2) break;
//
//            clusters.add(readCluster(currentCluster));
//
//            long nextCluster = getNextCluster(currentCluster);
//
//            // Protection against corrupted FAT
//            if (nextCluster == 0 || nextCluster == currentCluster) {
//                break;
//            }
//
//            currentCluster = nextCluster;
//        }
//        return clusters;
//    }

    public List<DirectoryEntry> getAllDeletedFilesAndDirectories() {
        List<DirectoryEntry> deleted = new ArrayList<>();
        for (DirectoryEntry directoryEntry : this.directoryEntries) {
            // When directory entry is deleted add to list of deleted
            if (directoryEntry.isDeleted) deleted.add(directoryEntry);
        }
        return deleted;
    }

    public Map<Integer, List<DirectoryEntry>> getAllFilesAndDirectoriesFromDirectory(String name) throws IOException {
        // List of clusters for if the name of the directory exists multiple time
        List<Long> clusters = new ArrayList<>();

        for (DirectoryEntry directoryEntry : this.directoryEntries) {
            // Add cluster to list if the directory has the same name as is searched for
            if (directoryEntry.isDirectory && directoryEntry.fileName.equals(name)) clusters.add(directoryEntry.startCluster);
        }

        // Map for if the name of the directory exists multiple time
        Map<Integer, List<DirectoryEntry>> searched = new HashMap<>();

        // Add for each cluster in the list of clusters a list of directory entries to the map with an index
        for (int i = 0; i < clusters.size(); i++) {
//            searched.put(i, this.readDirectory(clusters.get(i), 0));
        }

        return searched;
    }
}
