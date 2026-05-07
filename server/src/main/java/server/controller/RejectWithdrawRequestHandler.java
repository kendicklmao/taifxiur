package server.controller;

import server.service.WalletService;
import shared.network.Request;
import shared.network.Response;

public class RejectWithdrawRequestHandler implements RequestHandler {
    private final WalletService walletService;

    public RejectWithdrawRequestHandler(WalletService walletService) {
        this.walletService = walletService;
    }

    @Override
    public Response handle(Request request, ClientHandler clientHandler) {
        String rejectWithdrawId = request.getData().get("requestId");
        String rejectWithdrawAdmin = clientHandler.getLoggedInUsername();
        String rejectWithdrawResult = walletService.rejectWithdraw(rejectWithdrawId, rejectWithdrawAdmin);
        if (rejectWithdrawResult == null) {
            return new Response("SUCCESS", "Withdraw request rejected successfully");
        } else {
            return new Response("FAIL", rejectWithdrawResult);
        }
    }
}