package server.controller;

import java.math.BigDecimal;
import server.service.UserService;
import shared.network.Request;
import shared.network.Response;

public class GetWalletBalanceHandler implements RequestHandler {
    private final UserService userService;

    public GetWalletBalanceHandler(UserService userService) {
        this.userService = userService;
    }

    @Override
    public Response handle(Request request, ClientHandler clientHandler) {
        String balanceUsername = request.getData().get("username");
        BigDecimal balance = userService.getWalletBalance(balanceUsername);
        if (balance != null) {
            return new Response("SUCCESS", balance.toPlainString());
        } else {
            return new Response("FAIL", "Failed to get wallet balance");
        }
    }
}
