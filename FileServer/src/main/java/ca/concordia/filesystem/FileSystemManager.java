package ca.concordia.filesystem;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import ca.concordia.filesystem.datastructures.FEntry;
import ca.concordia.filesystem.datastructures.FNode;

public class FileSystemManager {

    private final int MAXFILES = 5;
    private final int MAXBLOCKS = 10;
    private final int BLOCK_SIZE = 128;

    private static FileSystemManager instance;
    private final RandomAccessFile disk;
    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    private final FEntry[] fEntryTable; // Array of inodes
    private final FNode[] fNodeTable;
    private final boolean[] freeBlockList; // Bitmap for free blocks

    private FileSystemManager(String filename) throws IOException {
        this.disk = new RandomAccessFile(filename, "rw");
        this.disk.setLength((long) MAXBLOCKS * BLOCK_SIZE); // Pre-allocate file size

        this.fEntryTable = new FEntry[MAXFILES];
        this.fNodeTable = new FNode[MAXBLOCKS]; // One FNode per potential block
        this.freeBlockList = new boolean[MAXBLOCKS];

        for (int i = 0; i < MAXBLOCKS; i++) {
            freeBlockList[i] = true;
            fNodeTable[i] = new FNode(-1); // Initialize all FNodes
        }
    }

    // Singleton getInstance method
    public static synchronized FileSystemManager getInstance(String filename) throws IOException {
        if (instance == null) {
            instance = new FileSystemManager(filename);
        }
        return instance;
    }

    // CREATE (create a new file with given name)
    public void createFile(String fileName) throws Exception {
        lock.writeLock().lock(); // Acquire global lock to ensure thread safety
        try {
            // Check filename length
            if (fileName.length() > 11) {
                throw new Exception("ERROR: filename too large");
            }

            // Check if file already exists
            for (FEntry entry : fEntryTable) {
                if (entry != null && entry.getFilename().equals(fileName)) {
                    throw new Exception("ERROR: file " + fileName + " already exists");
                }
            }

            // Find a free inode slot
            int freeFEntryIndex = -1;
            for (int i = 0; i < MAXFILES; i++) {
                if (fEntryTable[i] == null) {
                    freeFEntryIndex = i;
                    break;
                }
            }

            // Couldn't find a free node
            if (freeFEntryIndex == -1) {
                throw new Exception("ERROR: Maximum number of files reached");
            }

            fEntryTable[freeFEntryIndex] = new FEntry(fileName, (short) 0, (short) -1);
        } finally {
            lock.writeLock().unlock(); // release lock in all cases
        }
    }

    // LIST (return list of all filenames in filesystem, if none return empty array)
    public String[] listFiles() {
        lock.readLock().lock(); // thread safety when reading inode table
        try {
            List<String> files = new ArrayList<>();
            for (FEntry entry : fEntryTable) {
                if (entry != null) {
                    files.add(entry.getFilename());
                }
            }
            return files.toArray(new String[0]);
        } finally {
            lock.readLock().unlock();
        }
    }

    // READ
    public byte[] readFile(String fileName) throws Exception {
        lock.readLock().lock();
        try {
            FEntry target = null;
            for (FEntry entry : fEntryTable) {
                if (entry != null && entry.getFilename().equals(fileName)) {
                    target = entry;
                    break;
                }
            }

            if (target == null) {
                throw new Exception("ERROR: file " + fileName + " does not exist");
            }

            int size = target.getFilesize();
            if (size == 0) {
                return new byte[0];
            }

            byte[] buffer = new byte[size];
            int bytesRead = 0;
            int currentFNodeIndex = target.getFirstBlock();

            while(currentFNodeIndex != -1) {
                int blockIndex = fNodeTable[currentFNodeIndex].getBlockIndex();
                disk.seek((long) blockIndex * BLOCK_SIZE);

                int toRead = Math.min(BLOCK_SIZE, size - bytesRead);
                disk.read(buffer, bytesRead, toRead);
                bytesRead += toRead;

                currentFNodeIndex = fNodeTable[currentFNodeIndex].getNext();
            }

            return buffer;
        } finally {
            lock.readLock().unlock();
        }
    }

