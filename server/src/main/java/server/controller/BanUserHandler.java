package server.controller;

import server.service.UserService;
import shared.network.Request;
import shared.network.Response;

public class BanUserHandler implements RequestHandler {
    private final UserService userService;

    public BanUserHandler(UserService userService) {
        this.userService = userService;
    }

    @Override
    public Response handle(Request request, ClientHandler clientHandler) {
        String banUsername = request.getData().get("username");
        String banError = userService.banUser(banUsername, clientHandler.getLoggedInUsername());
        if (banError == null) {
            return new Response("SUCCESS", "User banned successfully");
        } else {
            return new Response("FAIL", banError);
        }
    }
}
