package ca.concordia.server;

import java.net.BindException;
import java.net.ServerSocket;
import java.net.Socket;

import ca.concordia.filesystem.FileSystemManager;

public class FileServer {

    private FileSystemManager fsManager;
    private int port;
    private ServerSocket serverSocket;
    private Thread serverThread;
    private volatile boolean isRunning = false;

    public FileServer(int port, String fileSystemName, int totalSize) {
        try {
            this.fsManager = FileSystemManager.getInstance(fileSystemName, totalSize);
            this.port = port;
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Could not initialize FileSystemManager.");
        }
    }

    public void start() {
        // Run server in background thread so caller can continue
        serverThread = new Thread(() -> runServer());
        serverThread.setDaemon(false);
        serverThread.start();
    }

    public void stop() {
        System.out.println("Shutting down server...");
        isRunning = false;
        
        try {
            // Close server socket - this will cause accept() to throw SocketException
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
            
            // Wait for server thread to finish
            if (serverThread != null) {
                serverThread.join(5000); // Wait up to 5 seconds
            }
            
            // Close file system
            if (fsManager != null) {
                fsManager.close();
            }
            
            System.out.println("Server stopped successfully.");
        } catch (Exception e) {
            System.err.println("Error during shutdown: " + e.getMessage());
            e.printStackTrace();
        }
    }


    private void runServer() {
        try {
            serverSocket = new ServerSocket(port);
            serverSocket.setReuseAddress(true); // Allow port reuse for quick restart
            isRunning = true;
            System.out.println("Server started. Listening on port " + this.port + "...");

            // Continuously accept client connections
            while (isRunning) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    System.out.println("Client connected: " + clientSocket.getRemoteSocketAddress());

                    // Create and start thread to handle this client
                    ServerThread clientHandler = new ServerThread(clientSocket, fsManager);
                    clientHandler.setDaemon(false); // Ensure threads complete their work
                    clientHandler.start();
                    
                } catch (java.net.SocketException e) {
                    // Socket was closed - this is expected during shutdown
                    if (isRunning) {
                        System.err.println("Socket error: " + e.getMessage());
                    }
                    break; // Exit the loop on socket close
                } catch (Exception e) {
                    // Handle other exceptions but keep server running
                    if (isRunning) {
                        System.err.println("Error handling client connection: " + e.getMessage());
                        e.printStackTrace();
                    }
                }
            }
        } catch (BindException e) {
            System.err.println("ERROR: Port " + port + " is already in use. Cannot start server.");
            System.err.println("Try: lsof -i :" + port + " to find the process using this port");
            e.printStackTrace();
        } catch (Exception e) {
            System.err.println("Could not start server on port " + port);
            e.printStackTrace();
        } finally {
            isRunning = false;
            // Ensure socket is closed
            try {
                if (serverSocket != null && !serverSocket.isClosed()) {
                    serverSocket.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
