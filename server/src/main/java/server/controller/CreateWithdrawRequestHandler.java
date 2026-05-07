package server.controller;

import java.math.BigDecimal;
import server.service.WalletService;
import shared.network.Request;
import shared.network.Response;

public class CreateWithdrawRequestHandler implements RequestHandler {
    private final WalletService walletService;

    public CreateWithdrawRequestHandler(WalletService walletService) {
        this.walletService = walletService;
    }

    @Override
    public Response handle(Request request, ClientHandler clientHandler) {
        String withdrawUsername = request.getData().get("username");
        BigDecimal withdrawAmount = new BigDecimal(request.getData().get("amount"));
        String bankName = request.getData().get("bankName");
        String accountNumber = request.getData().get("accountNumber");
        String withdrawError = walletService.createWithdrawRequest(withdrawUsername, withdrawAmount, bankName, accountNumber);
        if (withdrawError == null) {
            return new Response("SUCCESS", "Withdraw request created successfully");
        } else {
            return new Response("FAIL", withdrawError);
        }
    }
}