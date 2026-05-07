package server.controller;

import server.service.AuctionService;
import shared.network.Request;
import shared.network.Response;

public class TerminateAuctionHandler implements RequestHandler {

    private final AuctionService auctionService;

    public TerminateAuctionHandler(AuctionService auctionService) {
        this.auctionService = auctionService;
    }

    @Override
    public Response handle(Request request, ClientHandler clientHandler) {
        String terminateAuctionId = request.getData().get("auctionId");
        String terminateUsername = request.getData().get("username");
        String terminateError = auctionService.terminateAuction(terminateAuctionId, terminateUsername);
        if (terminateError == null) {
            return new Response("SUCCESS", "Auction terminated successfully");
        } else {
            return new Response("FAIL", terminateError);
        }
    }
}
