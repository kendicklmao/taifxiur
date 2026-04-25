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
    private PasswordField confirmPasswordField;
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
    private Label confirmPasswordError;
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

        usernameField.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal) {
                String username = usernameField.getText().trim();
                if (!username.isEmpty() && !Validator.isValidUsername(username)) {
                    usernameError.setText("Username must be 6+ characters, contain only letters, numbers, and underscores (no spaces)");
                    usernameField.getStyleClass().add("error-field");
                } else {
                    usernameError.setText("");
                    usernameField.getStyleClass().remove("error-field");
                }
            /*if (ctx.getUserService().exists(username)) {
                usernameError.setText("Username đã tồn tại");
                usernameField.setStyle("-fx-border-color: red;");
            }*/
            }
        });
        passwordField.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal) {
                String password = passwordField.getText();
                if (!password.isEmpty() && !Validator.isValidPassword(password)) {
                    passwordError.setText("Password must contain lowercase, uppercase, special characters, and numbers with length > 6");
                    passwordField.getStyleClass().add("error-field");
                } else {
                    passwordError.setText("");
                    passwordField.getStyleClass().remove("error-field");
                }
            }
        });
        confirmPasswordField.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal) {
                String password = passwordField.getText();
                String confirmPassword = confirmPasswordField.getText();
                if (!confirmPassword.isEmpty() && !password.equals(confirmPassword)) {
                    confirmPasswordError.setText("Passwords do not match");
                    confirmPasswordField.getStyleClass().add("error-field");
                } else {
                    confirmPasswordError.setText("");
                    confirmPasswordField.getStyleClass().remove("error-field");
                }
            }
        });
        emailField.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal) {
                if (!emailField.getText().isEmpty() && !Validator.isValidEmail(emailField.getText())) {
                    emailError.setText("Invalid email format");
                    emailField.getStyleClass().add("error-field");
                } else {
                    emailError.setText("");
                    emailField.getStyleClass().remove("error-field");
                }
            }
        });
        q1Field.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal) {
                if (q1Field.getText().isEmpty()) {
                    q1Error.setText("Security question cannot be empty");
                    q1Field.getStyleClass().add("error-field");
                } else {
                    q1Error.setText("");
                    q1Field.getStyleClass().remove("error-field");
                }
            }
        });
        q2Field.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal) {
                if (q2Field.getText().isEmpty()) {
                    q2Error.setText("Security question cannot be empty");
                    q2Field.getStyleClass().add("error-field");
                } else {
                    q2Error.setText("");
                    q2Field.getStyleClass().remove("error-field");
                }
            }
        });
        a1Field.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal) {
                if (a1Field.getText().isEmpty()) {
                    a1Error.setText("Security answer cannot be empty");
                    a1Field.getStyleClass().add("error-field");
                } else {
                    a1Error.setText("");
                    a1Field.getStyleClass().remove("error-field");
                }
            }
        });
        a2Field.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal) {
                if (a2Field.getText().isEmpty()) {
                    a2Error.setText("Security answer cannot be empty");
                    a2Field.getStyleClass().add("error-field");
                } else {
                    a2Error.setText("");
                    a2Field.getStyleClass().remove("error-field");
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
        confirmPasswordError.setText("");
        emailError.setText("");

        if (!Validator.isValidUsername(usernameField.getText())) {
            usernameError.setText("Username must be 6+ characters, contain only letters, numbers, and underscores (no spaces)");
            isValid = false;
        }

        if (!Validator.isValidPassword(passwordField.getText())) {
            passwordError.setText("Password must be >= 6 characters with uppercase, lowercase, numbers and special characters");
            isValid = false;
        }

        if (!passwordField.getText().equals(confirmPasswordField.getText())) {
            confirmPasswordError.setText("Passwords do not match");
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

        // Clear all error styles
        clearErrorStyle(usernameField);
        clearErrorStyle(passwordField);
        clearErrorStyle(confirmPasswordField);
        clearErrorStyle(emailField);
        clearErrorStyle(q1Field);
        clearErrorStyle(a1Field);
        clearErrorStyle(q2Field);
        clearErrorStyle(a2Field);

        if (usernameField.getText() == null || usernameField.getText().trim().isEmpty()) {
            addErrorStyle(usernameField);
            valid = false;
        }
        if (passwordField.getText() == null || passwordField.getText().trim().isEmpty()) {
            addErrorStyle(passwordField);
            valid = false;
        }
        if (confirmPasswordField.getText() == null || confirmPasswordField.getText().trim().isEmpty()) {
            addErrorStyle(confirmPasswordField);
            valid = false;
        }
        if (emailField.getText() == null || emailField.getText().trim().isEmpty()) {
            addErrorStyle(emailField);
            valid = false;
        }
        if (q1Field.getText() == null || q1Field.getText().trim().isEmpty()) {
            addErrorStyle(q1Field);
            valid = false;
        }
        if (a1Field.getText() == null || a1Field.getText().trim().isEmpty()) {
            addErrorStyle(a1Field);
            valid = false;
        }
        if (q2Field.getText() == null || q2Field.getText().trim().isEmpty()) {
            addErrorStyle(q2Field);
            valid = false;
        }
        if (a2Field.getText() == null || a2Field.getText().trim().isEmpty()) {
            addErrorStyle(a2Field);
            valid = false;
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

    private void addErrorStyle(javafx.scene.control.TextField field) {
        field.getStyleClass().add("error-field");
    }

    private void clearErrorStyle(javafx.scene.control.TextField field) {
        field.getStyleClass().remove("error-field");
    }
}