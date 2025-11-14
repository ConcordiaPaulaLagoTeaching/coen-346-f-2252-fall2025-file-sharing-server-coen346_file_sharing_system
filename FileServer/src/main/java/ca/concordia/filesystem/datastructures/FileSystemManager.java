package ca.concordia.filesystem.datastructures;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.concurrent.locks.*;

public class FileSystemManager {

    private static final int MAX_FILES = 5;
    private static final int MAX_BLOCKS = 10;
    private static final int BLOCK_SIZE = 128;
    private static final int MAX_FILENAME_LENGTH = 11;

    private final FEntry[] fileTable;
    private final FNode[] nodeTable;
    private final boolean[] freeBlockList;

    private final RandomAccessFile disk;

    private final ReentrantReadWriteLock rw = new ReentrantReadWriteLock(true);
    private final Lock readLock = rw.readLock();
    private final Lock writeLock = rw.writeLock();

    public FileSystemManager(String filename) {
        try {
            this.disk = new RandomAccessFile(filename, "rw");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        fileTable = new FEntry[MAX_FILES];
        nodeTable = new FNode[MAX_BLOCKS];
        freeBlockList = new boolean[MAX_BLOCKS];

        for (int i = 0; i < MAX_BLOCKS; i++)
            freeBlockList[i] = true;
    }

    // --- CREATE ---
    public void createFile(String filename) throws Exception {
        writeLock.lock();
        try {
            if (filename == null || filename.length() > MAX_FILENAME_LENGTH)
                throw new Exception("Invalid filename");

            for (FEntry e : fileTable)
                if (e != null && e.getFilename().equals(filename))
                    throw new Exception("Exists");

            for (int i = 0; i < fileTable.length; i++) {
                if (fileTable[i] == null) {
                    fileTable[i] = new FEntry(filename, (short) 0, (short) -1);
                    return;
                }
            }
            throw new Exception("File table full");
        } finally {
            writeLock.unlock();
        }
    }

    // --- LIST ---
    public String[] listFiles() {
        readLock.lock();
        try {
            int count = 0;
            for (FEntry e : fileTable)
                if (e != null) count++;

            String[] result = new String[count];
            int idx = 0;

            for (FEntry e : fileTable)
                if (e != null) result[idx++] = e.getFilename();

            return result;
        } finally {
            readLock.unlock();
        }
    }

    // --- WRITE ---
    public void writeFile(String filename, byte[] data) throws Exception {
        writeLock.lock();
        try {
            FEntry entry = findEntry(filename);
            if (entry == null) throw new Exception("Not found");

            int needed = (int) Math.ceil(data.length / (double) BLOCK_SIZE);
            int[] allocated = new int[needed];
            int count = 0;

            for (int i = 0; i < freeBlockList.length && count < needed; i++) {
                if (freeBlockList[i]) {
                    freeBlockList[i] = false;
                    allocated[count++] = i;
                }
            }

            if (count < needed)
                throw new Exception("Insufficient space");

            int pos = 0;
            for (int i = 0; i < allocated.length; i++) {
                int block = allocated[i];

                disk.seek(block * BLOCK_SIZE);
                int len = Math.min(BLOCK_SIZE, data.length - pos);
                disk.write(data, pos, len);
                pos += len;

                nodeTable[block] = new FNode(block);

                if (i < allocated.length - 1)
                    nodeTable[block].next = allocated[i + 1];
                else
                    nodeTable[block].next = -1;
            }

            entry.setFilesize((short) data.length);
            entry.setFirstBlock((short) allocated[0]);
        } finally {
            writeLock.unlock();
        }
    }

    // --- READ ---
    public byte[] readFile(String filename) throws Exception {
        readLock.lock();
        try {
            FEntry entry = findEntry(filename);
            if (entry == null) throw new Exception("Not found");

            byte[] result = new byte[entry.getFilesize()];
            int pos = 0;
            int block = entry.getFirstBlock();

            while (block != -1 && pos < result.length) {
                disk.seek(block * BLOCK_SIZE);
                int len = Math.min(BLOCK_SIZE, result.length - pos);
                disk.read(result, pos, len);
                pos += len;
                block = nodeTable[block].next;
            }

            return result;
        } finally {
            readLock.unlock();
        }
    }

    // --- DELETE ---
    public void deleteFile(String filename) throws Exception {
        writeLock.lock();
        try {
            for (int i = 0; i < fileTable.length; i++) {
                FEntry e = fileTable[i];
                if (e != null && e.getFilename().equals(filename)) {

                    int block = e.getFirstBlock();
                    while (block != -1) {
                        int next = nodeTable[block].next;
                        freeBlockList[block] = true;
                        nodeTable[block] = null;
                        block = next;
                    }

                    fileTable[i] = null;
                    return;
                }
            }
            throw new Exception("Not found");
        } finally {
            writeLock.unlock();
        }
    }

    private FEntry findEntry(String name) {
        for (FEntry e : fileTable)
            if (e != null && e.getFilename().equals(name))
                return e;
        return null;
    }
}
