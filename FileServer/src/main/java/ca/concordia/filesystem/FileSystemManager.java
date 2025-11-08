package ca.concordia.filesystem;

import ca.concordia.filesystem.datastructures.FEntry;
import ca.concordia.filesystem.datastructures.FNode;

import java.io.ObjectOutputStream;
import java.io.RandomAccessFile;
import java.util.concurrent.locks.ReentrantLock;

//WW - Files have 2 types of metadata: file entries (FEntry.java) and file nodes (FNode.java)
//WW - FEntry stores filename, filesize, pointer to first block
//WW - FNode stores block index and pointer to next FNode if file spans multiple blocks

//WW - FileSystemManager is initialized once with a file representing the disk
//WW - It manages file creation, deletion, reading, writing, and listing files
//WW - It uses locking to ensure thread safety

//WW - The disk file is accessed using RandomAccessFile for reading and writing data blocks
//WW - the disk file is divided into fixed-size blocks (e.g., 128 bytes each)
//WW - the disk file is initialized with the following elements: inode table, free block list, data blocks

public class FileSystemManager {

    //WW - (16bytes per FEntry) * (5 files) = 80 bytes for inode table
    //WW - (10 blocks) * (4 bits per FNode) = 40 bytes for FNodes
    //WW - 120 bytes for metadata < 128 bytes
    //WW - (10 blocks) * (128 bytes per block) = 1280 bytes for data blocks
    private final int MAXFILES = 5;
    private final int MAXBLOCKS = 10;
    private static final int BLOCK_SIZE = 128; // Example block size

    private final static FileSystemManager instance; //WW - Singleton instance

    private final RandomAccessFile disk; //WW - File representing the disk
    private final ReentrantLock globalLock = new ReentrantLock(); //WW - Global mutex lock for thread safety

    private FEntry[] inodeTable; // Array of inodes
    private FNode[] fnodeTable; // Array of FNodes
    //WW - if freeBlockList is stored in metadata on disk, then metadata size increases to 130 bytes/128 bytes
    private boolean[] freeBlockList; // Bitmap for free blocks WW - NOT SURE IF STORED IN METADATA OR JUST IN MEMORY
    private int totalSize; // Total size of the file system

    //WW - Singleton pattern
    public static FileSystemManager getInstance() {
        return instance;
    }

    public FileSystemManager(String filename, int totalSize){ //WW - maybe add throws IOException
        // Initialize the file system manager with a file
        if(instance == null) {
            //TODO Initialize the file system

            //WW - 1. Create or open the file representing the disk
            /*"rws"	Open for reading and writing, as with "rw", and also require that every update to the file's content or metadata be written synchronously to the underlying storage device.
            "rwd"  	Open for reading and writing, as with "rw", and also require that every update to the file's content be written synchronously to the underlying storage device. */
            try(RandomAccessFile disk = new RandomAccessFile(filename, "rw")){
                this.disk = disk;
                this.totalSize = totalSize;

                // Set the file size
                disk.setLength(totalSize);

            } catch (Exception e) {
                throw new RuntimeException("Failed to initialize FileSystemManager: " + e.getMessage());
            }

            //WW - 2. Initialize inode table and free block list
            FEntry[] inodeTable = new FEntry[MAXFILES];
            FNode[] fnodeTable = new FNode[MAXBLOCKS];
            boolean[] freeBlockList = new boolean[MAXBLOCKS];

            for (int i = 0; i < MAXFILES; i++) {
                inodeTable[i] = new FEntry("", (short)0, (short)-1); // Initialize empty FEntries
            }

            for (int i = 0; i < MAXBLOCKS; i++) {
                fnodeTable[i] = new FNode(-i); // Initialize all FNodes with NEGATIVE blockIndex to indicate free
            }

            for (int i = 0; i < MAXBLOCKS; i++) {
                freeBlockList[i] = true; // All blocks are free initially
            }

            this.inodeTable = inodeTable;
            this.fnodeTable = fnodeTable;
            this.freeBlockList = freeBlockList;

            //WW - Write metadata to disk
            try {
                writeMetadataToDisk(inodeTable, fnodeTable, freeBlockList);
            } catch (Exception e) {
                throw new RuntimeException("Failed to write initial metadata to disk: " + e.getMessage());
            }

            //WW - 3. Set up data blocks??

            //WW - 4. Assign instance
            try{
                instance = new FileSystemManager(filename, totalSize);
                //instance = this;
            } catch (Exception e) {
                throw new RuntimeException("Failed to create FileSystemManager instance: " + e.getMessage());
            }
            
        } else {
            throw new IllegalStateException("FileSystemManager is already initialized.");
        }

    }

    public void createFile(String fileName) throws Exception {
        // TODO
        //WW - 1. Acquire global lock
        //WW - 2. Check if file with same name exists
        //WW - 3. Find free inode in inode table
        //WW - 4. Allocate first free block from free block list
        //WW - 5. Update inode table and free block list
        //WW - 6. Write updated metadata to disk
        //WW - 7. Release global lock
    }

    //WW - TODO: Add deleteFile

    //WW - TODO: Add readFile

    //WW - TODO: Add writeFile

    //WW - TODO: Add listAllFiles

    //WW - TODO: Add helper methods for block allocation, deallocation, and metadata management

    //WW - Write metadata to disk
    private void writeMetadataToDisk(Fentry[] inodeTable, FNode[] fnodeTable, boolean[] freeBlockList) throws Exception {
        try {
            disk.seek(0);
            //WW - Write inode table
            for (FEntry entry : inodeTable) {
                // Serialize and write each FEntry
                ObjectOutputStream oos = new ObjectOutputStream(disk);
                oos.writeObject(entry);
                oos.flush();
            }

            //WW - Write FNode table
            for (FNode node : fnodeTable) {
                // Serialize and write each FNode
                ObjectOutputStream oos = new ObjectOutputStream(disk);
                oos.writeObject(node);
                oos.flush();
            }

            /*  //WW - Write free block list
            for (boolean isFree : freeBlockList) {
                // Serialize and write each boolean
                ObjectOutputStream oos = new ObjectOutputStream(disk);
                oos.writeBoolean(isFree);
                oos.flush();
            } */
        } catch (Exception e) {
            throw new Exception("Failed to write metadata to disk: " + e.getMessage());
        }

    }

    //WW - Read metadata from disk
    private void readMetadataFromDisk() throws Exception {
        try {
            disk.seek(0);
            //WW - Read inode table
            for (int i = 0; i < MAXFILES; i++) {
                // Deserialize and read each FEntry
                ObjectInputStream ois = new ObjectInputStream(disk);
                inodeTable[i] = (FEntry) ois.readObject();
                ois.close();
            }

            //WW - Read FNode table
            for (int i = 0; i < MAXBLOCKS; i++) {
                // Deserialize and read each FNode
                ObjectInputStream ois = new ObjectInputStream(disk);
                fnodeTable[i] = (FNode) ois.readObject();
                ois.close();
            }

            /*//WW - Read free block list
            for (int i = 0; i < MAXBLOCKS; i++) {
                // Deserialize and read each boolean
                ObjectInputStream ois = new ObjectInputStream(disk);
                freeBlockList[i] = ois.readBoolean();
                ois.close();
            } */
        } catch (Exception e) {
            throw new Exception("Failed to read metadata from disk: " + e.getMessage());
        }
    }
}
