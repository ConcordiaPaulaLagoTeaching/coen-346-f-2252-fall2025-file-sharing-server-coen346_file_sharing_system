package ca.concordia.filesystem;

import ca.concordia.filesystem.datastructures.FileSystemManager;
import org.junit.jupiter.api.*;

import java.io.File;

import static org.junit.jupiter.api.Assertions.*;

public class FileSystemTests {

    FileSystemManager fs;

    @BeforeEach
    void setup() throws Exception {

        // Always reset the disk so every test starts clean
        File disk = new File("testfs.dat");
        if (disk.exists()) {
            disk.delete();
        }

        // Create new fresh filesystem for EACH test
        fs = new FileSystemManager("testfs.dat");
    }

    @Test
    void testCreateFile() throws Exception {
        fs.createFile("a.txt");
        boolean found = false;

        for (String name : fs.listFiles()) {
            if (name.equals("a.txt")) {
                found = true;
                break;
            }
        }

        assertTrue(found);
    }

    @Test
    void testTooLongFilename() {
        Exception ex = assertThrows(Exception.class,
                () -> fs.createFile("verylongname.txt"));
        assertTrue(ex.getMessage().toLowerCase().contains("filename"));
        assertTrue(ex.getMessage().toLowerCase().contains("long"));
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
        String longContent =
                "This is a long content that exceeds 128 bytes. ".repeat(5);

        fs.writeFile("c.txt", longContent.getBytes());
        assertEquals(longContent, new String(fs.readFile("c.txt")));
    }

    @Test
    void testDeleteFile() throws Exception {
        fs.createFile("b.txt");
        fs.deleteFile("b.txt");

        for (String name : fs.listFiles()) {
            assertNotEquals("b.txt", name);
        }
    }
}
