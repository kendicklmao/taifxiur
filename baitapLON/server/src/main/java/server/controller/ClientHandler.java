package server.controller;

import com.google.gson.Gson;
import server.service.UserService;
import shared.models.User;
import shared.network.Request;
import shared.network.Response;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class ClientHandler implements Runnable {
    private static final List<ClientHandler> activeClients = new CopyOnWriteArrayList<>();

    private final Socket socket;
    private final Gson gson = new Gson();
    private PrintWriter out;

    private static final UserService userService = new UserService();

    public ClientHandler(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            activeClients.add(this);

            String clientMessage;
            while ((clientMessage = in.readLine()) != null) {
                System.out.println("Server received: " + clientMessage);

                Request request = gson.fromJson(clientMessage, Request.class);
                Response response = handleRequest(request);

                if (response != null) {
                    sendMessage(gson.toJson(response));
                }
            }
        } catch (Exception e) {
            System.out.println("Client disconnected: " + socket.getInetAddress());
        } finally {
            activeClients.remove(this);
        }
    }

    public void sendMessage(String jsonMessage) {
        if (out != null) {
            out.println(jsonMessage);
        }
    }

    public static void broadcast(String message) {
        for (ClientHandler client : activeClients) {
            client.sendMessage(message);
        }
    }

    private Response handleRequest(Request request) {
        String action = request.getAction();
        if (action == null) return new Response("FAIL", "Invalid action");

        switch (action) {
            case "LOGIN":
                String user = request.getData().get("username");
                String pass = request.getData().get("password");

                User loggedInUser = userService.login(user, pass);
                if (loggedInUser != null) {
                    return new Response("SUCCESS", loggedInUser.getRole().toString() + "," + loggedInUser.getUsername());
                } else {
                    return new Response("FAIL", "Sai tài khoản hoặc mật khẩu");
                }

            case "REGISTER":
                String rUser = request.getData().get("username");
                String rPass = request.getData().get("password");
                String rEmail = request.getData().get("email");
                String rQ1 = request.getData().get("q1");
                String rA1 = request.getData().get("a1");
                String rQ2 = request.getData().get("q2");
                String rA2 = request.getData().get("a2");

                shared.enums.Role rRole = shared.enums.Role.valueOf(request.getData().get("role"));

                boolean isRegistered = userService.register(rUser, rPass, rEmail, rQ1, rA1, rQ2, rA2, rRole);

                if (isRegistered) {
                    return new Response("SUCCESS", "Registered successfully");
                } else {
                    return new Response("FAIL", "Username is already taken or is invalid");
                }

            case "GET_AUCTIONS":
                return new Response("SUCCESS", "[]");

            case "PLACE_BID":
                String auctionId = request.getData().get("auctionId");
                String amount = request.getData().get("amount");

                Response updateResponse = new Response("UPDATE_PRICE", "UPDATE: Auction " + auctionId + " just had a new price: " + amount);
                broadcast(gson.toJson(updateResponse));

                return null;

            default:
                return new Response("FAIL", "Unsupported function");
        }
    }
}