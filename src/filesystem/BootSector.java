package filesystem;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * <p>BootSector represents the FAT32 BIOS Parameter Block (BPB)
 * extracted from the first 512 bytes of a partition.</p>
 *
 * <p>This class parses critical FAT32 filesystem parameters including
 * bytes per sector, sectors per cluster, FAT size, root cluster,
 * and calculates derived values like cluster count and cluster size.</p>
 *
 * <p>These parameters are essential for calculating byte offsets when
 * reading clusters, navigating the FAT table, and parsing directory entries.</p>
 *
 * @author Bastian Rentzsch
 * @version 1.0
 */
public class BootSector {

    /** Number of bytes in a sector (typically 512) */
    public int bytesPerSector;

    /** Number of sectors in a cluster (1-128, must be power of 2) */
    public int sectorsPerCluster;

    /** Number of reserved sectors before the FAT (typically 32 for FAT32) */
    public int reservedSectorsCount;

    /** Number of FAT tables (typically 2 for FAT32) */
    public int fatsCount;

    /** Size of one FAT table in sectors */
    public long fatSize;

    /** Total number of sectors in the partition */
    public long totalSectors;

    /** Starting cluster number of the root directory (typically 2) */
    public long rootCluster;

    /** Total number of data sectors in the partition */
    public long dataSector;

    /** Total number of clusters in the data region */
    public long clusterCount;

    /** Size of a single cluster in bytes */
    public int clusterSize;

    /** First sector number containing data (after reserved sectors and FATs) */
    public long firstDataSector;

    /**
     * Constructs a BootSector by parsing the FAT32 BIOS Parameter Block.
     *
     * <p>Reads values from fixed offsets in the 512-byte boot sector:<br>
     * - bytesPerSector: offset 11 (2 bytes)<br>
     * - sectorsPerCluster: offset 13 (1 byte)<br>
     * - reservedSectorsCount: offset 14 (2 bytes)<br>
     * - fatsCount: offset 16 (1 byte)<br>
     * - totalSectors: offset 32 (4 bytes)<br>
     * - fatSize: offset 36 (4 bytes)<br>
     * - rootCluster: offset 44 (4 bytes)<br></p>
     *
     * <p>Calculates derived values:<br>
     * - dataSector = totalSectors - reservedSectorsCount - (fatsCount × fatSize)<br>
     * - clusterCount = dataSector / sectorsPerCluster<br>
     * - clusterSize = bytesPerSector × sectorsPerCluster<br>
     * - firstDataSector = reservedSectorsCount + (fatsCount × fatSize)<br></p>
     *
     * @param bootSector the 512-byte boot sector data from the partition
     */
    public BootSector(byte[] bootSector) {
        // Creates a little-endian byte buffer for reading FAT32 values
        ByteBuffer byteBuffer = ByteBuffer.wrap(bootSector).order(ByteOrder.LITTLE_ENDIAN);

        // Offset starts with 0 byte for BPB (BIOS Parameter Block)
        this.bytesPerSector = byteBuffer.getShort(11) & 0xFFFF;
        this.sectorsPerCluster = byteBuffer.get(13) & 0xFF;
        this.reservedSectorsCount = byteBuffer.getShort(14) & 0xFFFF;
        this.fatsCount = byteBuffer.get(16) & 0xFF;
        this.totalSectors = byteBuffer.get(32) & 0xFFFFFFFFL;
        this.fatSize = byteBuffer.getInt(36) & 0xFFFFFFFFL;
        this.rootCluster = byteBuffer.getInt(44) & 0xFFFFFFFFL;

        this.dataSector = this.totalSectors - (this.reservedSectorsCount + (this.fatsCount * this.fatSize));
        this.clusterCount = dataSector / this.sectorsPerCluster;
        this.clusterSize = this.bytesPerSector * this.sectorsPerCluster;
        this. firstDataSector = this.reservedSectorsCount + (this.fatsCount * this.fatSize);
    }

    /**
     * Prints FAT32 boot sector information to the console.
     *
     * Displays key filesystem parameters in a formatted way
     * with blue color coding for better readability.
     *
     * @param index the partition index number for the header (1, 2, 3...)
     */
    public void printInfo(int index) {
        System.out.println("\u001b[34m===" + index + ". Boot Sector ===\u001b[0m");
        System.out.println("Bytes per Sector: " + this.bytesPerSector);
        System.out.println("Sectors per Cluster: " + this.sectorsPerCluster);
        System.out.println("Reserved Sectors: " + this.reservedSectorsCount);
        System.out.println("FAT Count: " + this.fatsCount);
        System.out.println("FAT Size: " + this.fatSize);
        System.out.println("Root Cluster: " + this.rootCluster);
    }
}