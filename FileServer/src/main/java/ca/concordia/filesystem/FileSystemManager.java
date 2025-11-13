package ca.concordia.filesystem;

import ca.concordia.filesystem.datastructures.FEntry;

import java.io.RandomAccessFile;
import java.io.IOException;
import java.util.concurrent.locks.ReentrantLock;

public class FileSystemManager {

	private final int MAXFILES = 5;
    private final int MAXBLOCKS = 10;
    private static FileSystemManager instance;
    private final RandomAccessFile disk;
    private final ReentrantLock globalLock = new ReentrantLock();

    private static final int BLOCK_SIZE = 128; // Example block size

    private FEntry[] inodeTable; // Array of inodes
    private boolean[] freeBlockList; // Bitmap for free blocks

    public static synchronized FileSystemManager getInstance (String filename, int totalSize) throws IOException {
        // Initialize the file system manager with a file
        if(instance == null) {
        	instance = new FileSystemManager(filename, totalSize);
        } 
        return instance;
    }



    private FileSystemManager(String filename, int totalSize) throws IOException {

        this.disk = new RandomAccessFile(filename, "rw");
        this.inodeTable = new FEntry[MAXFILES];
        this.freeBlockList = new boolean[MAXBLOCKS];
    }

    public void createFile(String fileName) throws Exception {

        throw new UnsupportedOperationException("Method not implemented yet.");
    }


}