package utilities;

public class Fat32Util {
    public static final int FAT32_EOC_MIN = 0x0FFFFFF8;
    public static final int FAT32_BAD = 0x0FFFFFF7;
    public static final int FAT32_FREE = 0x00000000;

    public static boolean isValidDataCluster(long cluster) {
        return cluster >= 2 && cluster <= 0x0FFFFFEF;
    }

    public static boolean isEndOfChain(long cluster) {
        return cluster >= FAT32_EOC_MIN && cluster <= 0x0FFFFFFF;
    }

    public static boolean isBadCluster(long cluster) {
        return cluster == FAT32_BAD;
    }

    public static long nextClusterFromFatEntry(int fatEntry) {
        return fatEntry & 0x0FFFFFFFL;
    }

    public static long firstSectorOfCluster(long cluster, long firstDataSector, long sectorsPerCluster) {
        if (!isValidDataCluster(cluster)) {
            throw new IllegalArgumentException("Ungültiger Cluster: " + cluster);
        }
        return firstDataSector + (cluster - 2) * sectorsPerCluster;
    }

    public static long byteOffsetOfCluster(long cluster, long firstDataSector, long sectorsPerCluster, long bytesPerSector) {
        long firstSector = firstSectorOfCluster(cluster, firstDataSector, sectorsPerCluster);
        return firstSector * bytesPerSector;
    }

    public static void checkClusterAgainstImage(long cluster, long firstDataSector,
                                                long sectorsPerCluster, long bytesPerSector,
                                                long fileLength) {
        long offset = byteOffsetOfCluster(cluster, firstDataSector, sectorsPerCluster, bytesPerSector);
        long clusterSize = sectorsPerCluster * bytesPerSector;

        if (offset < 0 || offset + clusterSize > fileLength) {
            throw new IllegalStateException(
                    "Cluster außerhalb der Image-Datei: cluster=" + cluster +
                            ", offset=" + offset +
                            ", clusterSize=" + clusterSize +
                            ", fileLength=" + fileLength
            );
        }
    }
}