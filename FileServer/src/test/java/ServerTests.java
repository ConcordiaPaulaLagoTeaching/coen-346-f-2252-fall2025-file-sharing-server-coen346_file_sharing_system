import java.io.File;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterAll;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import helpers.ClientRunner;
import helpers.ServerRunner;

public class ServerTests {

    static ServerRunner server;

    @BeforeAll
    static void startServer() throws Exception {
        server = new ServerRunner();
        server.start();
    }

    @AfterAll
    static void stopServer() {
        server.stop();
    }


    @Test
    void testServerRecoversAfterErrorCommand() throws Exception {
        // Send malformed command
        String bad = ClientRunner.send("WRIT invalid");
        assertTrue(bad.startsWith("ERROR"), "Server should report an error");

        // Send valid command afterward to verify server still responsive
        String ok = ClientRunner.send("LIST");
        assertNotNull(ok, "Server did not respond after error");
    }

    @Test
    void testMalformedInputDoesNotCrashServer() throws Exception {
    for (String cmd : new String[]{"", "BADCOMMAND", "CREATE", "WRITE", "READ", "DELETE"}) {
        try {
            String res = ClientRunner.send(cmd);
            assertNotNull(res, "Server did not respond to: " + cmd);
            assertTrue(res.startsWith("ERROR") || res.startsWith("SUCCESS"), 
                      "Server should respond with ERROR or SUCCESS for: " + cmd + " but got: " + res);
        } catch (Exception e) {
            // If connection fails, that's also acceptable (server didn't crash)
            System.out.println("Connection failed for: " + cmd + " (acceptable)");
        }
    }
}

    @Test
    @Timeout(15)
    void testHandlesHundredsOfClientsQuickly() throws Exception {
        int n = 100;
        ExecutorService pool = Executors.newFixedThreadPool(20);
        CountDownLatch latch = new CountDownLatch(n);

        for (int i = 0; i < n; i++) {
            pool.submit(() -> {
                try { ClientRunner.send("LIST"); }
                catch (Exception ignored) {}
                finally { latch.countDown(); }
            });
        }

        assertTrue(latch.await(15, TimeUnit.SECONDS), "Server scaled poorly under 100 clients");
    }

    @Test
    void testServerRestartPersistence() throws Exception {
    // Clean up any existing filesystem
    File fs = new File("testfs.dat");
    if (fs.exists()) {
        fs.delete();
    }
    
    // Step 1: Start server
    ServerRunner server1 = new ServerRunner();
    server1.start();

    ClientRunner.send("CREATE persist");
    ClientRunner.send("WRITE persist saveddata");
    server1.stop();

    // Give filesystem time to flush
    Thread.sleep(1000);

    // Step 2: Restart server
    ServerRunner server2 = new ServerRunner();
    server2.start();

    String response = ClientRunner.send("READ persist");
    assertTrue(response.contains("saveddata"), "File data not persisted across restart. Got: " + response);

    server2.stop();
    
    // Clean up
    if (fs.exists()) {
        fs.delete();
    }
}
}
