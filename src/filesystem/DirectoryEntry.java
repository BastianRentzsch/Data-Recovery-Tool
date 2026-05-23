package filesystem;

public class DirectoryEntry {

    public String fileName;
    public long startCluster;
    public long fileSize; // Stores the file size in bytes
    public boolean isDeleted;
    public boolean isDirectory;
    public int level; // Stores the directory depth level for formatted output

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

    @Override
    public String toString() {

        // Creates a formatted string representation of the entry
        return "  ".repeat(Math.max(0, this.level)) +
                (isDeleted ? "\u001b[31;43m[DELETED]\u001b[0m " : "") + // Adds a deleted marker if the entry is deleted
                this.fileName +
                " | Cluster: " +
                this.startCluster +
                " | Size: " +
                this.fileSize;


    }
}
