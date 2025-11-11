package ca.concordia.filesystem;

import ca.concordia.filesystem.datastructures.FEntry;
import ca.concordia.filesystem.datastructures.FNode;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

public class FileSystemManager {

    private static FileSystemManager instance;

    private final RandomAccessFile disk;
    private final ReentrantLock globalLock = new ReentrantLock();

    private final int MAXFILES = 5;
    private final int MAXBLOCKS = 10;
    private final int BLOCK_SIZE = 128;

    private final FEntry[] fEntries;
    private final FNode[] fNodes;
    private final boolean[] freeBlockList;

    // Constructor is private to enforce singleton
    private FileSystemManager(String filename, int totalSize) throws IOException {
        this.disk = new RandomAccessFile(filename, "rw");

        fEntries = new FEntry[MAXFILES];
        fNodes = new FNode[MAXBLOCKS];
        freeBlockList = new boolean[MAXBLOCKS];

        // initialize file entries and nodes
        for (int i = 0; i < MAXFILES; i++) {
            fEntries[i] = null; // empty slot = null
        }

        for (int i = 0; i < MAXBLOCKS; i++) {
            fNodes[i] = null;
            freeBlockList[i] = true;
        }

        // block 0 reserved for metadata
        freeBlockList[0] = false;
    }

    public static FileSystemManager getInstance(String filename, int totalSize) throws IOException {
        if (instance == null) {
            instance = new FileSystemManager(filename, totalSize);
        }
        return instance;
    }

    // Helper methods
    // 

    private int findFreeEntry() {
        for (int i = 0; i < MAXFILES; i++) {
            if (fEntries[i] == null) return i;
        }
        return -1;
    }

    private int findFreeBlock() {
        for (int i = 1; i < MAXBLOCKS; i++) {
            if (freeBlockList[i]) return i;
        }
        return -1;
    }

    private int lookupFile(String filename) {
        for (int i = 0; i < MAXFILES; i++) {
            if (fEntries[i] != null && fEntries[i].getFilename().equals(filename)) {
                return i;
            }
        }
        return -1;
    }



    public void createFile(String filename) throws Exception {
        globalLock.lock();
        try {
            if (filename.length() > 11)
                throw new IllegalArgumentException("ERROR: filename too large");

            if (lookupFile(filename) != -1)
                throw new IllegalStateException("ERROR: file already exists");

            int entryIndex = findFreeEntry();
            if (entryIndex == -1)
                throw new IllegalStateException("ERROR: maximum file count reached");

            // create an empty entry
            fEntries[entryIndex] = new FEntry(filename, (short) 0, (short) -1);

            System.out.println("Created file: " + filename);
        } finally {
            globalLock.unlock();
        }
    }

    public void deleteFile(String filename) throws Exception {
        globalLock.lock();
        try {
            int idx = lookupFile(filename);
            if (idx == -1)
                throw new IllegalArgumentException("ERROR: file " + filename + " does not exist");

            FEntry entry = fEntries[idx];
            short firstBlock = entry.getFirstBlock();

            // free all linked blocks if any
            int current = firstBlock;
            while (current != -1 && current < fNodes.length && fNodes[current] != null) {
                FNode node = fNodes[current];
                freeBlockList[nodeBlockIndex(node)] = true;
                int next = nodeNext(node);
                fNodes[current] = null;
                current = next;
            }

            // remove file entry
            fEntries[idx] = null;
            System.out.println("Deleted file: " + filename);

        } finally {
            globalLock.unlock();
        }
    }

    public List<String> listFiles() {
        List<String> files = new ArrayList<>();
        for (FEntry e : fEntries) {
            if (e != null)
                files.add(e.getFilename() + " (" + e.getFilesize() + " bytes)");
        }
        return files;
    }

    // Helper accessors for FNode private fields
    
    private int nodeBlockIndex(FNode node) {
        try {
            var field = FNode.class.getDeclaredField("blockIndex");
            field.setAccessible(true);
            return field.getInt(node);
        } catch (Exception e) {
            return -1;
        }
    }

    private int nodeNext(FNode node) {
        try {
            var field = FNode.class.getDeclaredField("next");
            field.setAccessible(true);
            return field.getInt(node);
        } catch (Exception e) {
            return -1;
        }
    }

    // Debug utility
   
    public void printState() {
        System.out.println("\n===== FILE SYSTEM STATE =====");
        for (int i = 0; i < fEntries.length; i++) {
            System.out.println("[" + i + "] " + (fEntries[i] == null ? "EMPTY" : fEntries[i].getFilename()));
        }
        System.out.print("Free blocks: ");
        for (int i = 0; i < freeBlockList.length; i++) {
            if (freeBlockList[i]) System.out.print(i + " ");
        }
        System.out.println("\n=============================\n");
    }
}
