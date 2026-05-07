package server.controller;

import java.util.List;
import com.google.gson.Gson;
import shared.utils.GsonUtils;
import server.service.AuctionService;
import shared.models.Auction;
import shared.network.Request;
import shared.network.Response;

public class GetAuctionsHandler implements RequestHandler {
    private final AuctionService auctionService;
    private final Gson gson = GsonUtils.createGson();

    public GetAuctionsHandler(AuctionService auctionService) {
        this.auctionService = auctionService;
    }

    @Override
    public Response handle(Request request, ClientHandler clientHandler) {
        List<Auction> list = auctionService.getAllAuctions();
        String json = gson.toJson(list);
        return new Response("SUCCESS", json);
    }
}
