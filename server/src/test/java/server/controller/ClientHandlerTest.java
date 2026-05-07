package server.controller;

import com.google.gson.Gson;
import org.junit.jupiter.api.*;
import server.database.DatabaseConfig;
import server.database.DatabaseInitializer;
import server.service.AuctionService;
import server.service.StorageService;
import server.service.UserService;
import server.service.WalletService;
import shared.network.Request;
import shared.network.Response;
import shared.utils.GsonUtils;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ClientHandlerTest {

    private static ServerSocket serverSocket;
    private static Thread serverThread;
    private static UserService userService;
    private static WalletService walletService;
    private static AuctionService auctionService;
    private static StorageService storageService;

    private Socket clientSocket;
    private PrintWriter out;
    private BufferedReader in;
    private Gson gson;

    @BeforeAll
    public static void setUpClass() throws Exception {
        DatabaseInitializer.initializeDatabase();
        userService = new UserService();
        walletService = new WalletService();
        auctionService = new AuctionService(userService, walletService);
        storageService = new StorageService();
        Map<String, RequestHandler> handlers = HandlerFactory.createHandlers(userService, auctionService, walletService, storageService);

        serverSocket = new ServerSocket(0);
        serverThread = new Thread(() -> {
            try {
                while (!Thread.currentThread().isInterrupted()) {
                    Socket socket = serverSocket.accept();
                    ClientHandler clientHandler = new ClientHandler(socket, userService, handlers);
                    new Thread(clientHandler).start();
                }

            } catch (IOException e) {
            }

        });
        serverThread.start();
    }

    @BeforeEach
    public void setUp() throws Exception {
        cleanupDatabase();
        userService.initializeDefaultUsers();

        clientSocket = new Socket("localhost", serverSocket.getLocalPort());
        out = new PrintWriter(clientSocket.getOutputStream(), true);
        in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        gson = GsonUtils.createGson();
    }

    @AfterEach
    public void tearDown() throws IOException {
        cleanupDatabase();
        if (clientSocket != null) {
            clientSocket.close();
        }  
    }

    @AfterAll
    public static void tearDownClass() throws IOException {
        serverThread.interrupt();
        serverSocket.close();
        DatabaseConfig.closeDataSource();
    }

    private void cleanupDatabase() {
    }

    @Test
    public void testLoginSuccess() throws Exception {
        Map<String, String> data = new HashMap<>();
        data.put("username", "bidder");
        data.put("password", "Bidder@123");
        Request request = new Request("LOGIN", data);
        String requestJson = gson.toJson(request);

        out.println(requestJson);
        String responseJson = in.readLine();
        Response response = gson.fromJson(responseJson, Response.class);

        assertEquals("SUCCESS", response.getStatus());
        assertEquals("BIDDER,bidder", response.getMessage());
    }

    @Test
    public void testLoginFailure() throws Exception {
        Map<String, String> data = new HashMap<>();
        data.put("username", "bidder");
        data.put("password", "WrongPassword");
        Request request = new Request("LOGIN", data);
        String requestJson = gson.toJson(request);

        out.println(requestJson);
        String responseJson = in.readLine();
        Response response = gson.fromJson(responseJson, Response.class);

        assertEquals("FAIL", response.getStatus());
        assertEquals("Invalid username or password", response.getMessage());
    }

    @Test
    public void testRegisterSuccess() throws Exception {
        String newUser = "newbidder" + System.currentTimeMillis();
        Map<String, String> data = new HashMap<>();
        data.put("username", newUser);
        data.put("password", "Password@123");
        data.put("email", newUser + "@test.com");
        data.put("q1", "q");
        data.put("a1", "a");
        data.put("q2", "q");
        data.put("a2", "a");
        data.put("role", "BIDDER");
        Request request = new Request("REGISTER", data);
        String requestJson = gson.toJson(request);

        out.println(requestJson);
        String responseJson = in.readLine();
        Response response = gson.fromJson(responseJson, Response.class);

        assertEquals("SUCCESS", response.getStatus());
        assertEquals("Registered successfully", response.getMessage());
    }
}