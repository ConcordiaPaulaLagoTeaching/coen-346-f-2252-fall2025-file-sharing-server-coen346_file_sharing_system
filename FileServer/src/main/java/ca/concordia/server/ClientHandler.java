package ca.concordia.server;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

import ca.concordia.filesystem.FileSystemManager;

public class ClientHandler implements Runnable {
    private final Socket clientSocket;
    private final FileSystemManager fsManager;
    
    public ClientHandler(Socket clientSocket, FileSystemManager fsManager) {
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
                    while ((line = reader.readLine()) != null) {

                        System.out.println("Received from client: " + line);
                        String[] parts = line.trim().split(" ",3);
                        String command = parts[0].toUpperCase();
                        try{
                        switch (command) {

                            case "CREATE":
                            if(parts.length < 2) {
                                writer.println("ERROR: Filename required.");
                                break;
                            }   
                                fsManager.createFile(parts[1]);
                                writer.println("SUCCESS: File '" + parts[1] + "' created.");
                                writer.flush();
                                break;
                            //TODO: Implement other commands READ, WRITE, DELETE, LIST
                            
                            case "QUIT":
                                writer.println("SUCCESS: Disconnecting.");
                                return;
                            default:
                                writer.println("ERROR: Unknown command.");
                                break;
                        }
                        } catch (Exception e) {
                            writer.println("ERROR: " + e.getMessage());
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
