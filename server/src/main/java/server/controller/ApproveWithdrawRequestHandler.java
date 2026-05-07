package server.controller;

import server.service.WalletService;
import shared.network.Request;
import shared.network.Response;

public class ApproveWithdrawRequestHandler implements RequestHandler {
    private final WalletService walletService;

    public ApproveWithdrawRequestHandler(WalletService walletService) {
        this.walletService = walletService;
    }

    @Override
    public Response handle(Request request, ClientHandler clientHandler) {
        String approveWithdrawId = request.getData().get("requestId");
        String approveWithdrawAdmin = clientHandler.getLoggedInUsername();
        String approveWithdrawResult = walletService.approveWithdraw(approveWithdrawId, approveWithdrawAdmin);
        if (approveWithdrawResult == null) {
            return new Response("SUCCESS", "Withdraw request approved successfully");
        } else {
            return new Response("FAIL", approveWithdrawResult);
        }
    }
}