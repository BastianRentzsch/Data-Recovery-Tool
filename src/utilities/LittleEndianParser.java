package utilities;

/**
 * LittleEndianParser provides static utility methods for parsing
 * little-endian byte values from binary data.
 *
 * <p>FAT32 and x86 architectures use little-endian byte order where
 * the least significant byte comes first. This class provides methods
 * to correctly interpret multi-byte values from byte arrays.</p>
 *
 * <p>Also supports UTF-16 little-endian character decoding for
 * long filename entries in FAT32 directory structures.</p>
 *
 * <p>All methods are static as this is a pure utility class.</p>
 *
 * @author Bastian Rentzsch
 * @version 1.0
 */
public class LittleEndianParser {
    /**
     * Reads an unsigned 16-bit little-endian value from a byte array.
     *
     * <p>Combines two bytes where the first byte (at offset) is the
     * least significant byte and the second byte (at offset+1) is
     * the most significant byte.</p>
     *
     * <p>Formula: (data[offset+1] &amp; 0xFF) &lt;&lt; 8 | (data[offset] &amp; 0xFF)</p>
     *
     * <p>Example: bytes [0x34, 0x12] returns 0x1234 (4660 decimal)</p>
     *
     * @param data the byte array containing the value
     * @param offset the starting offset in the byte array
     * @return the unsigned 16-bit integer value (0 to 65535)
     */
    public static int readUInt16LE(byte[] data,
                                   int offset) {
        // Combines two bytes into a 16-bit integer
        return ((data[offset + 1] & 0xFF) << 8) | (data[offset] & 0xFF);
    }

    /**
     * Reads an unsigned 32-bit little-endian value from a byte array.
     *
     * <p>Combines four bytes where data[offset] is the least significant
     * byte and data[offset+3] is the most significant byte.</p>
     *
     * <p>Formula: (data[offset+3] &amp; 0xFFL) &lt;&lt; 24 |<br>
     *          (data[offset+2] &amp; 0xFFL) &lt;&lt; 16 |<br>
     *          (data[offset+1] &amp; 0xFFL) &lt;&lt; 8 |<br>
     *          (data[offset] &amp; 0xFFL)</p>
     *
     * <p>Example: bytes [0x78, 0x56, 0x34, 0x12] returns 0x12345678 (305419896 decimal)</p>
     *
     * @param data the byte array containing the value
     * @param offset the starting offset in the byte array
     * @return the unsigned 32-bit integer value as a long (0 to 4294967295)
     */
    public static long readUInt32LE(byte[] data,
                                    int offset) {
        // Combines four bytes into a 32-bit long value
        return ((data[offset + 3] & 0xFFL) << 24) |
                ((data[offset + 2] & 0xFFL) << 16) |
                ((data[offset + 1] & 0xFFL) << 8) |
                (data[offset] & 0xFFL);
    }

    /**
     * Reads UTF-16 little-endian characters from a byte array.
     *
     * <p>Iterates through the byte array in 2-byte steps, reading each
     * UTF-16 code unit using readUInt16LE(). Stops when encountering
     * end markers: 0x0000 (null terminator) or 0xFFFF (invalid).</p>
     *
     * <p>Appends each decoded character to the provided StringBuilder.</p>
     *
     * <p>Used for decoding long filenames in FAT32 directory entries
     * which store UTF-16 LE characters across multiple entries.</p>
     *
     * @param data the byte array containing UTF-16 LE encoded text
     * @param offset the starting offset in the byte array
     * @param length the number of bytes to read (must be even)
     * @param stringBuilder the StringBuilder to append decoded characters to
     */
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
