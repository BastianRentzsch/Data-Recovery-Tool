package utilities;

public class LittleEndianParser {
    public static int readUInt16LE(byte[] data, int offset) {
        return ((data[offset + 1] & 0xFF) << 8) | (data[offset] & 0xFF);
    }

    public static long readUInt32LE(byte[] data, int offset) {
        return ((data[offset + 3] & 0xFFL) << 24) |
                ((data[offset + 2] & 0xFFL) << 16) |
                ((data[offset + 1] & 0xFFL) << 8) |
                (data[offset] & 0xFFL);
    }

    public static void readUTF16LEChars(byte[] data, int offset, int length, StringBuilder stringBuilder) {
        // Loop advances by 2 bytes each iteration because UTF-16 characters are 2 bytes long
        for (int i = offset; i < offset + length; i += 2) {
            int code = readUInt16LE(data, i);

            // End markers
            if (code == 0x0000 || code == 0xFFFF) break;

            // Converts UTF-16 code to a chars
            stringBuilder.append((char) code);
        }
    }
}
