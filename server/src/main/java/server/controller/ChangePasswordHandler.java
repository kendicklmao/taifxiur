package server.controller;

import server.service.UserService;
import shared.network.Request;
import shared.network.Response;

public class ChangePasswordHandler implements RequestHandler {
    private final UserService userService;

    public ChangePasswordHandler(UserService userService) {
        this.userService = userService;
    }

    @Override
    public Response handle(Request request, ClientHandler clientHandler) {
        String cpUser = request.getData().get("username");
        String cpOldPassword = request.getData().get("oldPassword");
        String cpNewPassword = request.getData().get("newPassword");

        try {
            boolean success = userService.changePassword(cpUser, cpOldPassword, cpNewPassword);
            if (success) {
                return new Response("SUCCESS", "Password changed successfully");
            } else {
                return new Response("FAIL", "Current password is incorrect or the new password is invalid");
            }

        } catch (IllegalArgumentException | IllegalStateException e) {
            return new Response("FAIL", e.getMessage());
        } catch (Exception e) {
            return new Response("FAIL", "An error occurred: " + e.getMessage());
        }
    }
}