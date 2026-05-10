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
    private final IAlertService alertService = new AlertServiceImpl();

    @FXML
    public void initialize() {
        roleBox.getItems().addAll(Role.BIDDER, Role.SELLER);
        roleBox.setValue(Role.BIDDER);

        // Đảm bảo đã kết nối tới Server để thực hiện kiểm tra thời gian thực
        try {
            if (!ctx.isConnected()) {
                ctx.connect();
            }
        } catch (Exception e) {
            System.err.println("Error establishing connection on register view load: " + e.getMessage());
        }

        usernameField.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal) {
                String text = usernameField.getText();
                String username = text == null ? "" : text.trim();
                if (username.isEmpty()) {
                    usernameError.setText("Username cannot be empty");
                    addErrorStyle(usernameField);
                } else if (!Validator.isValidUsername(username)) {
                    usernameError.setText("Username must be 3+ characters, contain only letters, numbers, and underscores (no spaces)");
                    addErrorStyle(usernameField);
                } else {
                    java.util.Map<String, String> data = new java.util.HashMap<>();
                    data.put("username", username);
                    new Thread(() -> {
                        try {
                            if (!ctx.isConnected()) {
                                ctx.connect();
                            }
                            shared.network.Request req = new shared.network.Request("CHECK_USERNAME", data);
                            shared.network.Response res = ctx.sendRequestAndWait(req, 5);
                            javafx.application.Platform.runLater(() -> {
                                if ("EXISTS".equals(res.getStatus())) {
                                    usernameError.setText("Username is already taken");
                                    addErrorStyle(usernameField);
                                } else {
                                    usernameError.setText("");
                                    clearErrorStyle(usernameField);
                                }
                            });
                        } catch (Exception e) {
                            System.err.println("Error checking username existence: " + e.getMessage());
                        }
                    }).start();
                }
            }
        });

        passwordField.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal) {
                String text = passwordField.getText();
                String password = text == null ? "" : text;
                if (password.isEmpty()) {
                    passwordError.setText("Password cannot be empty");
                    addErrorStyle(passwordField);
                } else if (!Validator.isValidPassword(password)) {
                    passwordError.setText("Password must contain lowercase, uppercase, special characters, and numbers with length > 6");
                    addErrorStyle(passwordField);
                } else {
                    passwordError.setText("");
                    clearErrorStyle(passwordField);
                }
            }
        });

        confirmPasswordField.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal) {
                String pText = passwordField.getText();
                String password = pText == null ? "" : pText;
                String cpText = confirmPasswordField.getText();
                String confirmPassword = cpText == null ? "" : cpText;
                if (confirmPassword.isEmpty()) {
                    confirmPasswordError.setText("Confirm password cannot be empty");
                    addErrorStyle(confirmPasswordField);
                } else if (!password.equals(confirmPassword)) {
                    confirmPasswordError.setText("Passwords do not match");
                    addErrorStyle(confirmPasswordField);
                } else {
                    confirmPasswordError.setText("");
                    clearErrorStyle(confirmPasswordField);
                }
            }
        });

        emailField.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal) {
                String text = emailField.getText();
                String email = text == null ? "" : text.trim();
                if (email.isEmpty()) {
                    emailError.setText("Email cannot be empty");
                    addErrorStyle(emailField);
                } else if (!Validator.isValidEmail(email)) {
                    emailError.setText("Invalid email format");
                    addErrorStyle(emailField);
                } else {
                    java.util.Map<String, String> data = new java.util.HashMap<>();
                    data.put("email", email);
                    new Thread(() -> {
                        try {
                            if (!ctx.isConnected()) {
                                ctx.connect();
                            }
                            shared.network.Request req = new shared.network.Request("CHECK_EMAIL", data);
                            shared.network.Response res = ctx.sendRequestAndWait(req, 5);
                            javafx.application.Platform.runLater(() -> {
                                if ("EXISTS".equals(res.getStatus())) {
                                    emailError.setText("Email is already registered");
                                    addErrorStyle(emailField);
                                } else {
                                    emailError.setText("");
                                    clearErrorStyle(emailField);
                                }
                            });
                        } catch (Exception e) {
                            System.err.println("Error checking email existence: " + e.getMessage());
                        }
                    }).start();
                }
            }
        });

        q1Field.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal) {
                String text = q1Field.getText();
                String q1 = text == null ? "" : text.trim();
                if (q1.isEmpty()) {
                    q1Error.setText("Security question cannot be empty");
                    addErrorStyle(q1Field);
                } else {
                    q1Error.setText("");
                    clearErrorStyle(q1Field);
                }
            }
        });

        q2Field.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal) {
                String text = q2Field.getText();
                String q2 = text == null ? "" : text.trim();
                if (q2.isEmpty()) {
                    q2Error.setText("Security question cannot be empty");
                    addErrorStyle(q2Field);
                } else {
                    q2Error.setText("");
                    clearErrorStyle(q2Field);
                }
            }
        });

        a1Field.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal) {
                String text = a1Field.getText();
                String a1 = text == null ? "" : text.trim();
                if (a1.isEmpty()) {
                    a1Error.setText("Security answer cannot be empty");
                    addErrorStyle(a1Field);
                } else {
                    a1Error.setText("");
                    clearErrorStyle(a1Field);
                }
            }
        });

        a2Field.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal) {
                String text = a2Field.getText();
                String a2 = text == null ? "" : text.trim();
                if (a2.isEmpty()) {
                    a2Error.setText("Security answer cannot be empty");
                    addErrorStyle(a2Field);
                } else {
                    a2Error.setText("");
                    clearErrorStyle(a2Field);
                }
            }
        });
    }

    @FXML
    public void goBack() {
        Navigator.switchSceneStatic("login.fxml");
    }

    private void showAlert(String title, String msg) {
        alertService.showAlert(title, msg, usernameField);
    }

    private boolean validateInput() {
        boolean isValid = true;
        usernameError.setText("");
        passwordError.setText("");
        confirmPasswordError.setText("");
        emailError.setText("");

        if (!Validator.isValidUsername(usernameField.getText())) {
            usernameError.setText("Username must be 3+ characters, contain only letters, numbers, and underscores (no spaces)");
            addErrorStyle(usernameField);
            isValid = false;
        } else {
            clearErrorStyle(usernameField);
        }

        if (!Validator.isValidPassword(passwordField.getText())) {
            passwordError.setText("Password must be >= 6 characters with uppercase, lowercase, numbers and special characters");
            addErrorStyle(passwordField);
            isValid = false;
        } else {
            clearErrorStyle(passwordField);
        }

        if (!passwordField.getText().equals(confirmPasswordField.getText())) {
            confirmPasswordError.setText("Passwords do not match");
            addErrorStyle(confirmPasswordField);
            isValid = false;
        } else {
            clearErrorStyle(confirmPasswordField);
        }

        if (!Validator.isValidEmail(emailField.getText())) {
            emailError.setText("Invalid email format (example: user@example.com)");
            addErrorStyle(emailField);
            isValid = false;
        } else {
            clearErrorStyle(emailField);
        }

        return isValid;
    }

    @FXML
    public void handleRegister() {
        formError.setText("");
        boolean valid = true;

        clearErrorStyle(usernameField);
        clearErrorStyle(passwordField);
        clearErrorStyle(confirmPasswordField);
        clearErrorStyle(emailField);
        clearErrorStyle(q1Field);
        clearErrorStyle(a1Field);
        clearErrorStyle(q2Field);
        clearErrorStyle(a2Field);

        usernameError.setText("");
        passwordError.setText("");
        confirmPasswordError.setText("");
        emailError.setText("");
        q1Error.setText("");
        a1Error.setText("");
        q2Error.setText("");
        a2Error.setText("");

        if (usernameField.getText() == null || usernameField.getText().trim().isEmpty()) {
            usernameError.setText("Username cannot be empty");
            addErrorStyle(usernameField);
            valid = false;
        }

        if (passwordField.getText() == null || passwordField.getText().isEmpty()) {
            passwordError.setText("Password cannot be empty");
            addErrorStyle(passwordField);
            valid = false;
        }

        if (confirmPasswordField.getText() == null || confirmPasswordField.getText().isEmpty()) {
            confirmPasswordError.setText("Confirm password cannot be empty");
            addErrorStyle(confirmPasswordField);
            valid = false;
        }

        if (emailField.getText() == null || emailField.getText().trim().isEmpty()) {
            emailError.setText("Email cannot be empty");
            addErrorStyle(emailField);
            valid = false;
        }

        if (q1Field.getText() == null || q1Field.getText().trim().isEmpty()) {
            q1Error.setText("Security question cannot be empty");
            addErrorStyle(q1Field);
            valid = false;
        }

        if (a1Field.getText() == null || a1Field.getText().trim().isEmpty()) {
            a1Error.setText("Security answer cannot be empty");
            addErrorStyle(a1Field);
            valid = false;
        }

        if (q2Field.getText() == null || q2Field.getText().trim().isEmpty()) {
            q2Error.setText("Security question cannot be empty");
            addErrorStyle(q2Field);
            valid = false;
        }

        if (a2Field.getText() == null || a2Field.getText().trim().isEmpty()) {
            a2Error.setText("Security answer cannot be empty");
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
                Navigator.switchSceneStatic("login.fxml");
            } else {
                String errorMsg = res.getMessage();
                if ("USERNAME_EXISTS".equals(errorMsg)) {
                    usernameError.setText("Username already exists");
                    addErrorStyle(usernameField);
                } else if ("EMAIL_EXISTS".equals(errorMsg)) {
                    emailError.setText("Email already exists");
                    addErrorStyle(emailField);
                } else {
                    showAlert("Error", errorMsg);
                }
            }

        } catch (Exception e) {
            showAlert("Error", "Error connecting to server");
            e.printStackTrace();
        }
    }

    private void addErrorStyle(javafx.scene.control.TextField field) {
        if (!field.getStyleClass().contains("error-field")) {
            field.getStyleClass().add("error-field");
        }
    }

    private void clearErrorStyle(javafx.scene.control.TextField field) {
        field.getStyleClass().removeAll(java.util.Collections.singleton("error-field"));
    }
}