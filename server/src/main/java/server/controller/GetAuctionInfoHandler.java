package server.controller;

import java.util.HashMap;
import java.util.Map;
import com.google.gson.Gson;
import shared.utils.GsonUtils;
import server.service.AuctionService;
import shared.models.Auction;
import shared.network.Request;
import shared.network.Response;

public class GetAuctionInfoHandler implements RequestHandler {
    private final AuctionService auctionService;
    private final Gson gson = GsonUtils.createGson();

    public GetAuctionInfoHandler(AuctionService auctionService) {
        this.auctionService = auctionService;
    }

    @Override
    public Response handle(Request request, ClientHandler clientHandler) {
        String infoAuctionId = request.getData().get("auctionId");
        if (infoAuctionId == null || infoAuctionId.isBlank()) {
            return new Response("FAIL", "Missing auctionId");
        }

        Auction infoAuction = auctionService.getAuction(infoAuctionId);
        if (infoAuction == null) {
            return new Response("FAIL", "Auction not found");
        }

        Map<String, String> info = new HashMap<>();
        try {
            info.put("id", infoAuction.getId());
            info.put("itemName", infoAuction.getItem().getName());
            info.put("seller", infoAuction.getSeller().getUsername());
            info.put("status", infoAuction.getStatus().toString());
            info.put("currentPrice", infoAuction.getCurrentPrice().toPlainString());
            info.put("startPrice", infoAuction.getStartPrice().toPlainString());
            info.put("minIncrement", infoAuction.getItem().getMinIncrement().toPlainString());
            info.put("startTime", infoAuction.getStartTime().toString());
            info.put("endTime", infoAuction.getEndTime().toString());
            if (infoAuction.getHighestBidder() != null) {
                info.put("highestBidder", infoAuction.getHighestBidder().getUsername());
            } else {
                info.put("highestBidder", "");
            }

        } catch (Exception e) {
            return new Response("FAIL", "Error reading auction info: " + e.getMessage());
        }

        return new Response("SUCCESS", gson.toJson(info));
    }
}