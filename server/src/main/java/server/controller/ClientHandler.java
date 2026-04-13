package server.controller;

import com.google.gson.Gson;

import server.service.AuctionService;
import server.service.UserService;
import shared.enums.AuctionStatus;
import shared.enums.ItemStatus;
import shared.models.Art;
import shared.models.Auction;
import shared.models.Collectible;
import shared.models.Electronic;
import shared.models.Fashion;
import shared.models.Item;
import shared.models.Seller;
import shared.models.User;
import shared.models.Vehicle;
import shared.network.Request;
import shared.network.Response;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.net.Socket;
import java.time.*;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class ClientHandler implements Runnable {
    private static final List<ClientHandler> activeClients = new CopyOnWriteArrayList<>();

    private final Socket socket;
    private final Gson gson = new Gson();
    private static final AuctionService auctionService = new AuctionService();
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
                    return new Response("FAIL", "Sai tài khoản hoặc mật khẩu1");
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

                List<Auction> list = auctionService.getAuctionsByStatus(AuctionStatus.RUNNING);

                String json = gson.toJson(list);

                return new Response("SUCCESS", json);

            case "PLACE_BID":
                String auctionId = request.getData().get("auctionId");
                String amount = request.getData().get("amount");

                Response updateResponse = new Response("UPDATE_PRICE", "UPDATE: Auction " + auctionId + " just had a new price: " + amount);
                broadcast(gson.toJson(updateResponse));

                return null;
            case "CREATE_AUCTION":
                try {
                    var data = request.getData();
                    String name = data.get("name");
                    String desc = data.get("description");
                    BigDecimal price = new BigDecimal(data.get("price"));

                    LocalDate startDate = LocalDate.parse(data.get("startDate"));
                    LocalDate endDate = LocalDate.parse(data.get("endDate"));

                    Instant start = startDate.atStartOfDay(ZoneId.systemDefault()).toInstant();
                    Instant end = endDate.atStartOfDay(ZoneId.systemDefault()).toInstant();

                    String category = data.get("category");

                    String username = data.get("username");
                    User u = userService.getUser(username);

                    if (u == null || !(u instanceof Seller)) {
                        return new Response("FAIL", "User không hợp lệ");
                    }

                    Seller seller = (Seller) u;
                    System.out.println("seller = " + seller);

                    Item item = null;

                    if (category.equals("COLLECTIBLES")) {
                        int year = Integer.parseInt(data.get("year"));
                        item = new Collectible(name, desc, seller, year);
                    }

                    else if (category.equals("ELECTRONICS")) {
                        item = new Electronic(name, desc, seller, "Default", ItemStatus.NEW);
                    }

                    else if (category.equals("ARTS")) {
                        item = new Art(name, desc, seller, "Unknown", 2020, true);
                    }

                    else if (category.equals("VEHICLES")) {
                        item = new Vehicle(name, desc, seller, "Unknown", 2020, 0);
                    }

                    else if (category.equals("FASHIONS")) {
                        item = new Fashion(name, desc, seller, "Brand", ItemStatus.NEW);
                    }

                    Auction auction = auctionService.createAuction(seller, item, price, start, end);

                    return new Response("SUCCESS", gson.toJson(auction));

                } catch (Exception e) {
                    System.out.println("💥 ERROR CREATE AUCTION");
                    e.printStackTrace();
                    return new Response("FAIL", "create auction failed");
                }

            default:
                return new Response("FAIL", "Unsupported function");
        }
    }
}