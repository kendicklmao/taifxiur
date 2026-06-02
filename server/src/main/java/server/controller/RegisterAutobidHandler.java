package server.controller;

import java.math.BigDecimal;

import server.service.AuctionService;
import server.service.UserService;
import shared.models.users.Bidder;
import shared.models.users.User;
import shared.network.Request;
import shared.network.Response;
import shared.utils.GsonUtils;
import com.google.gson.Gson;

public class RegisterAutobidHandler implements RequestHandler {
    private final UserService userService;
    private final AuctionService auctionService;
    
    public RegisterAutobidHandler(UserService userService, AuctionService auctionService) {
        this.userService = userService;
        this.auctionService = auctionService;
    }

    @Override
    public Response handle(Request request, ClientHandler clientHandler) {
        String raAuctionId = request.getData().get("auctionId");
        String raAmount = request.getData().get("maxBid");
        String raUsername = request.getData().get("username");

        if (userService.isBanned(raUsername)) {
            return new Response("FAIL", "Your account has been banned. You cannot perform this action.");
        }

        User raUser = userService.getUser(raUsername);
        if (raUser instanceof Bidder bidder) {
            BigDecimal maxBid = new BigDecimal(raAmount);
            try {
                auctionService.registerAutoBid(raAuctionId, bidder, maxBid);
                
                try {
                    var updatedAuction = auctionService.getAuction(raAuctionId);
                    java.util.Map<String, String> payload = new java.util.HashMap<>();
                    payload.put("auctionId", raAuctionId);
                    payload.put("newPrice", updatedAuction.getCurrentPrice().toPlainString());
                    if (updatedAuction.getHighestBidder() != null) {
                        payload.put("highestBidder", updatedAuction.getHighestBidder().getUsername());
                    }
                    payload.put("endTime", updatedAuction.getEndTime().toString());
                    payload.put("bidTime", java.time.Instant.now().toString());
                    Gson gson = GsonUtils.createGson();
                    Response updateResponse = new Response("UPDATE_PRICE", gson.toJson(payload));
                    ClientHandler.broadcast(gson.toJson(updateResponse));
                } catch (Exception e) {
                    e.printStackTrace();
                }

                return new Response("SUCCESS", "Auto-bid registered successfully");
            } catch (IllegalStateException | IllegalArgumentException e) {
                return new Response("FAIL", e.getMessage());
            } catch (Exception e) {
                return new Response("FAIL", "Error during auto-bid registration: " + e.getMessage());
            }
        } else {
            return new Response("FAIL", "Invalid user for auto-bidding");
        }
    }
}