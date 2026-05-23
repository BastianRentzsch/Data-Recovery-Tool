package masterBootRecord;

import java.io.IOException;
import java.io.RandomAccessFile;

public class MasterBootRecord {

    public PartitionEntry[] partitionEntries;

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
