package server.controller;

import server.service.UserService;
import shared.network.Request;
import shared.network.Response;

public class ApproveDepositRequestHandler implements RequestHandler {
    private final UserService userService;

    public ApproveDepositRequestHandler(UserService userService) {
        this.userService = userService;
    }

    @Override
    public Response handle(Request request, ClientHandler clientHandler) {
        String approveDepositId = request.getData().get("requestId");
        String approveDepositAdmin = clientHandler.getLoggedInUsername();
        String approveDepositResult = userService.approveDepositRequest(approveDepositId, approveDepositAdmin);
        if (approveDepositResult == null) {
            return new Response("SUCCESS", "Deposit request approved successfully");
        } else {
            return new Response("FAIL", approveDepositResult);
        }
    }
}
