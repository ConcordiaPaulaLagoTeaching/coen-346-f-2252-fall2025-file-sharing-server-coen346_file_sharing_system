package ca.concordia.filesystem.datastructures;

import java.io.Serializable;

/**
 * FEntry represents a file entry in the file system.
 * Each FEntry contains metadata about a file stored in the system.
 * 
 * Structure:
 * - filename: max 11 characters
 * - filesize: actual size of file content (may be less than allocated blocks)
 * - firstBlock: index to the first FNode in the linked list of blocks
 */
public class FEntry implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private String filename;    // Max 11 characters
    private short filesize;     // Size of actual file content in bytes
    private short firstBlock;   // Index into FNode array (-1 if file is empty)

    /**
     * Constructor for FEntry
     * @param filename Name of the file (max 11 characters)
     * @param filesize Size of the file in bytes
     * @param firstBlock Index of first FNode, -1 if no blocks allocated
     */
    public FEntry(String filename, short filesize, short firstBlock) {
        if (filename.length() > 11) {
            throw new IllegalArgumentException("Filename cannot be longer than 11 characters.");
        }
        this.filename = filename;
        this.filesize = filesize;
        this.firstBlock = firstBlock;
    }

    // Getters
    public String getFilename() {
        return filename;
    }

    public short getFilesize() {
        return filesize;
    }

    public short getFirstBlock() {
        return firstBlock;
    }

    // Setters
    public void setFilename(String filename) {
        if (filename.length() > 11) {
            throw new IllegalArgumentException("Filename cannot be longer than 11 characters.");
        }
        this.filename = filename;
    }

    public void setFilesize(short filesize) {
        if (filesize < 0) {
            throw new IllegalArgumentException("Filesize cannot be negative.");
        }
        this.filesize = filesize;
    }

    public void setFirstBlock(short firstBlock) {
        this.firstBlock = firstBlock;
    }

    /**
     * Check if this FEntry is free (empty/unused)
     * @return true if this entry is not being used
     */
    public boolean isFree() {
        return filename.isEmpty();
    }

    @Override
    public String toString() {
        return String.format("FEntry{filename='%s', size=%d, firstBlock=%d}", 
                           filename, filesize, firstBlock);
    }
}
