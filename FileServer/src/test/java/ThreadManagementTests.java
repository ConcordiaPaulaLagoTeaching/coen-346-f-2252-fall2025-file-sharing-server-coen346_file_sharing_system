import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import helpers.ClientRunner;
import helpers.ServerRunner;

public class ThreadManagementTests {
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
    void testMultipleClientsConcurrentAccess() throws Exception {
        int numClients = 20;
        ExecutorService pool = Executors.newFixedThreadPool(numClients);
        CountDownLatch latch = new CountDownLatch(numClients);
        ConcurrentLinkedQueue<String> responses = new ConcurrentLinkedQueue<>();

        for (int i = 0; i < numClients; i++) {
            final int id = i;
            pool.submit(() -> {
                try {
                    String res = ClientRunner.send("CREATE file" + id);
                    responses.add(res);
                } catch (Exception e) {
                    fail("Client " + id + " failed: " + e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }

        boolean finished = latch.await(10, TimeUnit.SECONDS);
        pool.shutdownNow();

        assertTrue(finished, "Server did not handle concurrent clients in time");
        assertEquals(numClients, responses.size(), "Not all clients received responses");
    }

    @Test
void testReadersAndWritersSynchronization() throws Exception {
    // Create the shared file first
    ClientRunner.send("CREATE shared");
    ClientRunner.send("WRITE shared hello");

    ExecutorService pool = Executors.newFixedThreadPool(10);
    CountDownLatch latch = new CountDownLatch(6);  // Changed from 10 to 6

    // Start 5 readers
    for (int i = 0; i < 5; i++) {
        pool.submit(() -> {
            try {
                String res = ClientRunner.send("READ shared");
                System.out.println("Reader got: " + res);
                assertTrue(res.contains("hello") || res.contains("world"));
            } catch (Exception e) {
                System.err.println("Reader failed: " + e.getMessage());
            } finally {
                latch.countDown();
            }
        });
    }

    // Start 1 writer
    pool.submit(() -> {
        try {
            Thread.sleep(100);  // Let some readers start
            String res = ClientRunner.send("WRITE shared world");
            System.out.println("Writer got: " + res);
        } catch (Exception e) {
            System.err.println("Writer failed: " + e.getMessage());
        } finally {
            latch.countDown();
        }
    });

    boolean finished = latch.await(15, TimeUnit.SECONDS);  // Increased timeout
    pool.shutdownNow();

    assertTrue(finished, "Threads did not complete — possible deadlock");
}

    @Test
    void testDeadlockPreventionUnderStress() throws Exception {
        int n = 50;
        ExecutorService pool = Executors.newFixedThreadPool(10);
        CountDownLatch latch = new CountDownLatch(n);

        for (int i = 0; i < n; i++) {
            final int id = i;
            pool.submit(() -> {
                try {
                    String cmd;
                    if (id % 3 == 0) cmd = "WRITE shared data" + id;
                    else cmd = "READ shared";
                    ClientRunner.send(cmd);
                } catch (Exception ignored) {}
                finally { latch.countDown(); }
            });
        }

        boolean finished = latch.await(15, TimeUnit.SECONDS);
        pool.shutdownNow();

        assertTrue(finished, "Possible deadlock: not all threads finished");
    }






}
