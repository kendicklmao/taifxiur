package server.controller;

import server.service.UserService;
import shared.network.Request;
import shared.network.Response;

public class ResetPasswordHandler implements RequestHandler {
    private final UserService userService;

    public ResetPasswordHandler(UserService userService) {
        this.userService = userService;
    }

    @Override
    public Response handle(Request request, ClientHandler clientHandler) {
        String rpUser = request.getData().get("username");
        String rpEmail = request.getData().get("email");
        String rpAnswer1 = request.getData().get("a1");
        String rpAnswer2 = request.getData().get("a2");
        String rpNewPassword = request.getData().get("newPassword");

        boolean resetSuccess = userService.resetPassword(rpUser, rpEmail, rpAnswer1, rpAnswer2, rpNewPassword);
        if (resetSuccess) {
            return new Response("SUCCESS", "Password reset successfully");
        } else {
            return new Response("FAIL", "Security answers are incorrect or the new password is invalid");
        }
    }
}
