package server.controller;

import server.service.WalletService;
import shared.network.Request;
import shared.network.Response;

public class ApproveDepositRequestHandler implements RequestHandler {
    private final WalletService walletService;

    public ApproveDepositRequestHandler(WalletService walletService) {
        this.walletService = walletService;
    }

    @Override
    public Response handle(Request request, ClientHandler clientHandler) {
        String approveDepositId = request.getData().get("requestId");
        String approveDepositAdmin = clientHandler.getLoggedInUsername();
        String approveDepositResult = walletService.approveDeposit(approveDepositId, approveDepositAdmin);
        if (approveDepositResult == null) {
            return new Response("SUCCESS", "Deposit request approved successfully");
        } else {
            return new Response("FAIL", approveDepositResult);
        }
    }
}