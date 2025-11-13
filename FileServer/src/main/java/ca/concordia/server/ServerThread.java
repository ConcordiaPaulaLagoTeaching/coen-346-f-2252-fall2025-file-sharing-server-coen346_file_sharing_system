package ca.concordia.server;

import java.io.IOException;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

import ca.concordia.filesystem.FileSystemManager;

/**
 * Basic worker for handling a single client connection for file-related
 * requests.
 * TODO: implement protocol-specific handling inside run().
 */
public class ServerThread extends Thread {
    protected Socket clientSocket;
    private FileSystemManager fsManager;

    public ServerThread(Socket clientSocket, FileSystemManager fsManager) {
        this.clientSocket = clientSocket;
        this.fsManager = fsManager;
    }

    public void run() {
        try (
                BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                PrintWriter writer = new PrintWriter(clientSocket.getOutputStream(), true)) {
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println("Received from client: " + line);
                String[] parts = line.split(" ");
                String command = parts[0].toUpperCase();

                // check for filename and payload based on command
                String filename;
                String payload;

                if (parts.length > 1 == false) {
                    filename = "";
                } else {
                    filename = parts[1];
                }
                if (parts.length > 2 == false) {
                    payload = "";
                } else {
                    payload = parts[2];
                }

                switch (command) {
                    case "CREATE":
                        try {
                            fsManager.createFile(filename);
                            writer.println("SUCCESS: File '" + filename + "' created.");
                            writer.flush();
                        } catch (Exception e) {
                            writer.println("File Creation Error: " + e.getMessage());
                            writer.flush();
                        }
                        break;
                    case "READ":
                        try {
                            byte[] content = fsManager.readFile(filename);
                            writer.println("SUCCESS: File '" + filename + "' read. Content: " + new String(content));
                            writer.flush();
                        } catch (Exception e) {
                            writer.println("File Read Error: " + e.getMessage());
                            writer.flush();
                        }
                        break;
                    case "WRITE":
                        try {
                            fsManager.writeFile(filename, payload.getBytes());
                            writer.println("SUCCESS: File '" + filename + "' written.");
                            writer.flush();
                        } catch (Exception e) {
                            writer.println("File Write Error: " + e.getMessage());
                            writer.flush();
                        }
                        break;
                    case "DELETE":
                        try {
                            fsManager.deleteFile(filename);
                            writer.println("SUCCESS: File '" + filename + "' deleted.");
                            writer.flush();
                        } catch (Exception e) {
                            writer.println("File Deletion Error: " + e.getMessage());
                            writer.flush();
                        }
                        break;
                    case "LIST":
                        try {
                            String[] files = fsManager.listFiles();
                            writer.println("SUCCESS: Files:");
                            if (files.length == 0) {
                                writer.println("(no files)");
                            } else {
                                for (int i = 0; i < files.length; i++) {
                                    writer.println("[" + (i + 1) + "] " + files[i]);
                                }
                            }
                            writer.flush();
                        } catch (Exception e) {
                            writer.println("File Listing Error: " + e.getMessage());
                            writer.flush();
                        }
                        break;
                    case "QUIT":
                        writer.println("SUCCESS: Disconnecting.");
                        return;
                    default:
                        writer.println("ERROR: Unknown command.");
                        break;
                }
            }
        } catch (Exception e) {
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