package utilities;

public class LittleEndianParser {

    // Reads an unsigned 16-bit little-endian value
    public static int readUInt16LE(byte[] data,
                                   int offset) {

        // Combines two bytes into a 16-bit integer
        return ((data[offset + 1] & 0xFF) << 8) | (data[offset] & 0xFF);

    }

    // Reads an unsigned 32-bit little-endian value
    public static long readUInt32LE(byte[] data,
                                    int offset) {

        // Combines four bytes into a 32-bit long value
        return ((data[offset + 3] & 0xFFL) << 24) |
                ((data[offset + 2] & 0xFFL) << 16) |
                ((data[offset + 1] & 0xFFL) << 8) |
                (data[offset] & 0xFFL);

    }

    // Reads UTF-16 little-endian characters from a byte array
    public static void readUTF16LEChars(byte[] data,
                                        int offset,
                                        int length,
                                        StringBuilder stringBuilder) {

        // Iterates through UTF-16 characters using 2-byte steps
        for (int i = offset; i < offset + length; i += 2) {

            // Reads a single UTF-16 character code
            int code = readUInt16LE(data, i);

            // Stops at UTF-16 end markers
            if (code == 0x0000 || code == 0xFFFF) break;

            // Converts the UTF-16 code to a character
            stringBuilder.append((char) code);

        }

    }

}
