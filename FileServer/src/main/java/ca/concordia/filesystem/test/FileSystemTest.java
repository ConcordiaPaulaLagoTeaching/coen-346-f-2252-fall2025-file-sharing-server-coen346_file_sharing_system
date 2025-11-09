package ca.concordia.filesystem.test;

import ca.concordia.filesystem.FileSystemManager;

import java.io.File;
import java.util.Arrays;

/**
 * Test suite for FileSystemManager
 * Run these tests to verify your implementation works correctly
 */
public class FileSystemTest {
    
    private static final String TEST_DISK = "test_filesystem.dat";
    private static final int BLOCK_SIZE = 128;
    private static final int MAX_FILES = 5;
    private static final int MAX_BLOCKS = 10;
    private static final int TOTAL_SIZE = MAX_BLOCKS * BLOCK_SIZE;
    
    private static int testsPassed = 0;
    private static int testsFailed = 0;
    
    public static void main(String[] args) {
        System.out.println("=== FileSystemManager Test Suite ===\n");
        
        try {
            // Clean up old test file
            new File(TEST_DISK).delete();
            
            // Run all tests
            testCreateFile();
            testCreateDuplicateFile();
            testCreateTooManyFiles();
            testFilenameTooLong();
            testWriteAndReadSmallFile();
            testWriteAndReadLargeFile();
            testWriteEmptyFile();
            testWriteToNonexistentFile();
            testDeleteFile();
            testDeleteNonexistentFile();
            testListFiles();
            testOverwriteFile();
            testFileTooLarge();
            testPersistence();
            
            // Summary
            System.out.println("\n=== Test Summary ===");
            System.out.println("Tests Passed: " + testsPassed);
            System.out.println("Tests Failed: " + testsFailed);
            
            if (testsFailed == 0) {
                System.out.println("\n✅ All tests passed!");
            } else {
                System.out.println("\n❌ Some tests failed. Review the output above.");
            }
            
        } catch (Exception e) {
            System.err.println("Fatal error during testing: " + e.getMessage());
            e.printStackTrace();
        } finally {
            // Clean up
            new File(TEST_DISK).delete();
        }
    }
    
    //test cases
    
    private static void testCreateFile() {
        System.out.println("Test 1: Create File");
        try {
            FileSystemManager fsm = FileSystemManager.getInstance(TEST_DISK, TOTAL_SIZE, MAX_FILES, MAX_BLOCKS, BLOCK_SIZE);
            fsm.createFile("test.txt");
            
            String[] files = fsm.listFiles();
            if (files.length == 1 && files[0].equals("test.txt")) {
                pass("Created file successfully");
            } else {
                fail("File not found in list");
            }
            
            fsm.close();
            new File(TEST_DISK).delete();
        } catch (Exception e) {
            fail("Exception: " + e.getMessage());
        }
    }
    
    private static void testCreateDuplicateFile() {
        System.out.println("\nTest 2: Create Duplicate File");
        try {
            FileSystemManager fsm = FileSystemManager.getInstance(TEST_DISK, TOTAL_SIZE, MAX_FILES, MAX_BLOCKS, BLOCK_SIZE);
            fsm.createFile("test.txt");
            
            try {
                fsm.createFile("test.txt");
                fail("Should have thrown exception for duplicate file");
            } catch (Exception e) {
                if (e.getMessage().contains("already exists")) {
                    pass("Correctly rejected duplicate file");
                } else {
                    fail("Wrong error message: " + e.getMessage());
                }
            }
            
            fsm.close();
            new File(TEST_DISK).delete();
        } catch (Exception e) {
            fail("Exception: " + e.getMessage());
        }
    }
    
    private static void testCreateTooManyFiles() {
        System.out.println("\nTest 3: Create Too Many Files");
        try {
            FileSystemManager fsm = FileSystemManager.getInstance(TEST_DISK, TOTAL_SIZE, MAX_FILES, MAX_BLOCKS, BLOCK_SIZE);
            
            // Create MAX_FILES files
            for (int i = 0; i < MAX_FILES; i++) {
                fsm.createFile("file" + i);
            }
            
            // Try to create one more
            try {
                fsm.createFile("extra");
                fail("Should have thrown exception when exceeding MAX_FILES");
            } catch (Exception e) {
                if (e.getMessage().contains("maximum")) {
                    pass("Correctly rejected when MAX_FILES reached");
                } else {
                    fail("Wrong error message: " + e.getMessage());
                }
            }
            
            fsm.close();
            new File(TEST_DISK).delete();
        } catch (Exception e) {
            fail("Exception: " + e.getMessage());
        }
    }
    
