package ca.concordia.filesystem;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import ca.concordia.filesystem.datastructures.FEntry;
import ca.concordia.filesystem.datastructures.FNode;

/**
 * FileSystemManager manages a simulated file system stored in a single file.
 * 
 * File System Layout:
 * [FEntry Array] [FNode Array] [Wasted Bytes] [Data Blocks...]
 * 
 * - FEntry Array: Metadata for each file (filename, size, first block)
 * - FNode Array: Block allocation info (linked list of blocks per file)
 * - Data Blocks: Actual file content
 * 
 * Thread Safety:
 * - Uses ReentrantReadWriteLock for synchronization
 * - Multiple readers can access simultaneously
 * - Only one writer at a time
 */
public class FileSystemManager {
    
    // Configuration constants
    private final int MAXFILES;
    private final int MAXBLOCKS;
    private final int BLOCK_SIZE;
    
    // File system structures
    private final RandomAccessFile disk;
    private final FEntry[] fentryTable;
    private final FNode[] fnodeTable;
    
    // Synchronization
    private final ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock(true); // Fair lock
    
    // File system layout information
    private final int metadataSize;      // Size of FEntry + FNode arrays in bytes
    private final int metadataBlocks;    // Number of blocks used for metadata
    private final int dataBlockStart;    // Starting block index for file data
    
    // Singleton instance
    private static FileSystemManager instance;

    // Private constructor for singleton pattern
    private FileSystemManager(String filename, int totalSize) throws IOException {

        //SET DEFAULT VALUES HERE
        this.MAXFILES = 8;
        this.MAXBLOCKS = 16;
        this.BLOCK_SIZE = 128;
        
        // Open or create the disk file
        this.disk = new RandomAccessFile(filename, "rws");
        disk.setLength(totalSize);
        
        // Initialize data structures
        this.fentryTable = new FEntry[MAXFILES];
        this.fnodeTable = new FNode[MAXBLOCKS];
        
        // Calculate metadata layout
        int fentrySize = MAXFILES * 16;  // Each FEntry is approximately 16 bytes
        int fnodeSize = MAXBLOCKS * 8;   // Each FNode is approximately 8 bytes
        this.metadataSize = fentrySize + fnodeSize;
        this.metadataBlocks = (int) Math.ceil((double) metadataSize / BLOCK_SIZE);
        this.dataBlockStart = metadataBlocks;
        
        // Initialize or load file system
        if (disk.length() == totalSize && totalSize > 0) {
            // Try to load existing file system
            try {
                loadMetadata();
            } catch (Exception e) {
                // If loading fails, initialize fresh
                initializeFileSystem();
            }
        } else {
            initializeFileSystem();
        }
    }

    //Get singleton instance of FileSystemManager
    public static synchronized FileSystemManager getInstance(String filename, int totalSize) throws IOException {
        if (instance == null) {
            instance = new FileSystemManager(filename, totalSize);
        }
        return instance;
    }

    // Initialize a fresh file system
    private void initializeFileSystem() throws IOException {
        // Initialize FEntry table (all empty)
        for (int i = 0; i < MAXFILES; i++) {
            fentryTable[i] = new FEntry("", (short) 0, (short) -1);
        }
        
        // Initialize FNode table (all free)
        for (int i = 0; i < MAXBLOCKS; i++) {
            fnodeTable[i] = new FNode(-(i + 1));  // -(i+1) indicates free block i
        }
        
        // Write metadata to disk
        saveMetadata();
        
        // Zero out data blocks
        byte[] zeroBlock = new byte[BLOCK_SIZE];
        for (int i = dataBlockStart; i < MAXBLOCKS; i++) {
            writeDataBlock(i, zeroBlock, 0, BLOCK_SIZE);
        }
    }

