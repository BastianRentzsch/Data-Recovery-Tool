import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class BootSector {
    public int bytesPerSector;
    public int sectorsPerCluster;
    public int reservedSectorsCount;
    public int fatsCount;
    public long fatSize;
    public long rootCluster;
    public int clusterSize;

    public BootSector(byte[] bootSector) {
        ByteBuffer byteBuffer = ByteBuffer.wrap(bootSector).order(ByteOrder.LITTLE_ENDIAN);

        this.bytesPerSector = byteBuffer.getShort(11) & 0xFFFF;
        this.sectorsPerCluster = byteBuffer.get(13) & 0xFF;
        this.reservedSectorsCount = byteBuffer.getShort(14) & 0xFFFF;
        this.fatsCount = byteBuffer.get(16) & 0xFF;
        this.fatSize = byteBuffer.getInt(36) & 0xFFFFFFFFL;
        this.rootCluster = byteBuffer.getInt(44) & 0xFFFFFFFFL;
        this.clusterSize = this.bytesPerSector * this.sectorsPerCluster;
    }

    public void printInfo() {
        System.out.println("\u001b[34m=== FAT32 Boot Sector ===\u001b[0m");
        System.out.println("Bytes per Sector: " + this.bytesPerSector);
        System.out.println("Sectors per Cluster: " + this.sectorsPerCluster);
        System.out.println("Reserved Sectors: " + this.reservedSectorsCount);
        System.out.println("FAT Count: " + this.fatsCount);
        System.out.println("FAT Size: " + this.fatSize);
        System.out.println("Root Cluster: " + this.rootCluster);
    }
}
