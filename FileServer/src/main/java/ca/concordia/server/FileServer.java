package ca.concordia.server;
import ca.concordia.filesystem.FileSystemManager;
import ca.concordia.server.ServerThread;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.net.BindException;

public class FileServer {

    private FileSystemManager fsManager;
    private int port;
    private ServerSocket serverSocket;
    private Thread serverThread;
    private volatile boolean isRunning = false;

    //semaphores for concurrency control
    private final Semaphore mutex = new Semaphore(1);
    private final Semaphore isEmpty = new Semaphore(1);
    private final Semaphore writeLock = new Semaphore(1);
    private final AtomicInteger readCount = new AtomicInteger(0);

    public FileServer(int port, String fileSystemName, int totalSize){
        try{
            FileSystemManager fsManager = FileSystemManager.getInstance(fileSystemName, totalSize);
            this.fsManager = fsManager;
            this.port = port;
        } catch (Exception e){
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

    private void runServer(){

        try {
            serverSocket = new ServerSocket(port);
            isRunning = true;
            System.out.println("Server started. Listening on port " + this.port +"...");

            while (isRunning) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    System.out.println("Handling client: " + clientSocket);

                    //create thread to handle this client
                    ServerThread clientHandler = new ServerThread(clientSocket, fsManager, mutex, isEmpty, writeLock, readCount);
                    clientHandler.start();
                } catch (Exception e) {
                    e.printStackTrace();
                    System.err.println("Error handling client connection.");
                }
                break;
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
