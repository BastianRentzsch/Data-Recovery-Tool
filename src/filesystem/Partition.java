package filesystem;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.HashSet;
import java.util.List;

/**
 * Partition represents a logical FAT32 partition on a disk image.
 *
 * <p>This class stores the byte boundaries of the partition, parses the
 * boot sector to extract FAT32 parameters, and reads all directory
 * entries from the root cluster recursively.</p>
 *
 * <p>The partition's directory entries are populated during construction
 * by calling DirectoryParser.readDirectory() starting from the root cluster.</p>
 *
 * @author Bastian Rentzsch
 * @version 1.0
 */
public class Partition {
    /** Byte offset where the partition starts (512 × startLBA) */
    public long startByte;

    /** Byte offset where the partition ends (startByte + 512 × size) */
    public long endByte;

    /** Boot sector containing FAT32 filesystem parameters */
    public BootSector bootSector;

    /** All files and directories found in this partition (including deleted) */
    public List<DirectoryEntry> directoryEntries;

    /**
     * Constructs a Partition by reading the boot sector and parsing all directory entries.
     *
     * <p>Calculates startByte and endByte from the LBA address and size,
     * seeks to the partition start, reads the 512-byte boot sector,
     * creates a BootSector object, and recursively reads all directory entries
     * starting from the root cluster.</p>
     *
     * @param disk the RandomAccessFile handle for the disk image
     * @param startLBA the starting logical block address of the partition
     * @param size the size of the partition in sectors (512 bytes each)
     * @throws IOException if reading the boot sector or directory entries fails
     */
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

    /**
     * Reads all entries from a directory starting at the given cluster.
     *
     * <p>Delegates to DirectoryParser.readDirectory() with the partition's
     * boot sector and byte boundaries.</p>
     *
     * @param startCluster the starting cluster number of the directory
     * @param level the directory depth level for formatted output
     * @param disk the RandomAccessFile handle for the disk image
     * @return a list of all DirectoryEntry objects in the directory
     * @throws IOException if reading cluster data fails
     */
    public List<DirectoryEntry> readDirectory(long startCluster,
                                              int level,
                                              RandomAccessFile disk) throws IOException {
        return DirectoryParser.readDirectory(startCluster, level, this.bootSector, this.startByte,
                                             this.endByte, disk, new HashSet<>());
    }
}