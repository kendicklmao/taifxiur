package server.controller;

import java.math.BigDecimal;
import server.service.UserService;
import shared.network.Request;
import shared.network.Response;

public class CreateDepositRequestHandler implements RequestHandler {
    private final UserService userService;

    public CreateDepositRequestHandler(UserService userService) {
        this.userService = userService;
    }

    @Override
    public Response handle(Request request, ClientHandler clientHandler) {
        String depositUsername = request.getData().get("username");
        BigDecimal depositAmount = new BigDecimal(request.getData().get("amount"));
        String depositBankName = request.getData().get("bankName");
        String depositAccountNumber = request.getData().get("accountNumber");
        String depositError = userService.createDepositRequest(depositUsername, depositAmount, depositBankName, depositAccountNumber);
        if (depositError == null) {
            return new Response("SUCCESS", "Deposit request created successfully");
        } else {
            return new Response("FAIL", depositError);
        }
    }
}
