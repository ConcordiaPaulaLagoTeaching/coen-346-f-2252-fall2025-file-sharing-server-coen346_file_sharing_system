package ca.concordia.server;

import ca.concordia.filesystem.datastructures.FileSystemManager;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ClientHandler extends Thread {

    private final Socket socket;
    private final FileSystemManager fs;

    public ClientHandler(Socket socket, FileSystemManager fs) {
        this.socket = socket;
        this.fs = fs;
    }

    @Override
    public void run() {
        try (
                BufferedReader in = new BufferedReader(
                        new InputStreamReader(socket.getInputStream()));
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true)
        ) {

            String line;
            while ((line = in.readLine()) != null) {

                String[] parts = line.split(" ", 3);
                String cmd = parts[0].toUpperCase();

                switch (cmd) {

                    case "CREATE":
                        if (parts.length < 2) { out.println("ERROR"); break; }
                        try { fs.createFile(parts[1]); out.println("OK"); }
                        catch (Exception e) { out.println("ERROR"); }
                        break;

                    case "DELETE":
                        if (parts.length < 2) { out.println("ERROR"); break; }
                        try { fs.deleteFile(parts[1]); out.println("OK"); }
                        catch (Exception e) { out.println("ERROR"); }
                        break;

                    case "WRITE":
                        if (parts.length < 3) { out.println("ERROR"); break; }
                        try {
                            fs.writeFile(parts[1], parts[2].getBytes());
                            out.println("OK");
                        } catch (Exception e) {
                            out.println("ERROR");
                        }
                        break;

                    case "READ":
                        if (parts.length < 2) { out.println("ERROR"); break; }
                        try {
                            byte[] data = fs.readFile(parts[1]);
                            out.println(new String(data));
                        } catch (Exception e) {
                            out.println("ERROR");
                        }
                        break;

                    case "LIST":
                        String[] list = fs.listFiles();
                        out.println(String.join(",", list));
                        break;

                    default:
                        out.println("ERROR");
                }
            }

        } catch (Exception ignored) {}
    }
}
