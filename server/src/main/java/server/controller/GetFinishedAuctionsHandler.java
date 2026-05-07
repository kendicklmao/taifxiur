package server.controller;

import java.util.List;
import com.google.gson.Gson;
import shared.utils.GsonUtils;
import server.service.AuctionService;
import shared.enums.AuctionStatus;
import shared.models.Auction;
import shared.network.Request;
import shared.network.Response;

public class GetFinishedAuctionsHandler implements RequestHandler {
    private final AuctionService auctionService;
    private final Gson gson = GsonUtils.createGson();

    public GetFinishedAuctionsHandler(AuctionService auctionService) {
        this.auctionService = auctionService;
    }

    @Override
    public Response handle(Request request, ClientHandler clientHandler) {
        List<Auction> finishedList = auctionService.getAuctionsByStatus(AuctionStatus.FINISHED);
        return new Response("SUCCESS", gson.toJson(finishedList));
    }
}
