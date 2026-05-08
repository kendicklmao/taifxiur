package server.controller;

import java.util.List;

import com.google.gson.Gson;

import server.service.AuctionService;
import shared.models.Auction;
import shared.network.Request;
import shared.network.Response;
import shared.utils.GsonUtils;

public class GetAuctionHandler implements RequestHandler {
    private final AuctionService auctionService;
    private final Gson gson = GsonUtils.createGson();
    
    public GetAuctionHandler(AuctionService auctionService) {
        this.auctionService = auctionService;
    }

    @Override
    public Response handle(Request request, ClientHandler clientHandler) {
        List<Auction> list = auctionService.getAllAuctions();
        String json = gson.toJson(list);
        return new Response("SUCCESS", json);
    }
}