package ca.concordia.server;
import ca.concordia.filesystem.FileSystemManager;
import ca.concordia.server.ServerThread;

import java.net.ServerSocket;
import java.net.Socket;
import java.net.BindException;

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

    public void start(){
        // Run server in background thread so caller can continue
        serverThread = new Thread(() -> runServer());
        serverThread.setDaemon(false);
        serverThread.start();
    }

    private void runServer() {
        try {
            serverSocket = new ServerSocket(port);
            isRunning = true;
            System.out.println("Server started. Listening on port " + this.port + "...");

            while (isRunning) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    System.out.println("Handling client: " + clientSocket);

                    // Create thread to handle this client
                    ServerThread clientHandler = new ServerThread(clientSocket, fsManager);
                    clientHandler.start();
                } catch (Exception e) {
                    e.printStackTrace();
                    System.err.println("Error handling client connection.");
                }
            }
        } catch (BindException e) {
            System.err.println("ERROR: Port " + port + " is already in use. Cannot start server.");
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Could not start server on port " + port);
        } finally {
            isRunning = false;
            if (serverSocket != null && !serverSocket.isClosed()) {
                try {
                    serverSocket.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

}
