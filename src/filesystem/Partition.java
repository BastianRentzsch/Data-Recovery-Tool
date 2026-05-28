package filesystem;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.HashSet;
import java.util.List;

public class Partition {
    public long startByte; // Stores the byte offset where the partition starts
    public long endByte;  // Stores the byte offset where the partition ends
    public BootSector bootSector;
    public List<DirectoryEntry> directoryEntries;

    public Partition(RandomAccessFile disk,
                     long startLBA,
                     long size) throws IOException {
        this.startByte = 512 * startLBA;
        this.endByte = this.startByte + 512 * size;

        // Jump to the start of the partition
        disk.seek(this.startByte);

        // Read the boot sector of this partition
        byte[] bootSectorBytes = new byte[(512)];
        disk.readFully(bootSectorBytes);
        this.bootSector = new BootSector(bootSectorBytes);

        // Reads all files and directories starting from the root cluster
        this.directoryEntries = DirectoryParser.readDirectory(this.bootSector.rootCluster, 0, this.bootSector,
                                                              this.startByte, this.endByte, disk, new HashSet<>());
    }

    // Reads a directory starting from a specific cluster
    public List<DirectoryEntry> readDirectory(long startCluster,
                                              int level,
                                              RandomAccessFile disk) throws IOException {
        return DirectoryParser.readDirectory(startCluster, level, this.bootSector, this.startByte,
                                             this.endByte, disk, new HashSet<>());
    }
}