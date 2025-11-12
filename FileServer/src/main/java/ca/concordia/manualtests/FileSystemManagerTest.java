package ca.concordia.manualtests;

import ca.concordia.filesystem.FileSystemManager;
import java.nio.charset.StandardCharsets;

/**
 * FileSystemManagerTest
 * ----------------------
 * Manual test harness for FileSystemManager.
 * Demonstrates all core operations: create, write, read, list, and delete.
 * 
 * Run this independently before integrating with the FileServer.
 */
public class FileSystemManagerTest {

    public static void main(String[] args) {
        try {
            System.out.println("=== Initializing Test Filesystem ===");
            FileSystemManager fs = new FileSystemManager("virtual_disk.dat", 1280);

            System.out.println("\n[TEST] Creating files...");
            fs.createFile("file1.txt");
            fs.createFile("file2.txt");
            fs.createFile("file3.txt");

            System.out.println("\n[TEST] Listing files after creation:");
            for (String name : fs.listFiles()) {
                System.out.println(" - " + name);
            }

            System.out.println("\n[TEST] Writing content to 'file1.txt'...");
            String content1 = "Hello, COEN346 FileSystem!";
            fs.writeFile("file1.txt", content1.getBytes(StandardCharsets.UTF_8));

            System.out.println("[TEST] Writing content to 'file2.txt'...");
            String content2 = "Another file with random text data.";
            fs.writeFile("file2.txt", content2.getBytes(StandardCharsets.UTF_8));

            System.out.println("\n[TEST] Reading files back:");
            byte[] read1 = fs.readFile("file1.txt");
            byte[] read2 = fs.readFile("file2.txt");

            System.out.println("Contents of file1.txt: " + new String(read1, StandardCharsets.UTF_8));
            System.out.println("Contents of file2.txt: " + new String(read2, StandardCharsets.UTF_8));

            System.out.println("\n[TEST] Files currently in filesystem:");
            for (String name : fs.listFiles()) {
                System.out.println(" - " + name);
            }

            System.out.println("\n[TEST] Deleting 'file2.txt'...");
            fs.deleteFile("file2.txt");

            System.out.println("\n[TEST] Files after deleting file2.txt:");
            for (String name : fs.listFiles()) {
                System.out.println(" - " + name);
            }

            try {
                System.out.println("\n[TEST] Attempting to read deleted file2.txt...");
                fs.readFile("file2.txt");
            } catch (Exception e) {
                System.out.println("Expected error: " + e.getMessage());
            }

            System.out.println("\n[TEST] Overwriting file1.txt with larger content...");
            String longText = "This is a larger text that spans multiple blocks. "
                    + "It tests if the FileSystemManager correctly handles multi-block writes.";
            fs.writeFile("file1.txt", longText.getBytes(StandardCharsets.UTF_8));

            byte[] readLong = fs.readFile("file1.txt");
            System.out.println("Contents of overwritten file1.txt:");
            System.out.println(new String(readLong, StandardCharsets.UTF_8));

            System.out.println("\n All tests completed successfully.");

        } catch (Exception e) {
            System.err.println("Test failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
