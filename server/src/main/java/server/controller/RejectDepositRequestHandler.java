package server.controller;

import server.service.WalletService;
import shared.network.Request;
import shared.network.Response;

public class RejectDepositRequestHandler implements RequestHandler {
    private final WalletService walletService;

    public RejectDepositRequestHandler(WalletService walletService) {
        this.walletService = walletService;
    }

    @Override
    public Response handle(Request request, ClientHandler clientHandler) {
        String rejectDepositId = request.getData().get("requestId");
        String rejectDepositAdmin = clientHandler.getLoggedInUsername();
        String rejectDepositResult = walletService.rejectDeposit(rejectDepositId, rejectDepositAdmin);
        if (rejectDepositResult == null) {
            return new Response("SUCCESS", "Deposit request rejected successfully");
        } else {
            return new Response("FAIL", rejectDepositResult);
        }
    }
}