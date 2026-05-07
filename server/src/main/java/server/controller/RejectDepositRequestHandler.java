package server.controller;

import server.service.UserService;
import shared.network.Request;
import shared.network.Response;

public class RejectDepositRequestHandler implements RequestHandler {
    private final UserService userService;

    public RejectDepositRequestHandler(UserService userService) {
        this.userService = userService;
    }

    @Override
    public Response handle(Request request, ClientHandler clientHandler) {
        String rejectDepositId = request.getData().get("requestId");
        String rejectDepositAdmin = clientHandler.getLoggedInUsername();
        String rejectDepositResult = userService.rejectDepositRequest(rejectDepositId, rejectDepositAdmin);
        if (rejectDepositResult == null) {
            return new Response("SUCCESS", "Deposit request rejected successfully");
        } else {
            return new Response("FAIL", rejectDepositResult);
        }
    }
}
