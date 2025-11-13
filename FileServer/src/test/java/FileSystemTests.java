import java.io.File;

import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ca.concordia.filesystem.FileSystemManager;

public class FileSystemTests {
    private FileSystemManager fs;
    private static final String TEST_FS = "testfs.dat";

    @BeforeEach
    void setup() throws Exception {
        // Delete old test filesystem
        File testFile = new File(TEST_FS);
        if (testFile.exists()) {
            testFile.delete();
        }
        
        // Create fresh filesystem for each test
        // Force new instance by using reflection to reset singleton
        try {
            java.lang.reflect.Field instance = FileSystemManager.class.getDeclaredField("instance");
            instance.setAccessible(true);
            instance.set(null, null);
        } catch (Exception e) {
            // If reflection fails, just continue
        }
        
        fs = FileSystemManager.getInstance(TEST_FS, 10 * 128);
    }

    @AfterEach
    void cleanup() {
        try {
            if (fs != null) {
                fs.close();
            }
        } catch (Exception e) {
            // Ignore
        }
        
        // Clean up test file
        File testFile = new File(TEST_FS);
        if (testFile.exists()) {
            testFile.delete();
        }
    }

    @Test
    void testCreateFile() throws Exception {
        fs.createFile("a.txt");
        boolean fileFound = false;
        for (String fileName : fs.listFiles()) {
            if(fileName.equals("a.txt")) {
                fileFound = true;
                break;
            }
        }
        assertTrue(fileFound);
    }

    @Test
    void testTooLongFilename() {
        Exception ex = assertThrows(Exception.class, () -> fs.createFile("verylongname.txt"));
        String msg = ex.getMessage().toLowerCase();
        assertTrue(msg.contains("filename") || msg.contains("too large") || msg.contains("long"), 
                   "Expected error about filename length but got: " + ex.getMessage());
    }

    @Test
    void testWriteAndReadFile() throws Exception {
        fs.createFile("a.txt");
        fs.writeFile("a.txt", "hello".getBytes());
        assertEquals("hello", new String(fs.readFile("a.txt")));
    }

    @Test
    void testWriteAndReadLongFile() throws Exception {
        fs.createFile("c.txt");
        String longContent = "This is a long content that exceeds 128 bytes. ".repeat(5);
        fs.writeFile("c.txt", longContent.getBytes());
        assertEquals(longContent, new String(fs.readFile("c.txt")));
    }

    @Test
    void testDeleteFile() throws Exception {
        fs.createFile("b.txt");
        fs.deleteFile("b.txt");
        for(String fileName : fs.listFiles()){
            assertNotEquals("b.txt", fileName);
        }
    }
}
