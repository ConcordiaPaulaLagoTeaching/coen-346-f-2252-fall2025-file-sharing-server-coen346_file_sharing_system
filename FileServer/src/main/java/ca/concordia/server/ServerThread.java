package ca.concordia.server;

import java.io.IOException;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import ca.concordia.filesystem.FileSystemManager;

/**
 * Basic worker for handling a single client connection for file-related
 * requests.
 * TODO: implement protocol-specific handling inside run().
 */
public class ServerThread extends Thread {
    protected Socket clientSocket;
    private FileSystemManager fsManager;
    private final Semaphore mutex;
    private final Semaphore isEmpty;
    private final Semaphore writeLock;
    private final AtomicInteger readCount;

    public ServerThread(Socket clientSocket, FileSystemManager fsManager,
            Semaphore mutex, Semaphore isEmpty, Semaphore writeLock,
            AtomicInteger readCount) {
        this.clientSocket = clientSocket;
        this.fsManager = fsManager;
        this.mutex = mutex;
        this.isEmpty = isEmpty;
        this.writeLock = writeLock;
        this.readCount = readCount;
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
                            writeLock.acquire();
                            fsManager.createFile(filename);
                            writer.println("SUCCESS: File '" + filename + "' created.");
                            writer.flush();
                        } catch (Exception e) {
                            writer.println("File Creation Error" + e.getMessage());
                            writer.flush();
                        } finally {
                            writeLock.release();
                        }
                        break;
                    case "READ":
                        try {
                            // turnstile for readers
                            writeLock.acquire();
                            writeLock.release();

                            mutex.acquire();
                            if (readCount.getAndIncrement() == 1) {
                                // no longer empty
                                isEmpty.acquire();
                            }
                            // perform read operation
                            byte[] content = fsManager.readFile(filename);
                            mutex.release();

                            writer.println("SUCCESS: File '" + filename + "' read. Content: " + content);
                            writer.flush();
                        } catch (Exception e) {
                            writer.println("File Read Error" + e.getMessage());
                            writer.flush();
                        } finally {
                            try {
                                mutex.acquire();
                                if (readCount.decrementAndGet() == 0) {
                                    // empty now
                                    isEmpty.release();
                                }
                                mutex.release();
                            } catch (Exception e) {
                                // ignore
                            }
                        }
                        break;
                    case "WRITE":
                        try {
                            writeLock.acquire();
                            isEmpty.acquire();

                            // perform write operation
                            fsManager.writeFile(filename, payload.getBytes());
                            writeLock.release();
                            isEmpty.release();

                            writer.println("SUCCESS: File '" + filename + "' written.");
                            writer.flush();
                        } catch (Exception e) {
                            writer.println("File Write Error" + e.getMessage());
                            writer.flush();
                        } finally {
                        }
                        break;
                    case "DELETE":
                        try {
                            writeLock.acquire();
                            isEmpty.acquire();

                            // perform delete operation
                            fsManager.deleteFile(filename);
                            writeLock.release();
                            isEmpty.release();

                            writer.println("SUCCESS: File '" + filename + "' deleted.");
                            writer.flush();
                        } catch (Exception e) {
                            writer.println("File Deletion Error" + e.getMessage());
                            writer.flush();
                        }
                        break;
                    case "LIST":
                        try {
                            // turnstile for readers
                            writeLock.acquire();
                            writeLock.release();

                            mutex.acquire();
                            if (readCount.getAndIncrement() == 1) {
                                // no longer empty
                                isEmpty.acquire();
                            }
                            // perform list operation
                            String[] files = fsManager.listFiles();
                            mutex.release();

                            writer.println("SUCCESS: Files: \n");
                            // print file list
                            for (int i = 0; i < files.length; i++) {
                                writer.println("[" + (i + 1) + "] " + files[i] + " \n");
                            }
                            writer.flush();
                        } catch (Exception e) {
                            writer.println("File Listing Error" + e.getMessage());
                            writer.flush();
                        } finally {
                            try {
                                mutex.acquire();
                                if (readCount.decrementAndGet() == 0) {
                                    // empty now
                                    isEmpty.release();
                                }
                                mutex.release();
                            } catch (Exception e) {
                                // ignore
                            }
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