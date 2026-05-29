package masterBootRecord;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * PartitionEntry represents a single partition table entry from the MBR.
 *
 * <p>Each partition entry is 16 bytes and contains:<br>
 * - Bootable flag (1 byte at offset 0): 0x80 = bootable, 0x00 = non-bootable<br>
 * - Start CHS address (3 bytes at offset 1): Cylinder-Head-Sector start<br>
 * - Partition type (1 byte at offset 4): 0x0B = FAT32(big), 0x0C = FAT32(LBA)<br>
 * - End CHS address (3 bytes at offset 5): Cylinder-Head-Sector end<br>
 * - Start LBA (4 bytes at offset 8): Logical Block Address start<br>
 * - Size (4 bytes at offset 12): Number of sectors in partition</p>
 *
 * <p>All multi-byte values are stored in little-endian format.</p>
 *
 * @author Bastian Rentzsch
 * @version 1.0
 */
public class PartitionEntry {
    /** Bootable flag: 0x80 = active/bootable, 0x00 = inactive */
    public int flag;

    /** Start CHS address (cylinder-head-sector format, 3 bytes) */
    public int startCHS;

    /** Partition type identifier: 0x0B = FAT32, 0x0C = FAT32 LBA, 0x83 = Linux */
    public int type;

    /** End CHS address (cylinder-head-sector format, 3 bytes) */
    public int endCHS;

    /** Starting Logical Block Address (LBA) of the partition */
    public long startLBA;

    /** Size of the partition in sectors (512 bytes each) */
    public long size;

    /**
     * Constructs a PartitionEntry by parsing 16 bytes from the MBR.
     *
     * <p>Reads fields at fixed offsets using little-endian byte order:<br>
     * - flag: offset 0 (1 byte)<br>
     * - startCHS: offset 1 (3 bytes)<br>
     * - type: offset 4 (1 byte)<br>
     * - endCHS: offset 5 (3 bytes)<br>
     * - startLBA: offset 8 (4 bytes)<br>
     * - size: offset 12 (4 bytes)</p>
     *
     * <p>All values are masked with & 0xFF or & 0xFFFFFFL to convert
     * signed Java bytes to unsigned values.</p>
     *
     * @param entry the 16-byte partition entry data from the MBR
     */
    PartitionEntry(byte[] entry) {
        // Creates a little-endian byte buffer for reading partition values
        ByteBuffer byteBuffer = ByteBuffer.wrap(entry).order(ByteOrder.LITTLE_ENDIAN);

        this.flag = byteBuffer.get(0) & 0xFF;
        this.startCHS = byteBuffer.getInt(1) & 0xFFFFFF;
        this.type = byteBuffer.get(4) & 0xFF;
        this.endCHS = byteBuffer.getInt(5) & 0xFFFFFF;
        this.startLBA = byteBuffer.getInt(8) & 0xFFFFFFFFL;
        this.size = byteBuffer.getInt(12) & 0xFFFFFFFFL;
    }
}