package server.controller;

import server.service.UserService;
import shared.network.Request;
import shared.network.Response;
import shared.enums.Role;

public class RegisterHandler implements RequestHandler {
    private final UserService userService;

    public RegisterHandler(UserService userService) {
        this.userService = userService;
    }

    @Override
    public Response handle(Request request, ClientHandler clientHandler) {
        String rUser = request.getData().get("username");
        String rPass = request.getData().get("password");
        String rEmail = request.getData().get("email");
        String rQ1 = request.getData().get("q1");
        String rA1 = request.getData().get("a1");
        String rQ2 = request.getData().get("q2");
        String rA2 = request.getData().get("a2");

        Role rRole = Role.valueOf(request.getData().get("role"));
        
        if (userService.exists(rUser)) {
            return new Response("FAIL", "USERNAME_EXISTS");
        }
        
        if (userService.emailExists(rEmail)) {
            return new Response("FAIL", "EMAIL_EXISTS");
        }

        if (userService.register(rUser, rPass, rEmail, rQ1, rA1, rQ2, rA2, rRole)) {
            return new Response("SUCCESS", "Registration successful!");
        } else {
            return new Response("FAIL", "Registration failed!");
        }
    }
}