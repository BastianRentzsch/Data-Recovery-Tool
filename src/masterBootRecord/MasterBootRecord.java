package masterBootRecord;

import java.io.IOException;
import java.io.RandomAccessFile;

public class MasterBootRecord {
    public PartitionEntry[] partitionEntries;

    public MasterBootRecord(RandomAccessFile disk) throws IOException {
        this.partitionEntries = new PartitionEntry[4];
        long pos = 446;
        for (int i = 0; i < 4; i++) {
            disk.seek(pos);

            byte[] bytes = new byte[16];
            disk.readFully(bytes);
            partitionEntries[i] = new PartitionEntry(bytes);

            pos += 16;
        }
    }
}
