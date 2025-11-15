package helpers;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.time.Duration;
import java.time.Instant;

public class ServerRunner {
    private Process process;

    public void start() throws IOException, InterruptedException {

        // Start the server using the correct main class
        this.process = new ProcessBuilder(
                "java",
                "-cp",
                "target/classes",
                "ca.concordia.server.ServerMain"
        )
                .redirectErrorStream(true)
                .start();

        // Wait until the server opens port 12345
        Instant start = Instant.now();
        while (!isPortOpen("localhost", 12345)) {
            if (Duration.between(start, Instant.now()).getSeconds() > 10) {
                throw new RuntimeException("Server failed to start within timeout");
            }
            Thread.sleep(200);
        }
    }

    private boolean isPortOpen(String host, int port) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), 200);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    public void stop() {
        if (this.process != null && this.process.isAlive()) {
            this.process.destroy();
        }
    }
}
