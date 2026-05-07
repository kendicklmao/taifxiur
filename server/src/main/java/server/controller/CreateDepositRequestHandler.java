package server.controller;

import java.math.BigDecimal;
import server.service.WalletService;
import shared.network.Request;
import shared.network.Response;

public class CreateDepositRequestHandler implements RequestHandler {
    private final WalletService walletService;

    public CreateDepositRequestHandler(WalletService walletService) {
        this.walletService = walletService;
    }

    @Override
    public Response handle(Request request, ClientHandler clientHandler) {
        String depositUsername = request.getData().get("username");
        BigDecimal depositAmount = new BigDecimal(request.getData().get("amount"));
        String depositBankName = request.getData().get("bankName");
        String depositAccountNumber = request.getData().get("accountNumber");
        String depositError = walletService.createDepositRequest(depositUsername, depositAmount, depositBankName, depositAccountNumber);
        if (depositError == null) {
            return new Response("SUCCESS", "Deposit request created successfully");
        } else {
            return new Response("FAIL", depositError);
        }
    }
}