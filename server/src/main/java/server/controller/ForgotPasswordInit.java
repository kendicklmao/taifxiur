package server.controller;

import java.util.HashMap;
import java.util.Map;

import com.google.gson.Gson;

import server.service.UserService;
import shared.network.Request;
import shared.network.Response;
import shared.utils.GsonUtils;

public class ForgotPasswordInit implements RequestHandler {

    private final UserService userService;
    private final Gson gson = GsonUtils.createGson();

    public ForgotPasswordInit(UserService userService) {
        this.userService = userService;
    }

    @Override
    public Response handle(Request request, ClientHandler clientHandler) {
        String fpUser = request.getData().get("username");
        String fpEmail = request.getData().get("email");
        String[] questions = userService.getSecurityQuestions(fpUser, fpEmail);

        if (questions != null) {
            Map<String, String> payload = new HashMap<>();
            payload.put("q1", questions[0]);
            payload.put("q2", questions[1]);
            return new Response("SUCCESS", gson.toJson(payload));
        } else {
            return new Response("FAIL", "Account information does not match");
        }
    }
}
