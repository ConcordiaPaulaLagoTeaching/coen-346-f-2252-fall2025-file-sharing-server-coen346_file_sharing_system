package ca.concordia.server;
import ca.concordia.filesystem.FileSystemManager;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

public class FileServer {
    private final FileSystemManager fsManager;
    private final int port;

    public FileServer(int port, String fileSystemName, int totalSize) {
        this.fsManager = new FileSystemManager(fileSystemName, totalSize);
        this.port = port;
    }

    public void start() {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Server started. Listening on port " + port + "...");

            // Main server loop. accept clients one by one
            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Handling client: " + clientSocket);

                try (
                        BufferedReader reader =
                                new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                        PrintWriter writer =
                                new PrintWriter(clientSocket.getOutputStream(), true)
                ) {
                    String line;

                    // read commands from the client
                    while ((line = reader.readLine()) != null) {
                        System.out.println("Received from client: " + line);

                        // handle the command and get a response string
                        String response = handleCommand(line);

                        // always send back a response
                        writer.println(response);
                        writer.flush();

                        // when client asks to quit then break
                        if (line.trim().equalsIgnoreCase("QUIT")) {
                            break;
                        }
                    }
                } catch (Exception e) {
                    // log the error but keep the server running
                    e.printStackTrace();
                } finally {
                    try {
                        clientSocket.close();
                    } catch (Exception ignored) {
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Could not start server on port " + port);
        }
    }

    private String handleCommand(String line) {
        // Basic validation for empty input
        if (line == null || line.trim().isEmpty()) {
            return "ERROR: empty command";
        }

        // Split into maximum 3 parts
        String[] parts = line.trim().split("\\s+", 3);
        String command = parts[0].toUpperCase();

        try {
            switch (command) {
                case "CREATE":
                    if (parts.length < 2) {
                        return "ERROR: CREATE requires a filename";
                    }
                    fsManager.createFile(parts[1]);
                    return "OK: created " + parts[1];

                case "WRITE":
                    if (parts.length < 3) {
                        return "ERROR: WRITE requires filename and data";
                    }
                    fsManager.writeFile(parts[1], parts[2].getBytes());
                    return "OK: wrote " + parts[1];

                case "READ":
                    if (parts.length < 2) {
                        return "ERROR: READ requires a filename";
                    }
                    byte[] data = fsManager.readFile(parts[1]);
                    return "OK: " + new String(data);

                case "DELETE":
                    if (parts.length < 2) {
                        return "ERROR: DELETE requires a filename";
                    }
                    fsManager.deleteFile(parts[1]);
                    return "OK: deleted " + parts[1];

                case "LIST":
                    String[] files = fsManager.listFiles();
                    return "OK: " + String.join(",", files);

                case "QUIT":
                    return "OK: goodbye";

                default:
                    return "ERROR: unknown command";
            }
        } catch (Exception e) {
            String msg = e.getMessage();
            if (msg == null || msg.isEmpty()) {
                msg = "internal error";
            }
            return "ERROR: " + msg;
        }
    }
}
