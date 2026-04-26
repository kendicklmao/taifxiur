package client;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import shared.network.Request;
import shared.network.Response;
import shared.utils.GsonUtils;
import shared.utils.Validator;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ForgotPasswordController {
    @FXML
    private TextField usernameField;
    @FXML
    private TextField emailField;
    @FXML
    private Button loadQuestionsButton;
    @FXML
    private Button switchAccountButton;
    @FXML
    private Button resetPasswordButton;
    @FXML
    private VBox verificationBox;
    @FXML
    private Label question1Label;
    @FXML
    private Label question2Label;
    @FXML
    private TextField answer1Field;
    @FXML
    private TextField answer2Field;
    @FXML
    private PasswordField newPasswordField;
    @FXML
    private PasswordField confirmPasswordField;

    private final AppContext ctx = AppContext.getInstance();
    private final Gson gson = GsonUtils.createGson();
    private String loadedUsername;
    private String loadedEmail;

    @FXML
    public void initialize() {
        verificationBox.setVisible(false);
        verificationBox.setManaged(false);
        resetPasswordButton.setVisible(false);
        resetPasswordButton.setManaged(false);
        switchAccountButton.setVisible(false);
        switchAccountButton.setManaged(false);
    }

    @FXML
    public void handleLoadQuestions() {
        clearValidation();

        String username = usernameField.getText();
        String email = emailField.getText();
        boolean valid = true;

        if (!Validator.isValidUsername(username)) {
            markInvalid(usernameField);
            valid = false;
        }
        if (!Validator.isValidEmail(email)) {
            markInvalid(emailField);
            valid = false;
        }
        if (!valid) {
            showAlert(Alert.AlertType.ERROR, "Invalid information", "Please enter a valid username and email.");
            return;
        }

        try {
            ensureConnected();

            Map<String, String> data = new HashMap<>();
            data.put("username", username);
            data.put("email", email);

            Response response = ctx.sendRequestAndWait(new Request("FORGOT_PASSWORD_INIT", data), 10);
            if (!"SUCCESS".equals(response.getStatus())) {
                showAlert(Alert.AlertType.ERROR, "Lookup failed", response.getMessage());
                return;
            }

            Type mapType = new TypeToken<Map<String, String>>() {}.getType();
            Map<String, String> questions = gson.fromJson(response.getMessage(), mapType);

            question1Label.setText(questions.getOrDefault("q1", ""));
            question2Label.setText(questions.getOrDefault("q2", ""));
            loadedUsername = Validator.normalizeAndLowercase(username);
            loadedEmail = Validator.normalizeAndLowercase(email);

            verificationBox.setVisible(true);
            verificationBox.setManaged(true);
            resetPasswordButton.setVisible(true);
            resetPasswordButton.setManaged(true);
            switchAccountButton.setVisible(true);
            switchAccountButton.setManaged(true);

            usernameField.setDisable(true);
            emailField.setDisable(true);
            loadQuestionsButton.setDisable(true);
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Connection error", "Cannot connect to server.");
            e.printStackTrace();
        }
    }

    @FXML
    public void handleResetPassword() {
        clearValidation();

        if (loadedUsername == null || loadedEmail == null) {
            showAlert(Alert.AlertType.ERROR, "Missing verification", "Please load your security questions first.");
            return;
        }

        String answer1 = answer1Field.getText();
        String answer2 = answer2Field.getText();
        String newPassword = Validator.normalize(newPasswordField.getText());
        String confirmPassword = Validator.normalize(confirmPasswordField.getText());

        boolean valid = true;
        List<String> errors = new ArrayList<>();
        if (!hasAnswer(answer1)) {
            markInvalid(answer1Field);
            valid = false;
            errors.add("Security answer 1 cannot be empty.");
        }
        if (!hasAnswer(answer2)) {
            markInvalid(answer2Field);
            valid = false;
            errors.add("Security answer 2 cannot be empty.");
        }
        if (!Validator.isValidPassword(newPassword)) {
            markInvalid(newPasswordField);
            valid = false;
            errors.add("New password must be at least 6 characters and include uppercase, lowercase, number, and special character.");
        }
        if (confirmPassword == null || !confirmPassword.equals(newPassword)) {
            markInvalid(confirmPasswordField);
            valid = false;
            errors.add("Confirm password does not match.");
        }

        if (!valid) {
            showAlert(
                    Alert.AlertType.ERROR,
                    "Invalid information",
                    String.join("\n", errors)
            );
            return;
        }

        try {
            ensureConnected();

            Map<String, String> data = new HashMap<>();
            data.put("username", loadedUsername);
            data.put("email", loadedEmail);
            data.put("a1", answer1);
            data.put("a2", answer2);
            data.put("newPassword", newPassword);

            Response response = ctx.sendRequestAndWait(new Request("RESET_PASSWORD", data), 10);
            if ("SUCCESS".equals(response.getStatus())) {
                showAlert(Alert.AlertType.INFORMATION, "Success", "Password reset successfully. Please sign in again.");
                Navigator.switchScene("login.fxml");
            } else {
                showAlert(Alert.AlertType.ERROR, "Reset failed", response.getMessage());
            }
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Connection error", "Cannot connect to server.");
            e.printStackTrace();
        }
    }

    @FXML
    public void handleUseAnotherAccount() {
        loadedUsername = null;
        loadedEmail = null;

        usernameField.clear();
        emailField.clear();
        answer1Field.clear();
        answer2Field.clear();
        newPasswordField.clear();
        confirmPasswordField.clear();
        question1Label.setText("");
        question2Label.setText("");

        usernameField.setDisable(false);
        emailField.setDisable(false);
        loadQuestionsButton.setDisable(false);

        verificationBox.setVisible(false);
        verificationBox.setManaged(false);
        resetPasswordButton.setVisible(false);
        resetPasswordButton.setManaged(false);
        switchAccountButton.setVisible(false);
        switchAccountButton.setManaged(false);
        clearValidation();
    }

    @FXML
    public void goBack() {
        Navigator.switchScene("login.fxml");
    }

    private void ensureConnected() throws Exception {
        if (!ctx.isConnected()) {
            ctx.connect();
        }
    }

    private void markInvalid(Control control) {
        if (!control.getStyleClass().contains("error-field")) {
            control.getStyleClass().add("error-field");
        }
    }

    private void clearValidation() {
        clearInvalid(usernameField);
        clearInvalid(emailField);
        clearInvalid(answer1Field);
        clearInvalid(answer2Field);
        clearInvalid(newPasswordField);
        clearInvalid(confirmPasswordField);
    }

    private void clearInvalid(Control control) {
        control.getStyleClass().remove("error-field");
    }

    private boolean hasAnswer(String answer) {
        return answer != null && !answer.trim().isEmpty();
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
