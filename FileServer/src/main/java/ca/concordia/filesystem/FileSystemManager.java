package ca.concordia.filesystem;

import ca.concordia.filesystem.datastructures.FEntry;

import java.io.RandomAccessFile;
import java.util.concurrent.locks.ReentrantLock;

public class FileSystemManager {

    private final int MAXFILES = 5;
    private final int MAXBLOCKS = 10;
    private static final int BLOCK_SIZE = 128; // Example block size

    private final static FileSystemManager instance; //WW - redline because final, but not initialized in FileSystemManager()

    private final RandomAccessFile disk; //WW - File representing the disk
    private final ReentrantLock globalLock = new ReentrantLock(); //WW - Global mutex lock for thread safety

    private FEntry[] inodeTable; // Array of inodes
    private boolean[] freeBlockList; // Bitmap for free blocks

    //WW - Files have 2 types of metadata: file entries (FEntry.java) and file nodes (FNode.java)
    //WW - FEntry stores filename, filesize, pointer to first block
    //WW - FNode stores block index and pointer to next FNode if file spans multiple blocks

    //WW - FileSystemManager is initialized once with a file representing the disk
    //WW - It manages file creation, deletion, reading, writing, and listing files
    //WW - It uses locking to ensure thread safety
    
    //WW - The disk file is accessed using RandomAccessFile for reading and writing data blocks
    //WW - the disk file is divided into fixed-size blocks (e.g., 128 bytes each)
    //WW - the disk file is initialized with the following elements: inode table, free block list, data blocks

    public FileSystemManager(String filename, int totalSize) {
        // Initialize the file system manager with a file
        if(instance == null) {
            //TODO Initialize the file system
            
            
        } else {
            throw new IllegalStateException("FileSystemManager is already initialized.");
        }

    }

    public void createFile(String fileName) throws Exception {
        // TODO
        throw new UnsupportedOperationException("Method not implemented yet.");
    }

    //WW - TODO: Add deleteFile

    //WW - TODO: Add readFile

    //WW - TODO: Add writeFile

    //WW - TODO: Add listAllFiles
}
