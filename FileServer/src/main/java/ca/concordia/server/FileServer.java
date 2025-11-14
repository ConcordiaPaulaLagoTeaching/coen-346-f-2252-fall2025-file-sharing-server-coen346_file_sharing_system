package ca.concordia.server;
import ca.concordia.filesystem.FileSystemManager;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

public class FileServer {

    private FileSystemManager fsManager;
    private int port;
    public FileServer(int port, String fileSystemName, int totalSize){
        // Initialize the FileSystemManager
        FileSystemManager fsManager = new FileSystemManager(fileSystemName,
                10*128 );
        this.fsManager = fsManager;
        this.port = port;
    }

    public void start() {
        try (ServerSocket serverSocket = new ServerSocket(12345)) {
            System.out.println("Server started. Listening on port 12345...");

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Handling client: " + clientSocket);
                //  spawn a thread for each client
                new Thread(() -> handleClient(clientSocket)).start();
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Could not start server on port " + port);
        }
    }

    private void handleClient(Socket clientSocket) {
        try (
                BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                PrintWriter writer = new PrintWriter(clientSocket.getOutputStream(), true)
        ) {
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println("Received from client: " + line);
                String[] parts = line.split(" ");
                String command = parts[0].toUpperCase();

                switch (command) {
                    case "CREATE":
                        fsManager.createFile(parts[1]);
                        writer.println("SUCCESS: File '" + parts[1] + "' created.");
                        writer.flush();
                        break;
                    //TODO: Implement other commands READ, WRITE, DELETE, LIST

                    case "DELETE":
                        fsManager.deleteFile(parts[1]);
                        writer.println("SUCCESS: File '" + parts[1] + "' deleted.");
                        writer.flush();
                        break;

                    case "WRITE":
                        // WRITE filename data
                        String fileName = parts[1];
                        String content = line.substring(command.length() + fileName.length() + 2);
                        fsManager.writeFile(fileName, content.getBytes());
                        writer.println("SUCCESS: Wrote to '" + fileName + "'.");
                        writer.flush();
                        break;

                    case "READ":
                        byte[] bytes = fsManager.readFile(parts[1]);
                        writer.println("SUCCESS: Read from '" + parts[1] + "'.");
                        writer.println(new String(bytes));
                        writer.flush();
                        break;

                    case "LIST":
                        writer.println("SUCCESS: Listing files.");
                        String[] files = fsManager.listFiles();
                        for (String f : files) {
                            writer.println(f);
                        }
                        writer.println("END");
                        writer.flush();
                        break;

                    case "QUIT":
                        writer.println("SUCCESS: Disconnecting.");
                        return;
                    default:
                        writer.println("ERROR: Unknown command.");
                        break;
                }
            }
        }   catch (Exception e) {
        e.printStackTrace();
                } finally {
                        try {
                        clientSocket.close();
                } catch (Exception e) {
        // Ignore
        }
        }

        }

}
