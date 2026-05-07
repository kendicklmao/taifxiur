package server; 

import server.controller.*;
import server.database.DatabaseConfig;
import server.database.DatabaseInitializer;
import server.service.AuctionService;
import server.service.StorageService;
import server.service.UserService;
import server.service.WalletService;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;

public class ServerApplication {

    private static final int PORT = 54321;

    private static UserService userService;
    private static WalletService walletService;
    private static AuctionService auctionService;
    private static StorageService storageService;

    public static void main(String[] args) {
        System.setProperty("java.net.preferIPv4Stack", "false");
        System.setProperty("java.net.preferIPv6Addresses", "false");

        System.out.println("Initializing...");
        try {
            DatabaseInitializer.initializeDatabase();

            userService = new UserService();
            walletService = new WalletService();
            auctionService = new AuctionService(userService, walletService);
            storageService = new StorageService();

            try (Connection conn = DatabaseConfig.getDataSource().getConnection();
                 PreparedStatement pstmt = conn.prepareStatement("SELECT 1 FROM users LIMIT 1")) {
                ResultSet rs = pstmt.executeQuery();
                if (!rs.next()) {
                    userService.initializeDefaultUsers();
                }
            } catch (SQLException e) {
                System.err.println("Error checking for existing users: " + e.getMessage());
            }
            
            System.out.println("Database initialization completed");
            
            System.out.println("Loading auctions into memory...");
            System.out.println("All services initialized.");
        } catch (Exception e) {
            System.err.println("Failed to initialize database: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }

        Map<String, RequestHandler> handlers = HandlerFactory.createHandlers(userService, auctionService, walletService, storageService);

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Server open at port " + PORT + ". Waiting for connections...");

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("New client connected: " + clientSocket.getInetAddress());
                new Thread(new ClientHandler(clientSocket, userService, handlers)).start();
            }

        } catch (IOException e) {
            System.err.println("Error starting server: " + e.getMessage());
        } finally {
            DatabaseConfig.closeDataSource();
        }
    }
}