    // Save metadata (FEntry and FNode tables) to disk
    private void saveMetadata() throws IOException {
        disk.seek(0);
        
        // Write FEntry table
        for (FEntry entry : fentryTable) {
            writeUTF11(disk, entry.getFilename());
            disk.writeShort(entry.getFilesize());
            disk.writeShort(entry.getFirstBlock());
        }
        
        // Write FNode table
        for (FNode node : fnodeTable) {
            disk.writeInt(node.getBlockIndex());
            disk.writeInt(node.getNextBlock());
        }
    }

    // Load metadata from disk
    private void loadMetadata() throws IOException {
        disk.seek(0);
        
        // Read FEntry table
        for (int i = 0; i < MAXFILES; i++) {
            String filename = readUTF11(disk);
            short filesize = disk.readShort();
            short firstBlock = disk.readShort();
            fentryTable[i] = new FEntry(filename, filesize, firstBlock);
        }
        
        // Read FNode table
        for (int i = 0; i < MAXBLOCKS; i++) {
            int blockIndex = disk.readInt();
            int nextBlock = disk.readInt();
            fnodeTable[i] = new FNode(blockIndex);
            fnodeTable[i].setNextBlock(nextBlock);
        }
    }

    /**
     * Create a new empty file
     * @param filename Name of file to create (max 11 characters)
     * @throws Exception if file already exists or no space available
     */
    public void createFile(String filename) throws Exception {
        // Validate filename
        if (filename == null || filename.isEmpty()) {
            throw new IllegalArgumentException("Filename cannot be empty");
        }
        if (filename.length() > 11) {
            throw new IllegalArgumentException("ERROR: filename too large");
        }
        
        rwLock.writeLock().lock();
        try {
            // Check if file already exists
            for (FEntry entry : fentryTable) {
                if (entry.getFilename().equals(filename)) {
                    throw new Exception("ERROR: file " + filename + " already exists");
                }
            }
            
            // Find free FEntry
            int freeEntryIndex = -1;
            for (int i = 0; i < MAXFILES; i++) {
                if (fentryTable[i].isFree()) {
                    freeEntryIndex = i;
                    break;
                }
            }
            
            if (freeEntryIndex == -1) {
                throw new Exception("ERROR: maximum number of files reached");
            }
            
            // Create the file entry (empty file, no blocks allocated yet)
            fentryTable[freeEntryIndex] = new FEntry(filename, (short) 0, (short) -1);
            
            // Save metadata
            saveMetadata();
            
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    /**
     * Delete a file and free its blocks
     * @param filename Name of file to delete
     * @throws Exception if file does not exist
     */
    public void deleteFile(String filename) throws Exception {
        if (filename == null || filename.isEmpty()) {
            throw new IllegalArgumentException("Filename cannot be empty");
        }
        
        rwLock.writeLock().lock();
        try {
            // Find the file
            int entryIndex = findFileEntry(filename);
            if (entryIndex == -1) {
                throw new Exception("ERROR: file " + filename + " does not exist");
            }
            
            FEntry entry = fentryTable[entryIndex];
            short currentBlockIndex = entry.getFirstBlock();
            
            // Free all blocks and zero them out
            while (currentBlockIndex != -1) {
                FNode currentNode = fnodeTable[currentBlockIndex];
                int nextBlockIndex = currentNode.getNextBlock();
                
                // Zero out the data block
                byte[] zeroBlock = new byte[BLOCK_SIZE];
                writeDataBlock(currentBlockIndex, zeroBlock, 0, BLOCK_SIZE);
                
                // Mark FNode as free
                currentNode.markFree();
                
                currentBlockIndex = (short) nextBlockIndex;
            }
            
            // Clear the FEntry
            fentryTable[entryIndex] = new FEntry("", (short) 0, (short) -1);
            
            // Save metadata
            saveMetadata();
            
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    /**
     * Write content to a file (overwrites existing content)
     * @param filename Name of file to write
     * @param contents Byte array of content to write
     * @throws Exception if file doesn't exist or not enough space
     */
    public void writeFile(String filename, byte[] contents) throws Exception {
        if (filename == null || filename.isEmpty()) {
            throw new IllegalArgumentException("Filename cannot be empty");
        }
        if (contents == null) {
            contents = new byte[0];
        }
        
        rwLock.writeLock().lock();
        try {
            // Find the file
            int entryIndex = findFileEntry(filename);
            if (entryIndex == -1) {
                throw new Exception("ERROR: file " + filename + " does not exist");
            }
            
            // Calculate blocks needed
            int blocksNeeded = (int) Math.ceil((double) contents.length / BLOCK_SIZE);
            if (blocksNeeded == 0) {
                blocksNeeded = 0; // Empty file
            }
            
            // Check if we have enough free blocks (excluding metadata blocks)
            int availableBlocks = countFreeBlocks();
            if (blocksNeeded > availableBlocks) {
                throw new Exception("ERROR: file too large");
            }
            
            // Save current state for rollback
            FEntry entry = fentryTable[entryIndex];
            short oldFirstBlock = entry.getFirstBlock();
            short oldSize = entry.getFilesize();
            
            try {
                // Free old blocks first
                freeFileBlocks(oldFirstBlock);
                
                // Allocate new blocks and write content
                if (blocksNeeded > 0) {
                    short firstBlock = allocateBlocks(blocksNeeded);
                    writeContent(firstBlock, contents);
                    
                    // Update FEntry
                    entry.setFirstBlock(firstBlock);
                    entry.setFilesize((short) contents.length);
                } else {
                    // Empty file
                    entry.setFirstBlock((short) -1);
                    entry.setFilesize((short) 0);
                }
                
                // Save metadata
                saveMetadata();
                
            } catch (Exception e) {
                // Rollback on error
                entry.setFirstBlock(oldFirstBlock);
                entry.setFilesize(oldSize);
                throw e;
            }
            
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    /**
     * Read file contents
     * @param filename Name of file to read
     * @return Byte array of file contents
     * @throws Exception if file doesn't exist
     */
    public byte[] readFile(String filename) throws Exception {
        if (filename == null || filename.isEmpty()) {
            throw new IllegalArgumentException("Filename cannot be empty");
        }
        
        rwLock.readLock().lock();
        try {
            // Find the file
            int entryIndex = findFileEntry(filename);
            if (entryIndex == -1) {
                throw new Exception("ERROR: file " + filename + " does not exist");
            }
            
            FEntry entry = fentryTable[entryIndex];
            int fileSize = entry.getFilesize();
            
            if (fileSize == 0) {
                return new byte[0];
            }
            
            // Read content from blocks
            byte[] content = new byte[fileSize];
            int bytesRead = 0;
            short currentBlockIndex = entry.getFirstBlock();
            
            while (currentBlockIndex != -1 && bytesRead < fileSize) {
                int bytesToRead = Math.min(BLOCK_SIZE, fileSize - bytesRead);
                readDataBlock(currentBlockIndex, content, bytesRead, bytesToRead);
                bytesRead += bytesToRead;
                
                FNode currentNode = fnodeTable[currentBlockIndex];
                currentBlockIndex = (short) currentNode.getNextBlock();
            }
            
            return content;
            
        } finally {
            rwLock.readLock().unlock();
        }
    }

    /**
     * List all files in the file system
     * @return Array of filenames
     */
    public String[] listFiles() {
        rwLock.readLock().lock();
        try {
            List<String> files = new ArrayList<>();
            for (FEntry entry : fentryTable) {
                if (!entry.isFree()) {
                    files.add(entry.getFilename());
                }
            }
            return files.toArray(new String[0]);
        } finally {
            rwLock.readLock().unlock();
        }
    }

    // Helper methods

    // Find FEntry index by filename
    private int findFileEntry(String filename) {
        for (int i = 0; i < MAXFILES; i++) {
            if (fentryTable[i].getFilename().equals(filename)) {
                return i;
            }
        }
        return -1;
    }

    // Count available free blocks (excluding metadata blocks)

    private int countFreeBlocks() {
        int count = 0;
        for (int i = dataBlockStart; i < MAXBLOCKS; i++) {
            if (fnodeTable[i].isFree()) {
                count++;
            }
        }
        return count;
    }

    /**
     * Allocate a chain of blocks
     * @param numBlocks Number of blocks to allocate
     * @return Index of first block in chain
     */
    private short allocateBlocks(int numBlocks) throws Exception {
        if (numBlocks <= 0) {
            return -1;
        }
        
        short firstBlock = -1;
        short previousBlock = -1;
        int allocated = 0;
        
        for (int i = dataBlockStart; i < MAXBLOCKS && allocated < numBlocks; i++) {
            if (fnodeTable[i].isFree()) {
                fnodeTable[i].markInUse();
                
                if (firstBlock == -1) {
                    firstBlock = (short) i;
                } else {
                    fnodeTable[previousBlock].setNextBlock(i);
                }
                
                previousBlock = (short) i;
                allocated++;
            }
        }
        
        if (allocated < numBlocks) {
            throw new Exception("ERROR: file too large");
        }
        
        // Last block points to -1
        if (previousBlock != -1) {
            fnodeTable[previousBlock].setNextBlock(-1);
        }
        
        return firstBlock;
    }

    // Free all blocks in a file's block chain
    private void freeFileBlocks(short startBlock) throws IOException {
        short currentBlock = startBlock;
        
        while (currentBlock != -1) {
            FNode node = fnodeTable[currentBlock];
            short nextBlock = (short) node.getNextBlock();
            
            // Zero out data
            byte[] zeroBlock = new byte[BLOCK_SIZE];
            writeDataBlock(currentBlock, zeroBlock, 0, BLOCK_SIZE);
            
            // Mark as free
            node.markFree();
            
            currentBlock = nextBlock;
        }
    }

    // Write content across multiple blocks
    private void writeContent(short startBlock, byte[] content) throws IOException {
        int bytesWritten = 0;
        short currentBlock = startBlock;
        
        while (currentBlock != -1 && bytesWritten < content.length) {
            int bytesToWrite = Math.min(BLOCK_SIZE, content.length - bytesWritten);
            writeDataBlock(currentBlock, content, bytesWritten, bytesToWrite);
            bytesWritten += bytesToWrite;
            
            FNode node = fnodeTable[currentBlock];
            currentBlock = (short) node.getNextBlock();
        }
    }

    // Write data to a specific block
    private void writeDataBlock(int blockIndex, byte[] data, int offset, int length) throws IOException {
        long position = (long) blockIndex * BLOCK_SIZE;
        disk.seek(position);
        disk.write(data, offset, length);
    }

    // Read data from a specific block
    private void readDataBlock(int blockIndex, byte[] buffer, int offset, int length) throws IOException {
        long position = (long) blockIndex * BLOCK_SIZE;
        disk.seek(position);
        disk.readFully(buffer, offset, length);
    }

    // Write a fixed 11-character string to disk
    private void writeUTF11(RandomAccessFile file, String str) throws IOException {
        byte[] bytes = new byte[11];
        byte[] strBytes = str.getBytes("UTF-8");
        System.arraycopy(strBytes, 0, bytes, 0, Math.min(strBytes.length, 11));
        file.write(bytes);
    }

    // Read a fixed 11-character string from disk
    private String readUTF11(RandomAccessFile file) throws IOException {
        byte[] bytes = new byte[11];
        file.readFully(bytes);
        // Find null terminator or end
        int length = 0;
        while (length < 11 && bytes[length] != 0) {
            length++;
        }
        return new String(bytes, 0, length, "UTF-8");
    }

    //close
    public void close() throws IOException {
        rwLock.writeLock().lock();
        try {
            saveMetadata();
            disk.close();
        } finally {
            rwLock.writeLock().unlock();
        }
    }
}
