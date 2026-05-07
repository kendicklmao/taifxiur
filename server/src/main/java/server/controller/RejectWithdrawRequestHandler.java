package server.controller;

import server.service.UserService;
import shared.network.Request;
import shared.network.Response;

public class RejectWithdrawRequestHandler implements RequestHandler {
    private final UserService userService;

    public RejectWithdrawRequestHandler(UserService userService) {
        this.userService = userService;
    }

    @Override
    public Response handle(Request request, ClientHandler clientHandler) {
        String rejectWithdrawId = request.getData().get("requestId");
        String rejectWithdrawAdmin = clientHandler.getLoggedInUsername();
        String rejectWithdrawResult = userService.rejectWithdrawRequest(rejectWithdrawId, rejectWithdrawAdmin);
        if (rejectWithdrawResult == null) {
            return new Response("SUCCESS", "Withdraw request rejected successfully");
        } else {
            return new Response("FAIL", rejectWithdrawResult);
        }
    }
}
