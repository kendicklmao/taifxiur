package server.controller;

import server.service.UserService;
import shared.network.Request;
import shared.network.Response;

public class ApproveWithdrawRequestHandler implements RequestHandler {
    private final UserService userService;

    public ApproveWithdrawRequestHandler(UserService userService) {
        this.userService = userService;
    }

    @Override
    public Response handle(Request request, ClientHandler clientHandler) {
        String approveWithdrawId = request.getData().get("requestId");
        String approveWithdrawAdmin = clientHandler.getLoggedInUsername();
        String approveWithdrawResult = userService.approveWithdrawRequest(approveWithdrawId, approveWithdrawAdmin);
        if (approveWithdrawResult == null) {
            return new Response("SUCCESS", "Withdraw request approved successfully");
        } else {
            return new Response("FAIL", approveWithdrawResult);
        }
    }
}
