package server.controller;

import server.service.UserService;
import shared.network.Request;
import shared.network.Response;

public class CheckEmailHandler implements RequestHandler {
    private final UserService userService;

    public CheckEmailHandler(UserService userService) {
        this.userService = userService;
    }

    @Override
    public Response handle(Request request, ClientHandler clientHandler) {
        String email = request.getData().get("email");
        if (userService.emailExists(email)) {
            return new Response("EXISTS", "Email is already registered");
        } else {
            return new Response("OK", "Email is available");
        }
    }
}
