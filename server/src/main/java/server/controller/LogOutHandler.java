package server.controller;

import server.service.UserService;
import shared.network.Request;
import shared.network.Response;

public class LogOutHandler implements RequestHandler {

    private final UserService userService;

    public LogOutHandler(UserService userService) {
        this.userService = userService;
    }

    @Override
    public Response handle(Request request, ClientHandler clientHandler) {
        String loggedInUsername = clientHandler.getLoggedInUsername();
        if (loggedInUsername != null) {
            userService.logout(loggedInUsername);
            clientHandler.setLoggedInUsername(null);
        }
        return new Response("SUCCESS", "Logged out");
    }
}