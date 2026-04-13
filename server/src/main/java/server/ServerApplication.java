package server;

import server.controller.ClientHandler;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class ServerApplication {
    private static final int PORT = 54321;

    public static void main(String[] args) {
        System.out.println("Initializing...");

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Server open at port" + PORT + ". Waiting for connections...");

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("New client connected: " + clientSocket.getInetAddress());
                new Thread(new ClientHandler(clientSocket)).start();
            }

        } catch (IOException e) {
            System.err.println("Error starting server: " + e.getMessage());
        }
    }
}