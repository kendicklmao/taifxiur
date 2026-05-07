package server.controller;

import com.google.gson.Gson;
import shared.utils.GsonUtils;
import server.service.UserService;
import shared.network.Request;
import shared.network.Response;
import java.util.HashMap;
import java.util.Map;

public class ForgotPasswordInitHandler implements RequestHandler {
    private final UserService userService;
    private final Gson gson = GsonUtils.createGson();

    public ForgotPasswordInitHandler(UserService userService) {
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
