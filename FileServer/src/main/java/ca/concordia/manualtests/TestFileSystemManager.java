package ca.concordia.manualtests;

import ca.concordia.filesystem.FileSystemManager;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class TestFileSystemManager {
    public static void main(String[] args) throws Exception {
        System.out.println("=== Manual Test ===");
        FileSystemManager fs = new FileSystemManager("virtual_disk.dat", 2048);

        fs.createFile("file1.txt");
        fs.writeFile("file1.txt", "Hello, world!".getBytes(StandardCharsets.UTF_8));
        byte[] data = fs.readFile("file1.txt");
        System.out.println("Read: " + new String(data, StandardCharsets.UTF_8));

        fs.listFiles();
        fs.deleteFile("file1.txt");

        System.out.println("✅ Manual test complete.");
    }
}
