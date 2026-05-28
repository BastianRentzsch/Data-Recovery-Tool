package masterBootRecord;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class PartitionEntry {
    public int flag; // 1 byte | Stores the bootable flag of the partition
    public int startCHS; // 3 byte | Stores the start CHS address
    public int type; // 1 byte | Stores the partition type identifier
    public int endCHS; // 3 byte | Stores the end CHS address
    public long startLBA; // 4 byte | Stores the starting sector in LBA format
    public long size; // 4 byte | Stores the partition size in sectors

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