package server.controller;

import server.service.AuctionService;
import server.service.UserService;
import shared.models.Bidder;
import shared.models.User;
import shared.network.Request;
import shared.network.Response;

public class ItemPaidHandler implements RequestHandler {
    private final AuctionService auctionService;
    private final UserService userService;

    public ItemPaidHandler(AuctionService auctionService, UserService userService) {
        this.auctionService = auctionService;
        this.userService = userService;
    }

    @Override
    public Response handle(Request request, ClientHandler clientHandler) {
        String ipAuctionId = request.getData().get("auctionId");
        String ipUsername = request.getData().get("username");

        // Check if user is banned
        if (userService.isBanned(ipUsername)) {
            return new Response("FAIL", "Your account has been banned. You cannot perform this action.");
        }

        User ipUser = userService.getUser(ipUsername);
        if (ipUser instanceof Bidder bidder) {
            try {
                auctionService.itemPaid(ipAuctionId, bidder);
                return new Response("SUCCESS", "Payment processed");
            } catch (Exception e) {
                return new Response("FAIL", e.getMessage());
            }
        } else {
            return new Response("FAIL", "Invalid user for payment");
        }
    }
}