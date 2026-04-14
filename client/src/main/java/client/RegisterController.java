package client;

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
                    usernameError.setText("Username must contain letters and numbers, length between 6 and 20");
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
                    passwordError.setText("Password must contain lowercase, uppercase, special characters, and numbers with length > 6");
                } else {
                    passwordError.setText("");
                }
            }
        });
        emailField.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal) {
                if (!Validator.isValidEmail(emailField.getText())) {
                    emailError.setText("Invalid email format");
                } else {
                    emailError.setText("");
                }
            }
        });
        q1Field.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal) {
                if (q1Field.getText().isEmpty()) {
                    q1Error.setText("Security question cannot be empty");
                }
            }
        });
        q2Field.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal) {
                if (q2Field.getText().isEmpty()) {
                    q2Error.setText("Security question cannot be empty");
                }
            }
        });
        a1Field.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal) {
                if (a1Field.getText().isEmpty()) {
                    a1Error.setText("Security answer cannot be empty");
                }
            }
        });
        a2Field.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal) {
                if (a2Field.getText().isEmpty()) {
                    a2Error.setText("Security answer cannot be empty");
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
            usernameError.setText("Username must be 6-20 characters, no spaces");
            isValid = false;
        }

        if (!Validator.isValidPassword(passwordField.getText())) {
            passwordError.setText("Password must be >= 6 characters with uppercase, lowercase, numbers and special characters");
            isValid = false;
        }
        if (!Validator.isValidEmail(emailField.getText())) {
            emailError.setText("Invalid email format (example: user@example.com)");
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
        if (roleBox.getValue() == null) {
            formError.setText("Please select a role");
            return;
        }
        if (!validateInput()) {
            return;
        }
        try {
            if (!ctx.isConnected()) {
                ctx.connect();
            }

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
            shared.network.Response res = ctx.sendRequestAndWait(req, 10);

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