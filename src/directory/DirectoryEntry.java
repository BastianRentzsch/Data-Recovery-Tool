package directory;

public class DirectoryEntry {
    public String fileName;
    public long startCluster;
    public long fileSize;
    public boolean isDeleted;
    public boolean isDirectory;
    public int level;

    public DirectoryEntry(String fileName, long startCluster, long fileSize,
                          boolean isDeleted, boolean isDirectory, int level) {
        this.fileName = fileName;
        this.startCluster = startCluster;
        this.fileSize = fileSize;
        this.isDeleted = isDeleted;
        this.isDirectory = isDirectory;
        this.level = level;
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();

        // Indentations for the different levels of the directories
        result.repeat("  ", Math.max(0, this.level));

        result.append(isDeleted ? "\u001b[31;43m[DELETED]\u001b[0m " : "")
                .append(this.fileName)
                .append(" | Cluster: ")
                .append(this.startCluster)
                .append(" | Size: ")
                .append(this.fileSize);
        return result.toString();
    }
}
