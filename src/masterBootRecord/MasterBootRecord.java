package masterBootRecord;

import java.io.IOException;
import java.io.RandomAccessFile;

public class MasterBootRecord {
    public PartitionEntry[] partitionEntries = new PartitionEntry[4];

    public MasterBootRecord(RandomAccessFile disk) throws IOException {
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
