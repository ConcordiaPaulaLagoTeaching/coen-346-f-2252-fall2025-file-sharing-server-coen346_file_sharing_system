package ca.concordia.server;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

import ca.concordia.filesystem.FileSystemManager;


public class ServerThread extends Thread {
    
    protected Socket clientSocket;
    private FileSystemManager fsManager;


    public ServerThread(Socket clientSocket, FileSystemManager fsManager) {
        this.clientSocket = clientSocket;
        this.fsManager = fsManager;
    }


    @Override
    public void run() {
        try (
            BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            PrintWriter writer = new PrintWriter(clientSocket.getOutputStream(), true)
        ) {
            String line;
            // Read commands until client disconnects or sends empty line
            while ((line = reader.readLine()) != null) {
                 if (line.trim().isEmpty()) {
                     writer.println("ERROR: Empty command");
                     continue;  // Don't exit loop, just skip this iteration
                     }
    
                System.out.println("Received from client: " + line);
                
                // Parse command - split into max 3 parts (command, filename, payload)
                String[] parts = line.trim().split("\\s+", 3);
                if (parts.length == 0) {
                    writer.println("ERROR: Empty command");
                    continue;
                }
                
                String command = parts[0].toUpperCase();
                String filename = parts.length > 1 ? parts[1] : "";
                String payload = parts.length > 2 ? parts[2] : "";

                try {
                    // Process each command
                    // FileSystemManager methods handle all synchronization
                    switch (command) {
                        case "CREATE":
                            if (filename.isEmpty()) {
                                writer.println("ERROR: CREATE requires a filename");
                                break;
                            }
                            fsManager.createFile(filename);
                            writer.println("SUCCESS: File '" + filename + "' created.");
                            break;
                            
                        case "READ":
                            if (filename.isEmpty()) {
                                writer.println("ERROR: READ requires a filename");
                                break;
                            }
                            byte[] content = fsManager.readFile(filename);
                            writer.println("SUCCESS: " + new String(content));
                            break;
                            
                        case "WRITE":
                            if (filename.isEmpty()) {
                                writer.println("ERROR: WRITE requires a filename");
                                break;
                            }
                            if (payload.isEmpty()) {
                                writer.println("ERROR: WRITE requires content");
                                break;
                            }
                            fsManager.writeFile(filename, payload.getBytes());
                            writer.println("SUCCESS: File '" + filename + "' written.");
                            break;
                            
                        case "DELETE":
                            if (filename.isEmpty()) {
                                writer.println("ERROR: DELETE requires a filename");
                                break;
                            }
                            fsManager.deleteFile(filename);
                            writer.println("SUCCESS: File '" + filename + "' deleted.");
                            break;
                            
                        case "LIST":
                            String[] files = fsManager.listFiles();
                            if (files.length == 0) {
                                writer.println("SUCCESS: No files");
                            } else {
                                writer.println("SUCCESS: Files:");
                                for (int i = 0; i < files.length; i++) {
                                    writer.println("  [" + (i + 1) + "] " + files[i]);
                                }
                            }
                            break;
                            
                        case "QUIT":
                        case "EXIT":
                            writer.println("SUCCESS: Disconnecting");
                            return; // Exit thread
                            
                        default:
                            writer.println("ERROR: Unknown command: " + command);
                            break;
                    }
                } catch (Exception e) {
                    // Send error message to client but keep connection alive
                    writer.println("ERROR: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        } catch (Exception e) {
            System.err.println("Client handler error: " + e.getMessage());
            e.printStackTrace();
        } finally {
            // Always close the socket
            try {
                clientSocket.close();
                System.out.println("Client disconnected: " + clientSocket.getRemoteSocketAddress());
            } catch (Exception e) {
                // Ignore exceptions during cleanup
            }
        }
    }
}