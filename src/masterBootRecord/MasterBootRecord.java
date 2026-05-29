package masterBootRecord;

import java.io.IOException;
import java.io.RandomAccessFile;

/**
 * MasterBootRecord represents the Master Boot Record (MBR) from the first
 * sector (512 bytes) of a disk image.
 *
 * <p>The MBR contains:<br>
 * - Bootstrap code (bytes 0-445)<br>
 * - Four partition entries (bytes 446-509, 16 bytes each)<br>
 * - Boot signature (bytes 510-511, should be 0x55AA)</p>
 *
 * <p>This class parses the four partition entries starting at offset 446
 * and stores them in a PartitionEntry array.</p>
 *
 * @author Bastian Rentzsch
 * @version 1.0
 */
public class MasterBootRecord {
    /** Array of four partition entries (may contain nulls for empty entries) */
    public PartitionEntry[] partitionEntries;

    /**
     * Constructs a MasterBootRecord by reading four partition entries from disk.
     *
     * <p>Reads 16 bytes for each partition entry starting at offset 446:<br>
     * - Entry 0: offset 446<br>
     * - Entry 1: offset 462<br>
     * - Entry 2: offset 478<br>
     * - Entry 3: offset 494</p>
     *
     * <p>Each entry is passed to the PartitionEntry constructor for parsing.</p>
     *
     * @param disk the RandomAccessFile handle for the disk image
     * @throws IOException if reading partition entries fails
     */
    public MasterBootRecord(RandomAccessFile disk) throws IOException {
        // Allocates space for the maximum of four MBR partitions
        this.partitionEntries = new PartitionEntry[4];

        // the Master Partition entries start at byte 446
        long pos = 446;

        for (int i = 0; i < 4; i++) {
            disk.seek(pos);

            // One entry is 16 byte big
            byte[] bytes = new byte[16];
            disk.readFully(bytes);
            partitionEntries[i] = new PartitionEntry(bytes);

            pos += 16;
        }
    }
}