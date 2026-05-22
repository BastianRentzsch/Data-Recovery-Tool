package directory;

public class DirectoryParser {
//    private List<directory.DirectoryEntry> readDirectory(long cluster) throws IOException {
//        // Cluster can not be lower than the root directory cluster
//        if (cluster < this.bootSector.rootCluster) return new ArrayList<>();
//
//        return new ArrayList<>(readDirectory(cluster, 0));
//    }
//
//    public List<directory.DirectoryEntry> readDirectory(long cluster, int level) throws IOException {
//        // Cluster can not be lower than the root directory cluster
////        if (cluster < 2 ) return new ArrayList<>();
//
//        // Get all clusters that belong together with that cluster
//        List<byte[]> clusters = readClusterChain(cluster);
//        List<directory.DirectoryEntry> entries = new ArrayList<>();
////----
////        List<LfnEntry> pendingLfn = new ArrayList<>();
////----
//
//        for (byte[] fileData : clusters) {
//            List<String> longFileNameParts = new ArrayList<>();
//
//            for (int i = 0; i < fileData.length; i += 32) {
//                int firstByte = fileData[i] & 0xFF;
//
//                // Unused entry
//                if (firstByte == 0x00) {
////                    pendingLfn.clear();
//                    break;
//                }
//
//                // For looking if it is a directory and if it has a long name
//                int attribute = fileData[i + 11] & 0xFF;
//
//                // Has entry a long Filename
//                if (attribute == 0x0F) {
//                    StringBuilder part = new StringBuilder();
//
//                    // Chars 1-5
//                    LittleEndianParser.readUTF16LEChars(fileData, i + 1, 10, part);
//
//                    // Chars 6-11
//                    LittleEndianParser.readUTF16LEChars(fileData, i + 14, 12, part);
//
//                    // Chars 12-13
//                    LittleEndianParser.readUTF16LEChars(fileData, i + 28, 4, part);
//
//                    longFileNameParts.add(0, part.toString());
//                    continue;
////                    int ord = fileData[i] & 0xFF;
////                    boolean last = (ord & 0x40) != 0;
////                    int ordinal = ord & 0x1F;
////                    int checksum = fileData[i + 13] & 0xFF;
////                    String part = readLfnPart(fileData, i);
////
////                    pendingLfn.add(new LfnEntry(ordinal, last, checksum, part));
////                    continue;
//                }
//
//                // Is entry deleted
//                boolean isDeleted = firstByte == 0xE5;
//
//                // Is entry a Directory
//                boolean isDirectory = (attribute & 0x10) != 0;
//
////                byte[] shortNameBytes = Arrays.copyOfRange(fileData, i, i + 11);
////                int expectedChecksum = lfnChecksum(shortNameBytes);
//
////                String name = buildValidatedLfnName(pendingLfn, expectedChecksum);
//
//                String name = String.join("", longFileNameParts);
////                name == null ||
//                if (name.isEmpty()) {
//                    // Fallback to 8.3 short name
//                    name = new String(fileData, i, 8, StandardCharsets.US_ASCII).trim();
//                    String ext = new String(fileData, i + 8, 3, StandardCharsets.US_ASCII).trim();
//
//                    // Add extensions to file names
//                    if (!ext.isEmpty()) name += "." + ext;
//                }
//
//                longFileNameParts.clear();
////                pendingLfn.clear();
//
//                // Skip current directory and parent directory
//                if (name.equals(".") || name.equals("..")) continue;
//
//                // Replace first char with _ if the entry has been deleted
//                if (isDeleted && !name.isEmpty()) name = "_" + name.substring(1);
//
//                // Calculate Cluster
////                long high = ((fileData[i + 21] & 0xFFFF) << 8) | (fileData[i + 20] & 0xFFFF);
////                long low = ((fileData[i + 27] & 0xFFFF) << 8) | (fileData[i + 26] & 0xFFFF);
//
////                long high = ((fileData[i + 21] & 0xFFL) << 8) |
////                        (fileData[i + 20] & 0xFFL);
////                long low = ((fileData[i + 27] & 0xFFL) << 8) |
////                        (fileData[i + 26] & 0xFFL);
////                long startCluster = (high << 16) | low;
//
//                long high = LittleEndianParser.readUInt16LE(fileData, i + 20) & 0xFFFFL;
//                long low = LittleEndianParser.readUInt16LE(fileData, i + 26) & 0xFFFFL;
//                long startCluster = (high << 16) | low;
//
//                // Calculate size of File
////                long fileSize = ((fileData[i + 31] & 0xFFL) << 24) |
////                        ((fileData[i + 30] & 0xFFL) << 16) |
////                        ((fileData[i + 29] & 0xFFL) << 8) |
////                        (fileData[i + 28] & 0xFFL);
//                long fileSize = LittleEndianParser.readUInt32LE(fileData, i + 28);
//
//                // Add directory entry to list of all entries
//                entries.add(new directory.DirectoryEntry(
//                        name, startCluster, fileSize, isDeleted, isDirectory, level
//                ));
//
//                // Enter subdirectory
//                if (isDirectory && startCluster >= 2) entries.addAll(
//                        readDirectory(startCluster, (level + 1))
//                );
//            }
//        }
//
//        return entries;
//    }
}
