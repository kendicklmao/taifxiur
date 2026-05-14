package server.controller;

import com.google.gson.Gson;
import server.service.UserService;
import shared.network.Request;
import shared.network.Response;

public class UnbanUserHandler implements RequestHandler {
    private final UserService userService;

    public UnbanUserHandler(UserService userService) {
        this.userService = userService;
    }

    @Override
    public Response handle(Request request, ClientHandler clientHandler) {
        String unbanUsername = request.getData().get("username");
        String unbanError = userService.unbanUser(unbanUsername, clientHandler.getLoggedInUsername());
        if (unbanError == null) {
            Response broadcastRes = new Response("USER_UNBANNED", "User " + unbanUsername + " has been unbanned");
            ClientHandler.broadcast(new Gson().toJson(broadcastRes));
            return new Response("SUCCESS", "User unbanned successfully");
        } else {
            return new Response("FAIL", unbanError);
        }
    }
}