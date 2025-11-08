package ca.concordia.filesystem.datastructures;

import java.util.LinkedList;

public class FEntry {

    private String filename; //WW - less than 11 bytes
    private short filesize; //WW - size of the actual file. Note that a file might not use all of the space in the blocks allocated to it. 0 initially
    private short firstBlock; // Pointers to data blocks. WW - index to the array of FNodes, points to first block of the file. -1 initially

    public FEntry(String filename, short filesize, short firstblock) throws IllegalArgumentException{
        //Check filename is max 11 bytes long
        if (filename.length() > 11) {
            throw new IllegalArgumentException("Filename cannot be longer than 11 characters.");
        }
        this.filename = filename;
        this.filesize = filesize;
        this.firstBlock = firstblock;
    }

    // Getters and Setters
    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        if (filename.length() > 11) {
            throw new IllegalArgumentException("Filename cannot be longer than 11 characters.");
        }
        this.filename = filename;
    }

    public short getFilesize() {
        return filesize;
    }

    public void setFilesize(short filesize) {
        if (filesize < 0) {
            throw new IllegalArgumentException("Filesize cannot be negative.");
        }
        this.filesize = filesize;
    }

    public short getFirstBlock() {
        return firstBlock;
    }
}
