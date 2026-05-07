package server.controller;

import java.util.List;

import com.google.gson.Gson;

import server.service.AuctionService;
import shared.models.Auction;
import shared.network.Request;
import shared.network.Response;

public class GetAuctionHandler implements RequestHandler {
    private final AuctionService auctionService;
    private final Gson gson = new Gson();
    
    public GetAuctionHandler(AuctionService auctionService) {
        this.auctionService = auctionService;
    }

    @Override
    public Response handle(Request request, ClientHandler clientHandler) {
        // Return all auctions for bidders to see
        List<Auction> list = auctionService.getAllAuctions();
        String json = gson.toJson(list);
        return new Response("SUCCESS", json);
    }
}
