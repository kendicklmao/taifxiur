package server.controller;

import com.google.gson.Gson;

import server.service.AuctionService;
import server.service.UserService;
import server.service.WalletService;
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
import shared.models.AdminActionLog;
import shared.network.Request;
import shared.network.Response;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.net.Socket;
import java.time.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

public class ClientHandler implements Runnable {
    private static final List<ClientHandler> activeClients = new CopyOnWriteArrayList<>();

    private final Socket socket;
    private final Gson gson = GsonUtils.createGson();
    private final AuctionService auctionService;

    private final UserService userService;
    private String loggedInUsername;
    private BufferedReader in;
    private static final WalletService walletService = new WalletService();
    private PrintWriter out;

    public ClientHandler(Socket socket) throws java.io.IOException {
        this.socket = socket;
        this.userService = new UserService();
        this.auctionService = new AuctionService();
        this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        this.out = new PrintWriter(socket.getOutputStream(), true);
    }

    // Constructor for testing with mock services and streams
    public ClientHandler(Socket socket, UserService userService, AuctionService auctionService, BufferedReader in, PrintWriter out) {
        this.socket = socket;
        this.userService = userService;
        this.auctionService = auctionService;
        this.in = in;
        this.out = out;
    }

    @Override
    public void run() {
        try {

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

                try {
                    User loggedInUser = userService.login(user, pass);
                    if (loggedInUser != null) {
                        this.loggedInUsername = loggedInUser.getUsername(); // Track logged in username
                        return new Response("SUCCESS", loggedInUser.getRole().toString() + "," + loggedInUser.getUsername());
                    } else {
                        return new Response("FAIL", "Invalid username or password");
                    }
                } catch (server.service.UserAlreadyLoggedInException e) {
                    return new Response("FAIL", e.getMessage());
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

            case "FORGOT_PASSWORD_INIT":
                String fpUser = request.getData().get("username");
                String fpEmail = request.getData().get("email");
                String[] questions = userService.getSecurityQuestions(fpUser, fpEmail);

                if (questions != null) {
                    Map<String, String> payload = new HashMap<>();
                    payload.put("q1", questions[0]);
                    payload.put("q2", questions[1]);
                    return new Response("SUCCESS", gson.toJson(payload));
                } else {
                    return new Response("FAIL", "Account information does not match");
                }

            case "RESET_PASSWORD":
                String rpUser = request.getData().get("username");
                String rpEmail = request.getData().get("email");
                String rpAnswer1 = request.getData().get("a1");
                String rpAnswer2 = request.getData().get("a2");
                String rpNewPassword = request.getData().get("newPassword");

                boolean resetSuccess = userService.resetPassword(rpUser, rpEmail, rpAnswer1, rpAnswer2, rpNewPassword);
                if (resetSuccess) {
                    return new Response("SUCCESS", "Password reset successfully");
                } else {
                    return new Response("FAIL", "Security answers are incorrect or the new password is invalid");
                }

            case "CHANGE_PASSWORD":
                String cpUser = request.getData().get("username");
                String cpOldPassword = request.getData().get("oldPassword");
                String cpNewPassword = request.getData().get("newPassword");

                String changePasswordError = userService.changePassword(cpUser, cpOldPassword, cpNewPassword);
                if (changePasswordError == null) {
                    return new Response("SUCCESS", "Password changed successfully");
                } else {
                    return new Response("FAIL", changePasswordError);
                }

            case "GET_WALLET_BALANCE":
                String wbUser = request.getData().get("username");
                BigDecimal walletBalance = walletService.getWalletBalance(wbUser);
                if (walletBalance != null) {
                    return new Response("SUCCESS", walletBalance.toPlainString());
                } else {
                    return new Response("FAIL", "Cannot load wallet balance");
                }

            case "CREATE_DEPOSIT_REQUEST":
                try {
                    String drUser = request.getData().get("username");
                    BigDecimal drAmount = new BigDecimal(request.getData().get("amount"));
                    String depositError = walletService.createDepositRequest(drUser, drAmount);
                    if (depositError == null) {
                        return new Response("SUCCESS", "Deposit request sent to admin");
                    }
                    return new Response("FAIL", depositError);
                } catch (Exception e) {
                    return new Response("FAIL", "Invalid deposit amount");
                }

            case "CREATE_WITHDRAW_REQUEST":
                try {
                    String wrUser = request.getData().get("username");
                    BigDecimal wrAmount = new BigDecimal(request.getData().get("amount"));
                    String bankName = request.getData().get("bankName");
                    String accountNumber = request.getData().get("accountNumber");
                    String withdrawError = walletService.createWithdrawRequest(wrUser, wrAmount, bankName, accountNumber);
                    if (withdrawError == null) {
                        return new Response("SUCCESS", "Withdraw request sent to admin");
                    }
                    return new Response("FAIL", withdrawError);
                } catch (Exception e) {
                    return new Response("FAIL", "Invalid withdraw information");
                }

            case "GET_PENDING_DEPOSIT_REQUESTS":
                if (!isAdminLoggedIn()) {
                    return new Response("FAIL", "Only admin can view deposit requests");
                }
                return new Response("SUCCESS", gson.toJson(walletService.getPendingDepositRequests()));

            case "GET_PENDING_WITHDRAW_REQUESTS":
                if (!isAdminLoggedIn()) {
                    return new Response("FAIL", "Only admin can view withdraw requests");
                }
                return new Response("SUCCESS", gson.toJson(walletService.getPendingWithdrawRequests()));

            case "APPROVE_DEPOSIT_REQUEST":
                String adrId = request.getData().get("requestId");
                String approveDepositError = walletService.approveDeposit(adrId, loggedInUsername);
                if (approveDepositError == null) {
                    return new Response("SUCCESS", "Deposit request approved");
                }
                return new Response("FAIL", approveDepositError);

            case "REJECT_DEPOSIT_REQUEST":
                String rdrId = request.getData().get("requestId");
                String rejectDepositError = walletService.rejectDeposit(rdrId, loggedInUsername);
                if (rejectDepositError == null) {
                    return new Response("SUCCESS", "Deposit request rejected");
                }
                return new Response("FAIL", rejectDepositError);

            case "APPROVE_WITHDRAW_REQUEST":
                String awrId = request.getData().get("requestId");
                String approveWithdrawError = walletService.approveWithdraw(awrId, loggedInUsername);
                if (approveWithdrawError == null) {
                    return new Response("SUCCESS", "Withdraw request approved");
                }
                return new Response("FAIL", approveWithdrawError);

            case "REJECT_WITHDRAW_REQUEST":
                String rwrId = request.getData().get("requestId");
                String rejectWithdrawError = walletService.rejectWithdraw(rwrId, loggedInUsername);
                if (rejectWithdrawError == null) {
                    return new Response("SUCCESS", "Withdraw request rejected");
                }
                return new Response("FAIL", rejectWithdrawError);

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
                boolean banSuccess = userService.banUser(banUsername, loggedInUsername);
                if (banSuccess) {
                    return new Response("SUCCESS", "User banned successfully");
                } else {
                    return new Response("FAIL", "Failed to ban user");
                }

            case "UNBAN_USER":
                String unbanUsername = request.getData().get("username");
                boolean unbanSuccess = userService.unbanUser(unbanUsername, loggedInUsername);
                if (unbanSuccess) {
                    return new Response("SUCCESS", "User unbanned successfully");
                } else {
                    return new Response("FAIL", "Failed to unban user");
                }

            case "GET_ADMIN_ACTION_LOGS":
                List<AdminActionLog> logs = userService.getAdminActionLogs();
                return new Response("SUCCESS", gson.toJson(logs));

            default:
                return new Response("FAIL", "Unsupported function");
        }
    }

    private boolean isAdminLoggedIn() {
        if (loggedInUsername == null) {
            return false;
        }
        User user = userService.getUser(loggedInUsername);
        return user != null && "ADMIN".equals(user.getRole().name());
    }
}
