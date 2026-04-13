package client;

import java.io.BufferedReader;
import java.io.PrintWriter;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import shared.enums.Role;
import shared.utils.Validator;

public class RegisterController {
    @FXML
    private TextField usernameField;
    @FXML
    private PasswordField passwordField;
    @FXML
    private TextField emailField;
    @FXML
    private TextField q1Field;
    @FXML
    private TextField a1Field;
    @FXML
    private TextField q2Field;
    @FXML
    private TextField a2Field;
    @FXML
    private ChoiceBox<Role> roleBox;
    @FXML
    private Label usernameError;
    @FXML
    private Label passwordError;
    @FXML
    private Label emailError;
    @FXML
    private Label q1Error;
    @FXML
    private Label a1Error;
    @FXML
    private Label q2Error;
    @FXML
    private Label a2Error;
    @FXML
    private Label formError;
    private final AppContext ctx = AppContext.getInstance();

    @FXML
    public void initialize() {
        roleBox.getItems().addAll(Role.BIDDER, Role.SELLER);
        roleBox.setValue(Role.BIDDER);
        roleBox.setValue(Role.SELLER);
        usernameField.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal) {
                String username = usernameField.getText();
                if (!Validator.isValidUsername(username)) {
                    usernameError.setText("Tên đăng nhập chỉ chứa chữ cái, số và có ĐỘ dài lớn hơn 6 và nhỏ hơn 20");
                }
            /*if (ctx.getUserService().exists(username)) {
                usernameError.setText("Username đã tồn tại");
                usernameField.setStyle("-fx-border-color: red;");
            }*/
            } else {
                usernameError.setText("");
                usernameField.setStyle("");
            }
        });
        passwordField.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal) {
                String password = passwordField.getText();
                if (!Validator.isValidPassword(password)) {
                    passwordError.setText("Mật khẩu phải chứa chữ thường, chữ hoa, kí tự đặc biệt, số và có ĐỘ dài lớn hơn 6");
                } else {
                    passwordError.setText("");
                }
            }
        });
        emailField.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal) {
                if (!Validator.isValidEmail(emailField.getText())) {
                    emailError.setText("Email không hợp lệ");
                } else {
                    emailError.setText("");
                }
            }
        });
        q1Field.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal) {
                if (q1Field.getText().isEmpty()) {
                    q1Error.setText("Câu hỏi bảo mật không được để trống");
                }
            }
        });
        q2Field.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal) {
                if (q2Field.getText().isEmpty()) {
                    q2Error.setText("Câu hỏi bảo mật không được để trống");
                }
            }
        });
        a1Field.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal) {
                if (a1Field.getText().isEmpty()) {
                    a1Error.setText("Câu trả lời bảo mật không được để trống");
                }
            }
        });
        a2Field.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal) {
                if (a2Field.getText().isEmpty()) {
                    a2Error.setText("Câu trả lời bảo mật không được để trống");
                }
            }
        });

    }

    @FXML
    public void goBack() {
        Navigator.switchScene("login.fxml");
    }

    private void showAlert(String title, String msg) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setContentText(msg);
        alert.showAndWait();
    }

    private boolean validateInput() {
        boolean isValid = true;
        usernameError.setText("");
        passwordError.setText("");
        emailError.setText("");

        if (!Validator.isValidUsername(usernameField.getText())) {
            usernameError.setText("Tên đăng nhập phải từ 6-20 ký tự, không có dấu cách nhé thằng NGU.");
            isValid = false;
        }

        if (!Validator.isValidPassword(passwordField.getText())) {
            passwordError.setText("Pass phải >= 6 ký tự, có hoa, thường, số và ký tự đặc biệt nha đmm.");
            isValid = false;
        }
        if (!Validator.isValidEmail(emailField.getText())) {
            emailError.setText("Email sai định dạng (ví dụ đúng: ngulol@vnu.edu.vn)");
            isValid = false;
        }

        return isValid;
    }

    @FXML
    public void handleRegister() {
        formError.setText("");
        boolean valid = true;
        if (usernameField.getText() == null || usernameField.getText().trim().isEmpty()) {
            usernameField.setStyle("-fx-border-color: red;");
            valid = false;
        } else {
            usernameField.setStyle("");
        }
        if (passwordField.getText() == null || passwordField.getText().trim().isEmpty()) {
            passwordField.setStyle("-fx-border-color: red;");
            valid = false;
        } else {
            passwordField.setStyle("");
        }
        if (emailField.getText() == null || emailField.getText().trim().isEmpty()) {
            emailField.setStyle("-fx-border-color: red;");
            valid = false;
        } else {
            emailField.setStyle("");
        }
        if (q1Field.getText() == null || q1Field.getText().trim().isEmpty()) {
            q1Field.setStyle("-fx-border-color: red;");
            valid = false;
        } else {
            q1Field.setStyle("");
        }
        if (a1Field.getText() == null || a1Field.getText().trim().isEmpty()) {
            a1Field.setStyle("-fx-border-color: red;");
            valid = false;
        } else {
            a1Field.setStyle("");
        }
        if (q2Field.getText() == null || q2Field.getText().trim().isEmpty()) {
            q2Field.setStyle("-fx-border-color: red;");
            valid = false;
        } else {
            q2Field.setStyle("");
        }
        if (a2Field.getText() == null || a2Field.getText().trim().isEmpty()) {
            a2Field.setStyle("-fx-border-color: red;");
            valid = false;
        } else {
            a2Field.setStyle("");
        }
        if (!valid) {
            formError.setText("Please enter ALL information");
            return;
        }
        if (!validateInput()) {
            return;
        }
        try {
            PrintWriter out = ctx.getOut();
            BufferedReader in = ctx.getIn();

            java.util.Map<String, String> data = new java.util.HashMap<>();
            data.put("username", usernameField.getText());
            data.put("password", passwordField.getText());
            data.put("email", emailField.getText());
            data.put("q1", q1Field.getText());
            data.put("a1", a1Field.getText());
            data.put("q2", q2Field.getText());
            data.put("a2", a2Field.getText());
            data.put("role", roleBox.getValue().toString());

            shared.network.Request req = new shared.network.Request("REGISTER", data);
            com.google.gson.Gson gson = new com.google.gson.Gson();
            out.println(gson.toJson(req));

            String serverResponse = in.readLine();
            shared.network.Response res = gson.fromJson(serverResponse, shared.network.Response.class);

            if ("SUCCESS".equals(res.getStatus())) {
                showAlert("Success", "Registered successfully!");
                Navigator.switchScene("login.fxml");
            } else {
                showAlert("Error", res.getMessage());
            }
        } catch (Exception e) {
            showAlert("Error", "Error connecting to server");
            e.printStackTrace();
        }
    }
}