package helpers;

import ca.concordia.server.FileServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.time.Duration;
import java.time.Instant;

public class ServerRunner {
    
    private FileServer server;

    public void start() throws IOException, InterruptedException {
        // Create and start server directly (not as external process)
        server = new FileServer(12345, "testfs.dat", 16 * 128);
        
        // Start in background thread
        new Thread(() -> server.start()).start();
        
        // Wait for port to become available (server is ready)
        Instant start = Instant.now();
        while (!isPortOpen("localhost", 12345)) {
            if (Duration.between(start, Instant.now()).getSeconds() > 10) {
                throw new RuntimeException("Server failed to start within 10 second timeout");
            }
            Thread.sleep(200);
        }
        
        System.out.println("Test server is ready on port 12345");
    }


    private boolean isPortOpen(String host, int port) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), 200);
            return true;
        } catch (IOException ex) {
            return false;
        }
    }


    public void stop() {
        if (server != null) {
            server.stop(); // Call the new stop() method
            System.out.println("Test server stopped");
        }
        
        // Give the OS time to fully release the port
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            // Ignore interruption during cleanup
        }
    }
}
