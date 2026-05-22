package partitionTable;

import filesystem.DirectoryEntry;
import filesystem.DirectoryParser;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.List;

public class Partition {
    public long startByte;
    public BootSector bootSector;
    public List<DirectoryEntry> directoryEntries;

    public Partition(RandomAccessFile disk, long startLBA) throws IOException {
        // Jump to the start of the partition
        this.startByte = 512 * startLBA;
        disk.seek(this.startByte);

        // Read the boot sector of this partition
        byte[] bootSectorBytes = new byte[(512)];
        disk.readFully(bootSectorBytes);
        this.bootSector = new BootSector(bootSectorBytes);

        this.directoryEntries = this.readDirectory(disk);
    }

    public List<DirectoryEntry> readDirectory(RandomAccessFile disk) throws IOException {
//        if (disk == null) return new ArrayList<>();
        return DirectoryParser.readDirectory(this.bootSector.rootCluster, this.bootSector, this.startByte, disk);
    }
}
