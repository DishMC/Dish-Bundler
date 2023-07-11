package net.ouja.bundler;

public class FileEntry {
    private final String hash;

    private final String id;

    private final String path;

    private FileEntry(String hash, String id, String path) {
        this.hash = hash;
        this.id = id;
        this.path = path;
    }

    public String hash() {
        return this.hash;
    }

    public String id() {
        return this.id;
    }

    public String path() {
        return this.path;
    }

    public static FileEntry parseLine(String line) {
        String[] fields = line.split("\t");
        if (fields.length != 3)
            throw new IllegalStateException("Malformed library entry: " + line);
        return new FileEntry(fields[0], fields[1], fields[2]);
    }
}
