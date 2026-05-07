package server.controller;

import java.util.List;
import java.util.Map;
import com.google.gson.Gson;
import shared.utils.GsonUtils;
import server.service.UserService;
import shared.models.User;
import shared.network.Request;
import shared.network.Response;

public class GetPendingWithdrawRequestsHandler implements RequestHandler {
    private final UserService userService;
    private final Gson gson = GsonUtils.createGson();

    public GetPendingWithdrawRequestsHandler(UserService userService) {
        this.userService = userService;
    }

    @Override
    public Response handle(Request request, ClientHandler clientHandler) {
        User u = userService.getUser(clientHandler.getLoggedInUsername());
        if (u == null || u.getRole() != shared.enums.Role.ADMIN) return new Response("FAIL", "Unauthorized");
        List<Map<String, String>> withdrawRequests = userService.getPendingWithdrawRequests();
        return new Response("SUCCESS", gson.toJson(withdrawRequests));
    }
}
