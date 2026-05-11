package client;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.util.Duration;
import shared.models.Admin;
import shared.models.Bidder;
import shared.models.Seller;
import shared.models.User;
import shared.network.Request;
import shared.network.Response;

import java.util.HashMap;
import java.util.Map;

public class LoginController extends UserController {
    @FXML TextField usernameField;
    @FXML PasswordField passwordField;
    @FXML Button loginButton;
    @FXML Label countdownLabel;

    private INavigator navigator;

    // Constructor mặc định cho FXML loading
    public LoginController() {
        this.navigator = Navigator.getInstance();
    }

    // Constructor cho testing (Dependency Injection)
    public LoginController(AppContext ctx, INavigator navigator, IAlertService alertService) {
        this.ctx = ctx;
        this.navigator = navigator;
        this.alertService = alertService;
    }

    @FXML
    public void handleLogin() {
        try {
            if (!ctx.isConnected()) {
                ctx.connect();
            }

            Map<String, String> data = new HashMap<>();
            data.put("username", usernameField.getText());
            data.put("password", passwordField.getText());

            Request req = new Request("LOGIN", data);
            Response res = ctx.sendRequestAndWait(req, 15);

            if ("SUCCESS".equals(res.getStatus())) {
                String[] info = res.getMessage().split(",");
                String role = info[0];
                String username = info[1];

                User currentUser;
                if (role.equals("BIDDER")) {
                    currentUser = new Bidder(username, "dummy", "dummy@mail.com", "q", "a", "q", "a");
                } else if (role.equals("SELLER")) {
                    currentUser = new Seller(username, "dummy", "dummy@mail.com", "q", "a", "q", "a");
                } else {
                    currentUser = new Admin(username, "dummy", "dummy@mail.com", "q", "a", "q", "a");
                }

                ctx.setCurrentUser(currentUser);

                alertService.showAlert("Success", "Login successful with role: " + role, usernameField);
                if (role.equals("BIDDER")) {
                    navigator.switchScene("bidder_home.fxml");
                } else if (role.equals("SELLER")) {
                    navigator.switchScene("seller_home.fxml");
                } else if (role.equals("ADMIN")) {
                    navigator.switchScene("admin_home.fxml");
                }

            } else if ("ACCOUNT_DISABLED".equals(res.getStatus())) {
                int seconds = Integer.parseInt(res.getMessage());
                alertService.showAlert("Account Disabled", "Account is temporarily locked. Please try again in " + seconds + " seconds.", usernameField);
                disableLoginFor(seconds);
            } else {
                alertService.showAlert("Error", res.getMessage(), usernameField);
            }

        } catch (Exception e) {
            alertService.showAlert("Error", "Cannot connect to Server!", usernameField);
            e.printStackTrace();
        }
    }

    private void disableLoginFor(int seconds) {
        setLoginControlsDisabled(true);
        Timeline timeline = new Timeline();
        timeline.setCycleCount(seconds);
        timeline.getKeyFrames().add(new KeyFrame(Duration.seconds(1), e -> {
            int remaining = timeline.getCycleCount() - (int)timeline.getCurrentTime().toSeconds();
            countdownLabel.setText("Please wait " + remaining + " seconds before trying again.");
        }));
        timeline.setOnFinished(e -> {
            setLoginControlsDisabled(false);
            countdownLabel.setText("");
        });
        timeline.play();
    }

    private void setLoginControlsDisabled(boolean disabled) {
        usernameField.setDisable(disabled);
        passwordField.setDisable(disabled);
        loginButton.setDisable(disabled);
    }

    @FXML
    public void goToRegister() {
        navigator.switchScene("register.fxml");
    }

    @FXML
    public void goToForgotPassword() {
        navigator.switchScene("forgot_password.fxml");
    }
}