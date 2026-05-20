import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class Fat32Reader {
    public RandomAccessFile disk;
    public BootSector bootSector;
    public int offsetToBootSector = 0;
    public List<DirectoryEntry> directoryEntries = new LinkedList<>();

    public void open(String drivePath) throws IOException {
        if (drivePath.isEmpty()) throw new IOException();

        // if it is an Image the boot sector starts not at Byte 0
        if (drivePath.endsWith(".img")) {
            this.offsetToBootSector = 2048 * 8;
        }

        // only able to read
        this.disk = new RandomAccessFile(drivePath, "r");

        // go to the start of the boot sector
        disk.seek(this.offsetToBootSector);

        // read the boot sector and find important information
        byte[] bootSectorBytes = new byte[(512)];
        this.disk.readFully(bootSectorBytes);
        this.bootSector = new BootSector(bootSectorBytes);

        this.directoryEntries = this.readDirectory(this.bootSector.rootCluster);
    }

    private void readUTF16LEChars(byte[] data, int offset, int length, StringBuilder stringBuilder) {
        // loop advances by 2 bytes each iteration because UTF-16 characters are 2 bytes long
        for (int i = offset; i < offset + length; i += 2) {
            int lowByte = data[i] & 0xFF; // first
            int highByte = data[i + 1] & 0xFF;  // second

            // reconstructs a 16-bit value from two bytes
            int code = (highByte << 8) | lowByte;

            // end markers
            if (code == 0x0000 || code == 0xFFFF)
                break;

            // converts UTF-16 Code to a Chars
            stringBuilder.append((char) code);
        }
    }

    private List<DirectoryEntry> readDirectory(long cluster) throws IOException {
        // cluster can not be lower than the root directory cluster
        if (cluster < this.bootSector.rootCluster) return null;

        List<DirectoryEntry> directoryEntries = new LinkedList<>();
        directoryEntries.addAll(readDirectory(cluster, 0));

        return directoryEntries;
    }

    private List<DirectoryEntry> readDirectory(long cluster, int level) throws IOException {
        List<byte[]> clusters = readClusterChain(cluster);
        List<DirectoryEntry> directoryEntries = new LinkedList<>();

        for (byte[] fileData : clusters) {
            List<String> longFileNameParts = new LinkedList<>();

            for (int i = 0; i < fileData.length; i += 32) {
                int firstByte = fileData[i] & 0xFF;
                boolean isDeleted = false;
                String name = "";

                // unused entry
                if (firstByte == 0x00) break;

                // is entry deleted
                if (firstByte == 0xE5) isDeleted = true;

                int attribute = fileData[i + 11] & 0xFF;

                // is entry a Directory
                boolean isDirectory = (attribute & 0x10) != 0;

                // has entry a long Filename
                if (attribute == 0x0F) {
                    StringBuilder part = new StringBuilder();

                    // chars 1-5
                    readUTF16LEChars(fileData, i + 1, 10, part);

                    // chars 6-11
                    readUTF16LEChars(fileData, i + 14, 12, part);

                    // chars 12-13
                    readUTF16LEChars(fileData, i + 28, 4, part);

                    longFileNameParts.add(0, part.toString());
                    continue;
                }
                else {
                    name = String.join("", longFileNameParts);

                    if (name.isEmpty()) {
                        // fallback to 8.3 short name
                        name = new String(fileData, i, 8, StandardCharsets.US_ASCII).trim();
                        String ext = new String(fileData, i + 8, 3, StandardCharsets.US_ASCII).trim();

                        if (!ext.isEmpty()) name += "." + ext;
                    }

                    longFileNameParts.clear();
                }

                // replace first char with _ if the entry has been deleted
                if (isDeleted) name = "_" + name.substring(1);

                // calculate Cluster
                long high = ((fileData[i + 21] & 0xFFFF) << 8) | (fileData[i + 20] & 0xFFFF);
                long low = ((fileData[i + 27] & 0xFFFF) << 8) | (fileData[i + 26] & 0xFFFF);
                long startCluster = (high << 16) | low;

                // calculate size of File
                long fileSize = ((fileData[i + 31] & 0xFFL) << 24) |
                        ((fileData[i + 30] & 0xFFL) << 16) |
                        ((fileData[i + 29] & 0xFFL) << 8) |
                        (fileData[i + 28] & 0xFFL);

                // skip current directory and parent directory
                if (name.equals(".") || name.equals("..")) continue;

                // add directory entry to list of all entries
                directoryEntries.add(new DirectoryEntry(
                        name, startCluster, fileSize, isDeleted, isDirectory, level
                ));

                // enter subdirectory
                if (isDirectory) directoryEntries.addAll(readDirectory(startCluster, (level + 1)));
            }
        }

        return directoryEntries;
    }

    private long getNextCluster(long cluster) throws IOException {
        long fatOffset = this.offsetToBootSector + ((long) this.bootSector.reservedSectorsCount * this.bootSector.bytesPerSector);

        long fatEntryOffset = fatOffset + (cluster * 4);

        disk.seek(fatEntryOffset);

        byte[] bytes = new byte[4];
        disk.readFully(bytes);

        long value = ((bytes[3] & 0xFFL) << 24) |
                ((bytes[2] & 0xFFL) << 16) |
                ((bytes[1] & 0xFFL) << 8)  |
                (bytes[0] & 0xFFL);

        // FAT32 uses only 28 bits
        value &= 0x0FFFFFFFL;

        return value;
    }

    private byte[] readCluster(long cluster) throws IOException {
        long firstDataSector = this.bootSector.reservedSectorsCount + (this.bootSector.fatsCount * this.bootSector.fatSize);
        long firstSectorOfCluster = ((cluster - 2) * this.bootSector.sectorsPerCluster) + firstDataSector;

        long offset = this.offsetToBootSector + (firstSectorOfCluster * this.bootSector.bytesPerSector);

        this.disk.seek(offset);

        byte[] data = new byte[this.bootSector.clusterSize];
        this.disk.readFully(data);

        return data;
    }

    private List<byte[]> readClusterChain(long startCluster) throws IOException {
        List<byte[]> clusters = new LinkedList<>();

        long currentCluster = startCluster;

        // until End of Chain
        while (!(currentCluster >= 0x0FFFFFF8L)) {

            clusters.add(readCluster(currentCluster));

            long nextCluster = getNextCluster(currentCluster);

            // Protection against Corrupted FAT
            if (nextCluster == 0 || nextCluster == currentCluster) {
                break;
            }

            currentCluster = nextCluster;
        }
        return clusters;
    }

    public List<DirectoryEntry> getAllDeletedFilesAndDirectories() {
        List<DirectoryEntry> deleted = new LinkedList<>();

        for (DirectoryEntry directoryEntry : this.directoryEntries) {
            // when directory entry is deleted add to list of deleted
            if (directoryEntry.isDeleted) {
                deleted.add(directoryEntry);
            }
        }

        return deleted;
    }

    public Map<Integer, List<DirectoryEntry>> getAllFilesAndDirectoriesFromDirectory(String name) throws IOException {
        // list of clusters for if the name of the directory exists multiple time
        List<Long> clusters = new LinkedList<>();

        for (DirectoryEntry directoryEntry : this.directoryEntries) {
            if (directoryEntry.isDirectory) {
                if (directoryEntry.fileName.equals(name)) {
                    // add cluster to list if the directory has the same name as is searched for
                    clusters.add(directoryEntry.startCluster);
                }
            }
        }

        // map for if the name of the directory exists multiple time
        Map<Integer, List<DirectoryEntry>> searched = new HashMap<>();

        // add for each cluster in the list of clusters a list of directory entries to the map with an index
        for (int i = 0; i < clusters.size(); i++) {
            searched.put(i, this.readDirectory(clusters.get(i)));
        }

        return searched;
    }
}
