package filesystem;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class BootSector {

    public int bytesPerSector;
    public int sectorsPerCluster;
    public int reservedSectorsCount;
    public int fatsCount; // Stores the number of FAT tables
    public long fatSize; // Stores the size of one FAT in sectors
    public long totalSectors;
    public long rootCluster;

    public long dataSector; // Stores the total number of data sectors
    public long clusterCount; // Stores the total number of clusters
    public int clusterSize; // Stores the size of a cluster in bytes
    public long firstDataSector; // Stores the first sector of the data region

    public BootSector(byte[] bootSector) {

        // Creates a little-endian byte buffer for reading FAT32 values
        ByteBuffer byteBuffer = ByteBuffer.wrap(bootSector).order(ByteOrder.LITTLE_ENDIAN);

        // Offset starts with 0 byte for BPB (BIOS Parameter Block)

        // Offset 11 byte | Size 2 bytes
        this.bytesPerSector = byteBuffer.getShort(11) & 0xFFFF;
        // Offset 13 byte | Size 1 byte
        this.sectorsPerCluster = byteBuffer.get(13) & 0xFF;
        // Offset 14 byte | Size 2 bytes
        this.reservedSectorsCount = byteBuffer.getShort(14) & 0xFFFF;
        // Offset 16 byte | Size 1 byte
        this.fatsCount = byteBuffer.get(16) & 0xFF;
        // Offset 32 byte | Size 4 byte
        this.totalSectors = byteBuffer.get(32) & 0xFFFFFFFFL;
        // Offset 36 byte | Size 4 bytes
        this.fatSize = byteBuffer.getInt(36) & 0xFFFFFFFFL;
        // Root directory | Offset 44 byte | Size 4 bytes
        this.rootCluster = byteBuffer.getInt(44) & 0xFFFFFFFFL;

        // The count of sectors in the data region of the partition
        this.dataSector = this.totalSectors - (this.reservedSectorsCount + (this.fatsCount * this.fatSize));
        // the count of clusters in the data region of the partition
        this.clusterCount = dataSector / this.sectorsPerCluster;
        // Size of a single cluster
        this.clusterSize = this.bytesPerSector * this.sectorsPerCluster;
        // First sector with data of the partition
        this. firstDataSector = this.reservedSectorsCount + (this.fatsCount * this.fatSize);

    }

    // Prints important FAT32 boot sector information
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
