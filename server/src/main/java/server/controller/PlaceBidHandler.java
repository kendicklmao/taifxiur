package server.controller;

import java.math.BigDecimal;
import com.google.gson.Gson;
import shared.utils.GsonUtils;
import server.service.AuctionService;
import server.service.WalletService;
import server.service.UserService;
import shared.models.Bidder;
import shared.models.User;
import shared.network.Request;
import shared.network.Response;

public class PlaceBidHandler implements RequestHandler {
    private final AuctionService auctionService;
    private final WalletService walletService;  
    private final UserService userService;
    private final Gson gson = GsonUtils.createGson();

    public PlaceBidHandler(AuctionService auctionService, WalletService walletService, UserService userService) {
        this.auctionService = auctionService;
        this.walletService = walletService;
        this.userService = userService;
    }

    @Override
    public Response handle(Request request, ClientHandler clientHandler) {
        String pAuctionId = request.getData().get("auctionId");
        String pAmount = request.getData().get("amount");
        String pUsername = request.getData().get("username");

        User pUser = userService.getUser(pUsername);
        if (pUser instanceof Bidder bidder) {
            BigDecimal amount = new BigDecimal(pAmount);
            try {
                boolean success = auctionService.placeBid(pAuctionId, bidder, amount);
                if (success) {
                    Response updateResponse = new Response("UPDATE_PRICE", "UPDATE: Auction " + pAuctionId + " just had a new price: " + pAmount);
                    ClientHandler.broadcast(gson.toJson(updateResponse));
                    return new Response("SUCCESS", "Bid placed successfully");
                } else {
                    BigDecimal balance = this.walletService.getWalletBalance(pUsername);
                    System.out.println("PLACE_BID debug -> user=" + pUsername + " requested=" + pAmount + " balance=" + balance);
                    StringBuilder msg = new StringBuilder();
                    if (balance == null) {
                        msg.append("Could not determine balance. ");
                    } else {
                        msg.append("Balance: ").append(balance.toPlainString()).append(". ");
                    }
                    try {
                        var auction = auctionService.getAuction(pAuctionId);
                        if (auction != null) {
                            msg.append("CurrentPrice: ").append(auction.getCurrentPrice()).append(", MinIncrement: ").append(auction.getItem().getMinIncrement()).append('.');
                        }
                    } catch (Exception ignored) {
                    }

                    if (balance == null || balance.compareTo(amount) < 0) {
                        return new Response("FAIL", "Insufficient funds. " + msg.toString());
                    }
                    return new Response("FAIL", "Bid too low or auction not running. " + msg.toString());
                }
            } catch (IllegalStateException ise) {
                return new Response("FAIL", ise.getMessage());
            } catch (Exception e) {
                System.err.println("Error during PLACE_BID: " + e.getMessage());
                e.printStackTrace();
                return new Response("FAIL", "Error while placing bid: " + e.getMessage());
            }
        } else {
            return new Response("FAIL", "Invalid user for bidding");
        }
    }
}
