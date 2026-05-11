package client;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import shared.network.Request;
import shared.network.Response;
import shared.utils.FormatUtils;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public abstract class BaseHomeController extends UserController {
    
    @FXML
    protected Label welcomeLabel;
    
    protected Consumer<String> messageListener;

    protected void setupHome() {
        if (welcomeLabel != null && ctx.getCurrentUser() != null) {
            welcomeLabel.setText("Welcome " + ctx.getCurrentUser().getUsername());
        }
        
        messageListener = line -> {
            try {
                Response res = gson.fromJson(line, Response.class);
                onSocketMessage(res);
            } catch (Exception e) {
            }
        };
        ctx.addMessageListener(messageListener);
    }

    @FXML
    public void handleLogout() {
        try {
            Map<String, String> data = new HashMap<>();
            data.put("username", ctx.getCurrentUser().getUsername());
            Request req = new Request("LOGOUT", data);
            ctx.sendRequestAndWait(req, 30);
        } catch (Exception e) {
        }

        if (messageListener != null) {
            ctx.removeMessageListener(messageListener);
        }
        ctx.setCurrentUser(null);
        Navigator.switchSceneStatic("login.fxml");
    }

    @FXML
    public void handleChangePassword() {
        ChangePasswordSupport.showDialog(ctx, welcomeLabel);
    }

    protected String formatTime(Instant instant) {
        return FormatUtils.formatTime(instant);
    }

    protected void onSocketMessage(Response response) {
    }
}