    private static void testFilenameTooLong() {
        System.out.println("\nTest 4: Filename Too Long");
        try {
            FileSystemManager fsm = FileSystemManager.getInstance(TEST_DISK, TOTAL_SIZE, MAX_FILES, MAX_BLOCKS, BLOCK_SIZE);
            
            try {
                fsm.createFile("verylongfilename.txt"); // 22 characters
                fail("Should have thrown exception for filename too long");
            } catch (Exception e) {
                if (e.getMessage().contains("too large") || e.getMessage().contains("too long")) {
                    pass("Correctly rejected long filename");
                } else {
                    fail("Wrong error message: " + e.getMessage());
                }
            }
            
            fsm.close();
            new File(TEST_DISK).delete();
        } catch (Exception e) {
            fail("Exception: " + e.getMessage());
        }
    }
    
    private static void testWriteAndReadSmallFile() {
        System.out.println("\nTest 5: Write and Read Small File");
        try {
            FileSystemManager fsm = FileSystemManager.getInstance(TEST_DISK, TOTAL_SIZE, MAX_FILES, MAX_BLOCKS, BLOCK_SIZE);
            fsm.createFile("small.txt");
            
            byte[] content = "Hello World!".getBytes();
            fsm.writeFile("small.txt", content);
            
            byte[] readContent = fsm.readFile("small.txt");
            
            if (Arrays.equals(content, readContent)) {
                pass("Small file written and read correctly");
            } else {
                fail("Read content doesn't match written content");
            }
            
            fsm.close();
            new File(TEST_DISK).delete();
        } catch (Exception e) {
            fail("Exception: " + e.getMessage());
        }
    }
    
    private static void testWriteAndReadLargeFile() {
        System.out.println("\nTest 6: Write and Read Large File (Multiple Blocks)");
        try {
            FileSystemManager fsm = FileSystemManager.getInstance(TEST_DISK, TOTAL_SIZE, MAX_FILES, MAX_BLOCKS, BLOCK_SIZE);
            fsm.createFile("large.txt");
            
            // Create content larger than one block
            byte[] content = new byte[300]; // More than 2 blocks
            for (int i = 0; i < content.length; i++) {
                content[i] = (byte) (i % 256);
            }
            
            fsm.writeFile("large.txt", content);
            byte[] readContent = fsm.readFile("large.txt");
            
            if (Arrays.equals(content, readContent)) {
                pass("Large file written and read correctly");
            } else {
                fail("Read content doesn't match written content");
            }
            
            fsm.close();
            new File(TEST_DISK).delete();
        } catch (Exception e) {
            fail("Exception: " + e.getMessage());
        }
    }
    
    private static void testWriteEmptyFile() {
        System.out.println("\nTest 7: Write Empty File");
        try {
            FileSystemManager fsm = FileSystemManager.getInstance(TEST_DISK, TOTAL_SIZE, MAX_FILES, MAX_BLOCKS, BLOCK_SIZE);
            fsm.createFile("empty.txt");
            
            byte[] content = new byte[0];
            fsm.writeFile("empty.txt", content);
            
            byte[] readContent = fsm.readFile("empty.txt");
            
            if (readContent.length == 0) {
                pass("Empty file handled correctly");
            } else {
                fail("Empty file should have 0 bytes");
            }
            
            fsm.close();
            new File(TEST_DISK).delete();
        } catch (Exception e) {
            fail("Exception: " + e.getMessage());
        }
    }
    
    private static void testWriteToNonexistentFile() {
        System.out.println("\nTest 8: Write to Nonexistent File");
        try {
            FileSystemManager fsm = FileSystemManager.getInstance(TEST_DISK, TOTAL_SIZE, MAX_FILES, MAX_BLOCKS, BLOCK_SIZE);
            
            try {
                fsm.writeFile("nonexistent.txt", "test".getBytes());
                fail("Should have thrown exception for nonexistent file");
            } catch (Exception e) {
                if (e.getMessage().contains("does not exist")) {
                    pass("Correctly rejected write to nonexistent file");
                } else {
                    fail("Wrong error message: " + e.getMessage());
                }
            }
            
            fsm.close();
            new File(TEST_DISK).delete();
        } catch (Exception e) {
            fail("Exception: " + e.getMessage());
        }
    }
    
    private static void testDeleteFile() {
        System.out.println("\nTest 9: Delete File");
        try {
            FileSystemManager fsm = FileSystemManager.getInstance(TEST_DISK, TOTAL_SIZE, MAX_FILES, MAX_BLOCKS, BLOCK_SIZE);
            fsm.createFile("delete.txt");
            fsm.writeFile("delete.txt", "content".getBytes());
            fsm.deleteFile("delete.txt");
            
            String[] files = fsm.listFiles();
            if (files.length == 0) {
                pass("File deleted successfully");
            } else {
                fail("File still exists after deletion");
            }
            
            fsm.close();
            new File(TEST_DISK).delete();
        } catch (Exception e) {
            fail("Exception: " + e.getMessage());
        }
    }
    
