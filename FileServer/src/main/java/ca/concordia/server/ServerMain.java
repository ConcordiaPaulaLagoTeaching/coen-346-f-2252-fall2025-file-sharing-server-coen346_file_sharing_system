package ca.concordia.server;

public class ServerMain {
    public static void main(String[] args) {
        int port = 12345;
        String diskName = "serverdiskolo.dat";

        FileServer server = new FileServer(port, diskName);
        server.start();
    }
}
