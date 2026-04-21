package server;

import server.controller.ClientHandler;
import server.database.DatabaseConfig;
import server.database.DatabaseInitializer;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class ServerApplication {
    private static final int PORT = 54321;

    public static void main(String[] args) {
        // Set Java networking preferences for better IPv4/IPv6 compatibility
        System.setProperty("java.net.preferIPv4Stack", "false");
        System.setProperty("java.net.preferIPv6Addresses", "false");

        System.out.println("Initializing...");

        // Initialize database connection pool and schema
        try {
            DatabaseInitializer.initializeDatabase();
            System.out.println("Database initialization completed");
        } catch (Exception e) {
            System.err.println("Failed to initialize database: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Server open at port " + PORT + ". Waiting for connections...");

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("New client connected: " + clientSocket.getInetAddress());
                new Thread(new ClientHandler(clientSocket)).start();
            }

        } catch (IOException e) {
            System.err.println("Error starting server: " + e.getMessage());
        } finally {
            // Close database connection pool on shutdown
            DatabaseConfig.closeDataSource();
        }
    }
}