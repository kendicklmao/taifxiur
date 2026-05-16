package server.controller;

import com.google.gson.Gson;
import server.service.AuctionService;
import server.service.UserService;
import shared.network.Request;
import shared.network.Response;

public class BanUserHandler implements RequestHandler {
    private final UserService userService;
    private final AuctionService auctionService;

    public BanUserHandler(UserService userService, AuctionService auctionService) {
        this.userService = userService;
        this.auctionService = auctionService;
    }

    @Override
    public Response handle(Request request, ClientHandler clientHandler) {
        String banUsername = request.getData().get("username");
        String banError = userService.banUser(banUsername, clientHandler.getLoggedInUsername());
        if (banError == null) {
            // Tự động dừng tất cả các phiên đấu giá của người bị ban
            auctionService.terminateAllAuctionsBySeller(banUsername, clientHandler.getLoggedInUsername());
            
            Response broadcastRes = new Response("USER_BANNED", "User " + banUsername + " has been banned");
            ClientHandler.broadcast(new Gson().toJson(broadcastRes));
            return new Response("SUCCESS", "User banned and all their auctions terminated successfully");
        } else {
            return new Response("FAIL", banError);
        }
    }
}