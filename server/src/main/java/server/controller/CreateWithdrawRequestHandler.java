package server.controller;

import java.math.BigDecimal;
import server.service.UserService;
import server.service.WalletService;
import shared.network.Request;
import shared.network.Response;

public class CreateWithdrawRequestHandler implements RequestHandler {
    private final WalletService walletService;
    private final UserService userService;

    public CreateWithdrawRequestHandler(WalletService walletService, UserService userService) {
        this.walletService = walletService;
        this.userService = userService;
    }

    @Override
    public Response handle(Request request, ClientHandler clientHandler) {
        String withdrawUsername = request.getData().get("username");

        if (userService.isBanned(withdrawUsername)) {
            return new Response("FAIL", "Your account has been banned. You cannot perform this action.");
        }

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