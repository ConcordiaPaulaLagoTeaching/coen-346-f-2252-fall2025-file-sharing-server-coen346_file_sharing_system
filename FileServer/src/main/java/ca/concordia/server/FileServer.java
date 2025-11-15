package ca.concordia.server;

import ca.concordia.filesystem.datastructures.FileSystemManager;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class FileServer {

    private final int port;
    private final FileSystemManager fsManager;

    public FileServer(int port, String diskName) {
        this.port = port;
        this.fsManager = new FileSystemManager(diskName);
    }

    public void start() {
        System.out.println("Starting server on port " + port + "...");

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Server is running. Waiting for clients...");

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Client connected: " + clientSocket.getInetAddress());

                // MULTITHREADING — required
                ClientHandler handler = new ClientHandler(clientSocket, fsManager);
                Thread t = new Thread(handler);
                t.start();
            }

        } catch (IOException e) {
            System.out.println("Server error: " + e.getMessage());
        }
    }
}