    private static void testDeleteNonexistentFile() {
        System.out.println("\nTest 10: Delete Nonexistent File");
        try {
            FileSystemManager fsm = FileSystemManager.getInstance(TEST_DISK, TOTAL_SIZE, MAX_FILES, MAX_BLOCKS, BLOCK_SIZE);
            
            try {
                fsm.deleteFile("nonexistent.txt");
                fail("Should have thrown exception for nonexistent file");
            } catch (Exception e) {
                if (e.getMessage().contains("does not exist")) {
                    pass("Correctly rejected delete of nonexistent file");
                } else {
                    fail("Wrong error message: " + e.getMessage());
                }
            }
            
            fsm.close();
            new File(TEST_DISK).delete();
        } catch (Exception e) {
            fail("Exception: " + e.getMessage());
        }
    }
    
    private static void testListFiles() {
        System.out.println("\nTest 11: List Files");
        try {
            FileSystemManager fsm = FileSystemManager.getInstance(TEST_DISK, TOTAL_SIZE, MAX_FILES, MAX_BLOCKS, BLOCK_SIZE);
            fsm.createFile("file1.txt");
            fsm.createFile("file2.txt");
            fsm.createFile("file3.txt");
            
            String[] files = fsm.listFiles();
            
            if (files.length == 3) {
                pass("List files returned correct count");
            } else {
                fail("Expected 3 files, got " + files.length);
            }
            
            fsm.close();
            new File(TEST_DISK).delete();
        } catch (Exception e) {
            fail("Exception: " + e.getMessage());
        }
    }
    
    private static void testOverwriteFile() {
        System.out.println("\nTest 12: Overwrite File");
        try {
            FileSystemManager fsm = FileSystemManager.getInstance(TEST_DISK, TOTAL_SIZE, MAX_FILES, MAX_BLOCKS, BLOCK_SIZE);
            fsm.createFile("overwrite.txt");
            
            // Write initial content
            fsm.writeFile("overwrite.txt", "initial".getBytes());
            
            // Overwrite with new content
            byte[] newContent = "new content".getBytes();
            fsm.writeFile("overwrite.txt", newContent);
            
            byte[] readContent = fsm.readFile("overwrite.txt");
            
            if (Arrays.equals(newContent, readContent)) {
                pass("File overwritten correctly");
            } else {
                fail("Overwrite failed");
            }
            
            fsm.close();
            new File(TEST_DISK).delete();
        } catch (Exception e) {
            fail("Exception: " + e.getMessage());
        }
    }
    
    private static void testFileTooLarge() {
        System.out.println("\nTest 13: File Too Large");
        try {
            FileSystemManager fsm = FileSystemManager.getInstance(TEST_DISK, TOTAL_SIZE, MAX_FILES, MAX_BLOCKS, BLOCK_SIZE);
            fsm.createFile("huge.txt");
            
            // Try to write more than available blocks
            // With 10 blocks and 2 metadata blocks, we have 8 data blocks = 1024 bytes
            byte[] hugeContent = new byte[2000]; // Way too large
            
            try {
                fsm.writeFile("huge.txt", hugeContent);
                fail("Should have thrown exception for file too large");
            } catch (Exception e) {
                if (e.getMessage().contains("too large")) {
                    pass("Correctly rejected file too large");
                } else {
                    fail("Wrong error message: " + e.getMessage());
                }
            }
            
            fsm.close();
            new File(TEST_DISK).delete();
        } catch (Exception e) {
            fail("Exception: " + e.getMessage());
        }
    }
    
    private static void testPersistence() {
        System.out.println("\nTest 14: File System Persistence");
        try {
            // Create file system and write data
            FileSystemManager fsm1 = FileSystemManager.getInstance(TEST_DISK, TOTAL_SIZE, MAX_FILES, MAX_BLOCKS, BLOCK_SIZE);
            fsm1.createFile("persist.txt");
            byte[] content = "persistent data".getBytes();
            fsm1.writeFile("persist.txt", content);
            fsm1.close();
            
            // Reopen file system and read data
            FileSystemManager fsm2 = FileSystemManager.getInstance(TEST_DISK, TOTAL_SIZE, MAX_FILES, MAX_BLOCKS, BLOCK_SIZE);
            byte[] readContent = fsm2.readFile("persist.txt");
            
            if (Arrays.equals(content, readContent)) {
                pass("File system persisted correctly");
            } else {
                fail("Data not persisted correctly");
            }
            
            fsm2.close();
            new File(TEST_DISK).delete();
        } catch (Exception e) {
            fail("Exception: " + e.getMessage());
        }
    }
    
    //Helpers
    
    private static void pass(String message) {
        System.out.println("  ✅ PASS: " + message);
        testsPassed++;
    }
    
    private static void fail(String message) {
        System.out.println("  ❌ FAIL: " + message);
        testsFailed++;
    }
}
