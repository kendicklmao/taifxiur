package client;

import com.google.gson.Gson;
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

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

public class LoginController {
    @FXML TextField usernameField;
    @FXML PasswordField passwordField;
    private final AppContext ctx = AppContext.getInstance();

    @FXML
    public void handleLogin() {
        try {
            ctx.connect();

            PrintWriter out = ctx.getOut();
            BufferedReader in = ctx.getIn();

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

                showAlert("Thành công", "Đăng nhập thành công với vai trò: " + role);
                if (role.equals("BIDDER")) {
                    Navigator.switchScene("bidder_home.fxml");
                } else if (role.equals("SELLER")) {
                    Navigator.switchScene("seller_home.fxml");
                }
            } else {
                showAlert("Lỗi", "Sai tài khoản hoặc mật khẩu!");
            }

        } catch (Exception e) {
            showAlert("Lỗi", "Không thể kết nối tới Server!");
            e.printStackTrace();
        }
    }

    @FXML
    public void goToRegister() {
        Navigator.switchScene("register.fxml");
    }

    private void showAlert(String title, String msg){
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setContentText(msg);
        alert.showAndWait();
    }
}