package server.controller;

import server.service.UserService;
import shared.network.Request;
import shared.network.Response;

public class CheckUsernameHandler implements RequestHandler {
    private final UserService userService;

    public CheckUsernameHandler(UserService userService) {
        this.userService = userService;
    }

    @Override
    public Response handle(Request request, ClientHandler clientHandler) {
        String username = request.getData().get("username");
        if (userService.exists(username)) {
            return new Response("EXISTS", "Username is already taken");
        } else {
            return new Response("OK", "Username is available");
        }
    }
}