    // WRITE (write data to an existing file)
    public void writeFile(String fileName, byte[] contents) throws Exception {
        lock.writeLock().lock(); // Ensure thread safety
        List<Integer> allocatedBlocks = new ArrayList<>();
        try {
            // Find the file
            FEntry target = null;
            for (FEntry entry : fEntryTable) {
                if (entry != null && entry.getFilename().equals(fileName)) {
                    target = entry;
                    break;
                }
            }

            if (target == null) {
                throw new Exception("ERROR: file " + fileName + " does not exist");
            }

            int totalBytes = contents.length;
            int blocksNeeded = (int) Math.ceil((double) totalBytes / BLOCK_SIZE);

            // Check free blocks
            int freeCount = 0;
            for (boolean free : freeBlockList) {
                if (free) freeCount++;
            }
            if (blocksNeeded > freeCount) {
                throw new Exception("ERROR: file too large");
            }

            // Allocate blocks before writing
            for (int i = 0; i < MAXBLOCKS && allocatedBlocks.size() < blocksNeeded; i++) {
                if (freeBlockList[i]) {
                    allocatedBlocks.add(i);
                }
            }

            if (allocatedBlocks.size() < blocksNeeded) {
                throw new Exception("ERROR: file too large");
            }

            // Write data to allocated blocks
            int bytesWritten = 0;
            for (int i = 0; i < allocatedBlocks.size(); i++) {
                int blockIndex = allocatedBlocks.get(i);
                int startOffset = i * BLOCK_SIZE;
                int bytesLeft = Math.min(BLOCK_SIZE, totalBytes - startOffset);

                disk.seek(blockIndex * BLOCK_SIZE);
                disk.write(contents, startOffset, bytesLeft);
                freeBlockList[blockIndex] = false;
                bytesWritten += bytesLeft;
            }

            // Free old blocks only after successful write
            short oldBlock = target.getFirstBlock();
            if (oldBlock >= 0 && oldBlock < MAXBLOCKS) {
                freeBlockList[oldBlock] = true;
            }

            // Update metadata
            target.setFirstBlock((short) (int) allocatedBlocks.get(0));
            target.setFilesize((short) totalBytes);

        } catch (Exception e) {
            // Revert all allocated blocks on failure
            for (int block : allocatedBlocks) {
                freeBlockList[block] = true;
                disk.seek(block * BLOCK_SIZE);
                disk.write(new byte[BLOCK_SIZE]);
            }
            throw e;
        } finally {
            lock.writeLock().unlock();
        }
    }

    // DELETE (delete a file by name, free its allocated blocks, overwrite to zeros)
    public void deleteFile(String fileName) throws Exception {
        lock.writeLock().lock(); // Ensure thread safety
        try {
            // Find file
            int fEntryIndex = -1;
            for (int i = 0; i < MAXFILES; i++) {
                if (fEntryTable[i] != null && fEntryTable[i].getFilename().equals(fileName)) {
                    fEntryIndex = i;
                    break;
                }
            }

            // Couldn't find the file
            if (fEntryIndex == -1) {
                throw new Exception("ERROR: file " + fileName + " does not exist");
            }

            // Free all associated blocks and FNodes
            freeFileBlocks(fEntryIndex);

            fEntryTable[fEntryIndex] = null;
        } finally {
            lock.writeLock().unlock();
        }
    }

    private void freeFileBlocks(int fEntryIndex) throws IOException {
        FEntry entry = fEntryTable[fEntryIndex];
        if (entry == null || entry.getFirstBlock() == -1) {
            return; // No blocks to free
        }

        int currentFNodeIndex = entry.getFirstBlock();
        byte[] zeros = new byte[BLOCK_SIZE];

        while(currentFNodeIndex != -1) {
            int blockIndex = fNodeTable[currentFNodeIndex].getBlockIndex();

            // Overwrite data with zeroes
            disk.seek((long) blockIndex * BLOCK_SIZE);
            disk.write(zeros);

            // Mark block as free
            freeBlockList[blockIndex] = true;

            int nextFNodeIndex = fNodeTable[currentFNodeIndex].getNext();

            // Reset and free FNode
            fNodeTable[currentFNodeIndex].setNext(-1);
            fNodeTable[currentFNodeIndex].setBlockIndex(-1);

            currentFNodeIndex = nextFNodeIndex;
        }

        // Update FEntry to reflect cleared blocks
        entry.setFirstBlock((short) -1);
        entry.setFilesize((short) 0);
    }
}