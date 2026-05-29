package filesystem;

/**
 * DirectoryEntry represents a single file or directory entry in a FAT32 filesystem.
 *
 * <p>Each entry contains metadata about a file or directory including its name,
 * starting cluster number, file size, and status flags indicating whether
 * it is deleted or a directory.</p>
 *
 * <p>Deleted entries are marked with isDeleted = true (first byte of entry = 0xE5).<br>
 * Directory entries are marked with isDirectory = true (attribute bit 0x10).</p>
 *
 * @author Bastian Rentzsch
 * @version 1.0
 */
public class DirectoryEntry {
    /** Name of the file or directory (8.3 or long filename) */
    public String fileName;

    /** Starting cluster number of the file or directory (cluster 2 = first data cluster) */
    public long startCluster;

    /** Size of the file in bytes (0 for directories) */
    public long fileSize;

    /** True if the entry is marked as deleted (first byte = 0xE5) */
    public boolean isDeleted;

    /** True if the entry is a directory (attribute bit 0x10) */
    public boolean isDirectory;

    /** Directory depth level for formatted output (0 = root, 1 = subdirectory, etc.) */
    public int level;

    /**
     * Constructs a DirectoryEntry with all metadata fields.
     *
     * @param fileName the name of the file or directory
     * @param startCluster the starting cluster number (≥ 2 for valid entries)
     * @param fileSize the file size in bytes (0 for directories)
     * @param isDeleted true if entry is marked deleted
     * @param isDirectory true if entry is a directory
     * @param level the directory depth level for indentation
     */
    public DirectoryEntry(String fileName,
                          long startCluster,
                          long fileSize,
                          boolean isDeleted,
                          boolean isDirectory,
                          int level) {
        this.fileName = fileName;
        this.startCluster = startCluster;
        this.fileSize = fileSize;
        this.isDeleted = isDeleted;
        this.isDirectory = isDirectory;
        this.level = level;
    }

    /**
     * Returns a formatted string representation of the directory entry.
     *
     * <p>The format includes:<br>
     * - Indentation based on directory level<br>
     * - [DELETED] marker in red/yellow if isDeleted is true<br>
     * - File or directory name<br>
     * - Starting cluster number<br>
     * - File size in bytes</p>
     *
     * @return formatted string like "  [DELETED] filename | Cluster: 5 | Size: 2048"
     */
    @Override
    public String toString() {
        return "  ".repeat(Math.max(0, this.level)) +
                (isDeleted ? "\u001b[31;43m[DELETED]\u001b[0m " : "") +
                this.fileName +
                " | Cluster: " +
                this.startCluster +
                " | Size: " +
                this.fileSize;
    }
}