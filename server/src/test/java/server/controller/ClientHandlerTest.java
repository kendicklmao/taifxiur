package server.controller;

import com.google.gson.Gson;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import server.database.DatabaseConfig;
import server.service.UserService;
import shared.network.Request;
import shared.network.Response;
import shared.utils.GsonUtils;

import java.io.*;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ClientHandlerTest {

    private ClientHandler clientHandler;
    private Socket socket;
    private Gson gson;
    private PipedOutputStream clientOutput;
    private ByteArrayOutputStream serverOutput;
    private Thread clientHandlerThread;

    @Before
    public void setUp() throws Exception {
        socket = mock(Socket.class);
        gson = GsonUtils.createGson();

        clientOutput = new PipedOutputStream();
        PipedInputStream serverInput = new PipedInputStream(clientOutput);
        when(socket.getInputStream()).thenReturn(serverInput);

        serverOutput = new ByteArrayOutputStream();
        when(socket.getOutputStream()).thenReturn(serverOutput);

        clientHandler = new ClientHandler(socket);

        clientHandlerThread = new Thread(clientHandler);
        clientHandlerThread.start();

        UserService userService = new UserService();
        userService.initializeDefaultUsers();
    }

    @After
    public void tearDown() throws IOException {
        clientHandlerThread.interrupt();
        clientOutput.close();
        serverOutput.close();
        DatabaseConfig.closeDataSource();
    }

    @Test
    public void testLoginSuccess() throws Exception {
        Map<String, String> data = new HashMap<>();
        data.put("username", "bidder");
        data.put("password", "Admin@123");
        Request request = new Request("LOGIN", data);
        String requestJson = gson.toJson(request) + "\n";

        clientOutput.write(requestJson.getBytes());
        clientOutput.flush();

        Thread.sleep(500);

        String responseJson = serverOutput.toString().trim();
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
        String requestJson = gson.toJson(request) + "\n";

        clientOutput.write(requestJson.getBytes());
        clientOutput.flush();

        Thread.sleep(500);

        String responseJson = serverOutput.toString().trim();
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
        String requestJson = gson.toJson(request) + "\n";

        clientOutput.write(requestJson.getBytes());
        clientOutput.flush();

        Thread.sleep(500);

        String responseJson = serverOutput.toString().trim();
        Response response = gson.fromJson(responseJson, Response.class);

        assertEquals("SUCCESS", response.getStatus());
        assertEquals("Registered successfully", response.getMessage());
    }
}
