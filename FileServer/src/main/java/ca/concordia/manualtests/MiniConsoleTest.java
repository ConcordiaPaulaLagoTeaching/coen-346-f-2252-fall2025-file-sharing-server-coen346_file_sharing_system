package ca.concordia.manualtests;

import ca.concordia.filesystem.FileSystemManager;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

public class MiniConsoleTest {
    public static void main(String[] args) {
        try {
            FileSystemManager fs = new FileSystemManager("virtual_disk.dat", 1280);
            Scanner sc = new Scanner(System.in);
            System.out.println("=== Mini FileSystem Console ===");
            System.out.println("Commands: CREATE <name>, WRITE <name> <text>, READ <name>, DELETE <name>, LIST, EXIT");

            while (true) {
                System.out.print("> ");
                String commandLine = sc.nextLine().trim();
                if (commandLine.equalsIgnoreCase("EXIT")) break;

                String[] parts = commandLine.split(" ", 3);
                if (parts.length == 0) continue;

                switch (parts[0].toUpperCase()) {
                    case "CREATE":
                        fs.createFile(parts[1]);
                        System.out.println("Created " + parts[1]);
                        break;
                    case "WRITE":
                        fs.writeFile(parts[1], parts[2].getBytes(StandardCharsets.UTF_8));
                        System.out.println("Wrote to " + parts[1]);
                        break;
                    case "READ":
                        byte[] data = fs.readFile(parts[1]);
                        System.out.println("Contents: " + new String(data, StandardCharsets.UTF_8));
                        break;
                    case "DELETE":
                        fs.deleteFile(parts[1]);
                        System.out.println("Deleted " + parts[1]);
                        break;
                    case "LIST":
                        for (String name : fs.listFiles()) System.out.println(" - " + name);
                        break;
                    default:
                        System.out.println("Unknown command.");
                }
            }
            System.out.println("Exiting Mini FileSystem Console.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

