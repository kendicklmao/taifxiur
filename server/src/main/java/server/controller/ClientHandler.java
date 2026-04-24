package server.controller;

import com.google.gson.Gson;

import server.service.AuctionService;
import server.service.StorageService;
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
import shared.models.AdminActionLog;
import shared.models.Vehicle;
import shared.network.Request;
import shared.network.Response;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.net.Socket;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

public class ClientHandler implements Runnable {
    private static final List<ClientHandler> activeClients = new CopyOnWriteArrayList<>();

    private final Socket socket;
    private final Gson gson = GsonUtils.createGson();
    private static final AuctionService auctionService = new AuctionService();
    private static final StorageService storageService = new StorageService();
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

                try {
                    if (userService.exists(user) && userService.isBanned(user)) {
                        return new Response("FAIL", "Your account has been banned");
                    }
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

            case "GET_AUCTIONS":
                // Return all auctions for bidders to see
                List<Auction> list = auctionService.getAllAuctions();

                String json = gson.toJson(list);

                return new Response("SUCCESS", json);

            case "GET_AUCTION_INFO":
                String infoAuctionId = request.getData().get("auctionId");
                if (infoAuctionId == null || infoAuctionId.isBlank()) {
                    return new Response("FAIL", "Missing auctionId");
                }
                Auction infoAuction = auctionService.getAuction(infoAuctionId);
                if (infoAuction == null) {
                    return new Response("FAIL", "Auction not found");
                }
                Map<String, String> info = new HashMap<>();
                try {
                    info.put("id", infoAuction.getId());
                    info.put("itemName", infoAuction.getItem().getName());
                    info.put("seller", infoAuction.getSeller().getUsername());
                    info.put("status", infoAuction.getStatus().toString());
                    info.put("currentPrice", infoAuction.getCurrentPrice().toPlainString());
                    info.put("startPrice", infoAuction.getStartPrice().toPlainString());
                    info.put("minIncrement", infoAuction.getItem().getMinIncrement().toPlainString());
                    info.put("startTime", infoAuction.getStartTime().toString());
                    info.put("endTime", infoAuction.getEndTime().toString());
                    if (infoAuction.getHighestBidder() != null) {
                        info.put("highestBidder", infoAuction.getHighestBidder().getUsername());
                    } else {
                        info.put("highestBidder", "");
                    }
                } catch (Exception e) {
                    return new Response("FAIL", "Error reading auction info: " + e.getMessage());
                }
                return new Response("SUCCESS", gson.toJson(info));

            case "PLACE_BID":
                String pAuctionId = request.getData().get("auctionId");
                String pAmount = request.getData().get("amount");
                String pUsername = request.getData().get("username");

                User pUser = userService.getUser(pUsername);
                if (pUser instanceof Bidder bidder) {
                    BigDecimal amount = new BigDecimal(pAmount);
                    try {
                        boolean success = auctionService.placeBid(pAuctionId, bidder, amount);
                        if (success) {
                            Response updateResponse = new Response("UPDATE_PRICE", "UPDATE: Auction " + pAuctionId + " just had a new price: " + pAmount);
                            broadcast(gson.toJson(updateResponse));
                            return new Response("SUCCESS", "Bid placed successfully");
                        } else {
                            // Provide more detailed feedback to the client to aid debugging
                            server.service.WalletService ws = new server.service.WalletService();
                                BigDecimal avail = ws.getAvailableBalance(pUsername);
                                BigDecimal total = ws.getWalletBalance(pUsername);
                                // Log diagnostic info for debugging
                                System.out.println("PLACE_BID debug -> user=" + pUsername + " requested=" + pAmount + " avail=" + avail + " total=" + total);
                                StringBuilder msg = new StringBuilder();
                                if (avail == null) {
                                    msg.append("Could not determine available balance. ");
                                } else {
                                    BigDecimal held = (total == null) ? BigDecimal.ZERO : total.subtract(avail);
                                    msg.append("Available: ").append(avail.toPlainString()).append(", Held: ").append(held.toPlainString()).append(". ");
                                }
                            // Also include auction-level info if available
                            try {
                                var auction = auctionService.getAuction(pAuctionId);
                                if (auction != null) {
                                    msg.append("CurrentPrice: ").append(auction.getCurrentPrice()).append(", MinIncrement: ").append(auction.getItem().getMinIncrement()).append('.');
                                }
                            } catch (Exception ignored) {}

                            // If available balance insufficient, return that, otherwise general bid failure
                            if (avail == null || avail.compareTo(amount) < 0) {
                                return new Response("FAIL", "Insufficient funds. " + msg.toString());
                            }
                            return new Response("FAIL", "Bid too low or auction not running. " + msg.toString());
                        }
                    } catch (IllegalStateException ise) {
                        return new Response("FAIL", ise.getMessage());
                    } catch (Exception e) {
                        System.err.println("Error during PLACE_BID: " + e.getMessage());
                        e.printStackTrace();
                        return new Response("FAIL", "Error while placing bid: " + e.getMessage());
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

                    LocalDateTime startTime = LocalDateTime.ofInstant(start, ZoneOffset.UTC);
                    LocalDateTime endTime = LocalDateTime.ofInstant(end, ZoneOffset.UTC);

                    String category = data.get("category");
                    String imageBase64 = data.get("image");

                    String username = data.get("username");
                    User u = userService.getUser(username);

                    if (u == null || !(u instanceof Seller)) {
                        return new Response("FAIL", "Invalid user");
                    }

                    Seller seller = (Seller) u;
                    System.out.println("seller = " + seller);

                    String imageUrl = null;
                    if (imageBase64 != null && !imageBase64.isEmpty()) {
                        byte[] imageBytes = Base64.getDecoder().decode(imageBase64);
                        String imageId = UUID.randomUUID().toString();
                        String contentType = data.get("imageContentType");
                        imageUrl = storageService.uploadFile(imageId, imageBytes, contentType);
                    }

                    Item item = null;
                    if (category.equals("COLLECTIBLES")) {
                        int year = Integer.parseInt(data.getOrDefault("yearField", "0"));
                        item = new Collectible(name, desc, seller, year, startTime, endTime);
                    }

                    else if (category.equals("ELECTRONICS")) {
                        String brand = data.getOrDefault("brandField", "Default");
                        ItemStatus status = ItemStatus.valueOf(data.getOrDefault("statusField", "NEW").toUpperCase());
                        item = new Electronic(name, desc, seller, brand, status, startTime, endTime);
                    }

                    else if (category.equals("ARTS")) {
                        String artist = data.getOrDefault("artistField", "Unknown");
                        int year = Integer.parseInt(data.getOrDefault("yearField", "0"));
                        boolean original = Boolean.parseBoolean(data.getOrDefault("originalBox", "false"));
                        item = new Art(name, desc, seller, artist, year, original, startTime, endTime);
                    }

                    else if (category.equals("VEHICLES")) {
                        String brand = data.getOrDefault("brandField", "Unknown");
                        int model = Integer.parseInt(data.getOrDefault("modelField", "0"));
                        int km = Integer.parseInt(data.getOrDefault("kmField", "0"));
                        item = new Vehicle(name, desc, seller, brand, model, km, startTime, endTime);
                    }

                    else if (category.equals("FASHIONS")) {
                        String brand = data.getOrDefault("brandField", "Brand");
                        ItemStatus status = ItemStatus.valueOf(data.getOrDefault("statusField", "NEW").toUpperCase());
                        item = new Fashion(name, desc, seller, brand, status, startTime, endTime);
                    }

                    if (item != null) {
                        item.setImageUrl(imageUrl);
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

            case "GET_PENDING_DEPOSIT_REQUESTS":
                List<Map<String, String>> depositRequests = userService.getPendingDepositRequests();
                return new Response("SUCCESS", gson.toJson(depositRequests));

            case "GET_PENDING_WITHDRAW_REQUESTS":
                List<Map<String, String>> withdrawRequests = userService.getPendingWithdrawRequests();
                return new Response("SUCCESS", gson.toJson(withdrawRequests));

            case "APPROVE_DEPOSIT_REQUEST":
                String approveDepositId = request.getData().get("requestId");
                String approveDepositAdmin = loggedInUsername;
                String approveDepositResult = userService.approveDepositRequest(approveDepositId, approveDepositAdmin);
                if (approveDepositResult == null) {
                    return new Response("SUCCESS", "Deposit request approved successfully");
                } else {
                    return new Response("FAIL", approveDepositResult);
                }

            case "REJECT_DEPOSIT_REQUEST":
                String rejectDepositId = request.getData().get("requestId");
                String rejectDepositAdmin = loggedInUsername;
                String rejectDepositResult = userService.rejectDepositRequest(rejectDepositId, rejectDepositAdmin);
                if (rejectDepositResult == null) {
                    return new Response("SUCCESS", "Deposit request rejected successfully");
                } else {
                    return new Response("FAIL", rejectDepositResult);
                }

            case "APPROVE_WITHDRAW_REQUEST":
                String approveWithdrawId = request.getData().get("requestId");
                String approveWithdrawAdmin = loggedInUsername;
                String approveWithdrawResult = userService.approveWithdrawRequest(approveWithdrawId, approveWithdrawAdmin);
                if (approveWithdrawResult == null) {
                    return new Response("SUCCESS", "Withdraw request approved successfully");
                } else {
                    return new Response("FAIL", approveWithdrawResult);
                }

            case "REJECT_WITHDRAW_REQUEST":
                String rejectWithdrawId = request.getData().get("requestId");
                String rejectWithdrawAdmin = loggedInUsername;
                String rejectWithdrawResult = userService.rejectWithdrawRequest(rejectWithdrawId, rejectWithdrawAdmin);
                if (rejectWithdrawResult == null) {
                    return new Response("SUCCESS", "Withdraw request rejected successfully");
                } else {
                    return new Response("FAIL", rejectWithdrawResult);
                }

            case "CREATE_DEPOSIT_REQUEST":
                String depositUsername = request.getData().get("username");
                BigDecimal depositAmount = new BigDecimal(request.getData().get("amount"));
                String depositError = userService.createDepositRequest(depositUsername, depositAmount);
                if (depositError == null) {
                    return new Response("SUCCESS", "Deposit request created successfully");
                } else {
                    return new Response("FAIL", depositError);
                }

            case "CREATE_WITHDRAW_REQUEST":
                String withdrawUsername = request.getData().get("username");
                BigDecimal withdrawAmount = new BigDecimal(request.getData().get("amount"));
                String bankName = request.getData().get("bankName");
                String accountNumber = request.getData().get("accountNumber");
                String withdrawError = userService.createWithdrawRequest(withdrawUsername, withdrawAmount, bankName, accountNumber);
                if (withdrawError == null) {
                    return new Response("SUCCESS", "Withdraw request created successfully");
                } else {
                    return new Response("FAIL", withdrawError);
                }

            case "GET_WALLET_BALANCE":
                String balanceUsername = request.getData().get("username");
                BigDecimal balance = userService.getWalletBalance(balanceUsername);
                if (balance != null) {
                    return new Response("SUCCESS", balance.toPlainString());
                } else {
                    return new Response("FAIL", "Failed to get wallet balance");
                }

            default:
                return new Response("FAIL", "Unsupported function");
        }
    }
}
