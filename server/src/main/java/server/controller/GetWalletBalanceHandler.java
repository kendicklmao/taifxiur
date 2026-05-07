package server.controller;

import java.math.BigDecimal;
import server.service.WalletService;
import shared.network.Request;
import shared.network.Response;

public class GetWalletBalanceHandler implements RequestHandler {
    private final WalletService walletService;

    public GetWalletBalanceHandler(WalletService walletService) {
        this.walletService = walletService;
    }

    @Override
    public Response handle(Request request, ClientHandler clientHandler) {
        String balanceUsername = request.getData().get("username");
        BigDecimal balance = walletService.getWalletBalance(balanceUsername);
        if (balance != null) {
            return new Response("SUCCESS", balance.toPlainString());
        } else {
            return new Response("FAIL", "Failed to get wallet balance");
        }
    }
}