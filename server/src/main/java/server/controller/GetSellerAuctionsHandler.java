package server.controller;

import java.util.List;
import com.google.gson.Gson;
import shared.utils.GsonUtils;
import server.service.AuctionService;
import shared.models.Auction;
import shared.network.Request;
import shared.network.Response;

public class GetSellerAuctionsHandler implements RequestHandler {
    private final AuctionService auctionService;
    private final Gson gson = GsonUtils.createGson();

    public GetSellerAuctionsHandler(AuctionService auctionService) {
        this.auctionService = auctionService;
    }

    @Override
    public Response handle(Request request, ClientHandler clientHandler) {
        String sellerUsername = request.getData().get("username");
        List<Auction> sellerAuctions = auctionService.getAuctionsBySeller(sellerUsername);
        return new Response("SUCCESS", gson.toJson(sellerAuctions));
    }
}
