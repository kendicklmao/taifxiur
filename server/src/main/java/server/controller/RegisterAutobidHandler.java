package server.controller;

import java.math.BigDecimal;

import server.service.AuctionService;
import server.service.UserService;
import shared.models.Bidder;
import shared.models.User;
import shared.network.Request;
import shared.network.Response;

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

        User raUser = userService.getUser(raUsername);
        if (raUser instanceof Bidder bidder) {
            BigDecimal maxBid = new BigDecimal(raAmount);
            auctionService.registerAutoBid(raAuctionId, bidder, maxBid);
            return new Response("SUCCESS", "Auto-bid registered successfully");
        } else {
            return new Response("FAIL", "Invalid user for auto-bidding");
        }
    }
}
