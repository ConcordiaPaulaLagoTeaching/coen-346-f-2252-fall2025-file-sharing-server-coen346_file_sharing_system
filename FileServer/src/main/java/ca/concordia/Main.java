package ca.concordia;

import ca.concordia.server.FileServer;

public class Main {
    public static void main(String[] args) {
        System.out.println("Hello and welcome!");

        FileServer server = new FileServer(12345, "filesystem.dat");

        server.start();
    }
}
