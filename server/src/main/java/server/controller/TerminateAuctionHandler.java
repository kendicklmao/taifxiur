package server.controller;

import server.service.AuctionService;
import server.service.UserService;
import shared.network.Request;
import shared.network.Response;

public class TerminateAuctionHandler implements RequestHandler {

    private final AuctionService auctionService;
    private final UserService userService;

    public TerminateAuctionHandler(AuctionService auctionService, UserService userService) {
        this.auctionService = auctionService;
        this.userService = userService;
    }

    @Override
    public Response handle(Request request, ClientHandler clientHandler) {
        String terminateAuctionId = request.getData().get("auctionId");
        String terminateUsername = request.getData().get("username");

        if (userService.isBanned(terminateUsername)) {
            return new Response("FAIL", "Your account has been banned. You cannot perform this action.");
        }

        String terminateError = auctionService.terminateAuction(terminateAuctionId, terminateUsername);
        if (terminateError == null) {
            return new Response("SUCCESS", "Auction terminated successfully");
        } else {
            return new Response("FAIL", terminateError);
        }
    }
}