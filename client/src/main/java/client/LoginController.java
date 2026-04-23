package client;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import shared.models.Admin;
import shared.models.Bidder;
import shared.models.Seller;
import shared.models.User;
import shared.network.Request;
import shared.network.Response;

import java.util.HashMap;
import java.util.Map;

public class LoginController {
    @FXML TextField usernameField;
    @FXML PasswordField passwordField;
    private final AppContext ctx = AppContext.getInstance();

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
            Response res = ctx.sendRequestAndWait(req, 5);

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

                showAlert("Success", "Login successful with role: " + role);
                if (role.equals("BIDDER")) {
                    Navigator.switchScene("bidder_home.fxml");
                } else if (role.equals("SELLER")) {
                    Navigator.switchScene("seller_home.fxml");
                } else if (role.equals("ADMIN")) {
                    Navigator.switchScene("admin_home.fxml");
                }
            } else {
                showAlert("Error", res.getMessage());
            }

        } catch (Exception e) {
            showAlert("Error", "Cannot connect to Server!");
            e.printStackTrace();
        }
    }

    @FXML
    public void goToRegister() {
        Navigator.switchScene("register.fxml");
    }

    @FXML
    public void goToForgotPassword() {
        Navigator.switchScene("forgot_password.fxml");
    }

    private void showAlert(String title, String msg){
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setContentText(msg);
        alert.showAndWait();
    }
}
