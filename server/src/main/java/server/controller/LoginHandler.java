package server.controller;

import server.service.UserAlreadyLoggedInException;
import server.service.UserLockedException;
import server.service.UserService;
import shared.models.User;
import shared.network.Request;
import shared.network.Response;

public class LoginHandler implements RequestHandler {
    
    private final UserService userService;

    public LoginHandler(UserService userService) {
        this.userService = userService;
    }

    @Override
    public Response handle(Request request, ClientHandler clientHandler) {
        String user = request.getData().get("username");
        String pass = request.getData().get("password");

        try {
            if (userService.exists(user) && userService.isBanned(user)) {
                return new Response("FAIL", "Your account has been banned");
            }

            User loggedInUser = userService.login(user, pass);
            if (loggedInUser != null) {
                clientHandler.setLoggedInUsername(loggedInUser.getUsername());
                return new Response("SUCCESS", loggedInUser.getRole().toString() + "," + loggedInUser.getUsername());
            } else {
                return new Response("FAIL", "Invalid username or password");
            }

        } catch (UserAlreadyLoggedInException e) {
            return new Response("FAIL", e.getMessage());
        } catch (UserLockedException e) {
            return new Response("ACCOUNT_DISABLED", String.valueOf(e.getSecondsRemaining()));
        }
    }
}