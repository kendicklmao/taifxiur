package server.controller;

import com.google.gson.Gson;

import server.service.AuctionService;
import server.service.UserService;
import shared.utils.GsonUtils;
import shared.enums.AuctionStatus;
import shared.enums.ItemStatus;
import shared.models.Art;
import shared.models.Auction;
import shared.models.Bidder;
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
    private final Gson gson = GsonUtils.createGson();
    private static final AuctionService auctionService = new AuctionService();
    private PrintWriter out;

    private static final UserService userService = new UserService();
    private String loggedInUsername;

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
                    if (request.getRequestId() != null) {
                        response.setRequestId(request.getRequestId());
                    }
                    sendMessage(gson.toJson(response));
                }
            }
        } catch (Exception e) {
            System.out.println("Client disconnected: " + socket.getInetAddress());
        } finally {
            activeClients.remove(this);
            if (loggedInUsername != null) {
                userService.logout(loggedInUsername);
            }
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
                    this.loggedInUsername = loggedInUser.getUsername(); // Track logged in username
                    return new Response("SUCCESS", loggedInUser.getRole().toString() + "," + loggedInUser.getUsername());
                } else {
                    return new Response("FAIL", "Invalid username or password");
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
                // Return all auctions for bidders to see
                List<Auction> list = auctionService.getAllAuctions();

                String json = gson.toJson(list);

                return new Response("SUCCESS", json);

            case "PLACE_BID":
                String pAuctionId = request.getData().get("auctionId");
                String pAmount = request.getData().get("amount");
                String pUsername = request.getData().get("username");

                User pUser = userService.getUser(pUsername);
                if (pUser instanceof Bidder bidder) {
                    BigDecimal amount = new BigDecimal(pAmount);
                    boolean success = auctionService.placeBid(pAuctionId, bidder, amount);
                    if (success) {
                        Response updateResponse = new Response("UPDATE_PRICE", "UPDATE: Auction " + pAuctionId + " just had a new price: " + pAmount);
                        broadcast(gson.toJson(updateResponse));
                        return new Response("SUCCESS", "Bid placed successfully");
                    } else {
                        return new Response("FAIL", "Bid too low or auction not running");
                    }
                } else {
                    return new Response("FAIL", "Invalid user for bidding");
                }

            case "REGISTER_AUTOBID":
                String raAuctionId = request.getData().get("auctionId");
                String raAmount = request.getData().get("maxBid");
                String raUsername = request.getData().get("username");

                User raUser = userService.getUser(raUsername);
                if (raUser instanceof Bidder bidder) {
                    BigDecimal maxBid = new BigDecimal(raAmount);
                    auctionService.registerAutoBid(raAuctionId, bidder, maxBid);
                    return new Response("SUCCESS", "Auto-bid registered successfully");
                } else {
                    return new Response("FAIL", "Invalid user for auto-bidding");
                }

            case "ITEM_PAID":
                String ipAuctionId = request.getData().get("auctionId");
                String ipUsername = request.getData().get("username");

                User ipUser = userService.getUser(ipUsername);
                if (ipUser instanceof Bidder bidder) {
                    auctionService.itemPaid(ipAuctionId, bidder);
                    return new Response("SUCCESS", "Payment processed");
                } else {
                    return new Response("FAIL", "Invalid user for payment");
                }

            case "GET_FINISHED_AUCTIONS":
                List<Auction> finishedList = auctionService.getAuctionsByStatus(AuctionStatus.FINISHED);
                return new Response("SUCCESS", gson.toJson(finishedList));
            case "CREATE_AUCTION":
                try {
                    var data = request.getData();
                    String name = data.get("name");
                    String desc = data.get("description");
                    BigDecimal price = new BigDecimal(data.get("price"));

                    Instant start = Instant.parse(data.get("startTime"));
                    Instant end = Instant.parse(data.get("endTime"));

                    String category = data.get("category");

                    String username = data.get("username");
                    User u = userService.getUser(username);

                    if (u == null || !(u instanceof Seller)) {
                        return new Response("FAIL", "Invalid user");
                    }

                    Seller seller = (Seller) u;
                    System.out.println("seller = " + seller);

                    Item item = null;
                    if (category.equals("COLLECTIBLES")) {
                        int year = Integer.parseInt(data.getOrDefault("yearField", "0"));
                        item = new Collectible(name, desc, seller, year);
                    }

                    else if (category.equals("ELECTRONICS")) {
                        String brand = data.getOrDefault("brandField", "Default");
                        ItemStatus status = ItemStatus.valueOf(data.getOrDefault("statusField", "NEW").toUpperCase());
                        item = new Electronic(name, desc, seller, brand, status);
                    }

                    else if (category.equals("ARTS")) {
                        String artist = data.getOrDefault("artistField", "Unknown");
                        int year = Integer.parseInt(data.getOrDefault("yearField", "0"));
                        boolean original = Boolean.parseBoolean(data.getOrDefault("originalBox", "false"));
                        item = new Art(name, desc, seller, artist, year, original);
                    }

                    else if (category.equals("VEHICLES")) {
                        String brand = data.getOrDefault("brandField", "Unknown");
                        int model = Integer.parseInt(data.getOrDefault("modelField", "0"));
                        int km = Integer.parseInt(data.getOrDefault("kmField", "0"));
                        item = new Vehicle(name, desc, seller, brand, model, km);
                    }

                    else if (category.equals("FASHIONS")) {
                        String brand = data.getOrDefault("brandField", "Brand");
                        ItemStatus status = ItemStatus.valueOf(data.getOrDefault("statusField", "NEW").toUpperCase());
                        item = new Fashion(name, desc, seller, brand, status);
                    }

                    if (item != null) {
                        seller.addItem(item);
                    }

                    Auction auction = auctionService.createAuction(seller, item, price, start, end);

                    return new Response("SUCCESS", gson.toJson(auction));

                } catch (Exception e) {
                    System.out.println("💥 ERROR CREATE AUCTION: " + e.getMessage());
                    e.printStackTrace();
                    return new Response("FAIL", "create auction failed: " + e.getMessage());
                }

            case "GET_SELLER_AUCTIONS":
                String sellerUsername = request.getData().get("username");
                List<Auction> sellerAuctions = auctionService.getAuctionsBySeller(sellerUsername);
                return new Response("SUCCESS", gson.toJson(sellerAuctions));

            case "LOGOUT":
                if (loggedInUsername != null) {
                    userService.logout(loggedInUsername);
                    loggedInUsername = null;
                }
                return new Response("SUCCESS", "Logged out");

            case "GET_ALL_USERS":
                List<User> allUsers = userService.getAllUsers();
                return new Response("SUCCESS", gson.toJson(allUsers));

            case "BAN_USER":
                String banUsername = request.getData().get("username");
                boolean banSuccess = userService.banUser(banUsername);
                if (banSuccess) {
                    return new Response("SUCCESS", "User banned successfully");
                } else {
                    return new Response("FAIL", "Failed to ban user");
                }

            case "UNBAN_USER":
                String unbanUsername = request.getData().get("username");
                boolean unbanSuccess = userService.unbanUser(unbanUsername);
                if (unbanSuccess) {
                    return new Response("SUCCESS", "User unbanned successfully");
                } else {
                    return new Response("FAIL", "Failed to unban user");
                }

            default:
                return new Response("FAIL", "Unsupported function");
        }
    }
}
