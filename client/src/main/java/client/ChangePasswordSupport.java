package client;

import javafx.event.ActionEvent;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.layout.VBox;
import shared.network.Request;
import shared.network.Response;
import shared.utils.Validator;

import java.util.HashMap;
import java.util.Map;

public final class ChangePasswordSupport {
    private ChangePasswordSupport() {
    }

    public static void showDialog(AppContext ctx) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Change Password");
        dialog.setHeaderText("Update your account password");

        PasswordField currentPasswordField = new PasswordField();
        currentPasswordField.setPromptText("Current password");

        PasswordField newPasswordField = new PasswordField();
        newPasswordField.setPromptText("New password");

        PasswordField confirmPasswordField = new PasswordField();
        confirmPasswordField.setPromptText("Confirm new password");

        Label errorLabel = new Label();
        errorLabel.getStyleClass().add("error-label");
        errorLabel.setWrapText(true);

        VBox content = new VBox(10);
        content.getChildren().addAll(
                new Label("Current password"),
                currentPasswordField,
                new Label("New password"),
                newPasswordField,
                new Label("Confirm new password"),
                confirmPasswordField,
                errorLabel
        );

        ButtonType changeType = new ButtonType("Change Password", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(changeType, ButtonType.CANCEL);
        dialog.getDialogPane().setContent(content);

        Node changeButton = dialog.getDialogPane().lookupButton(changeType);
        changeButton.addEventFilter(ActionEvent.ACTION, event -> {
            String validationError = validate(
                    currentPasswordField.getText(),
                    newPasswordField.getText(),
                    confirmPasswordField.getText()
            );

            if (validationError != null) {
                errorLabel.setText(validationError);
                event.consume();
                return;
            }

            try {
                if (!ctx.isConnected()) {
                    ctx.connect();
                }

                Map<String, String> data = new HashMap<>();
                data.put("username", ctx.getCurrentUser().getUsername());
                data.put("oldPassword", currentPasswordField.getText());
                data.put("newPassword", newPasswordField.getText());

                Response response = ctx.sendRequestAndWait(new Request("CHANGE_PASSWORD", data), 10);
                if ("SUCCESS".equals(response.getStatus())) {
                    dialog.close();
                    showAlert("Success", response.getMessage());
                } else {
                    errorLabel.setText(response.getMessage());
                    event.consume();
                }
            } catch (Exception e) {
                errorLabel.setText("Cannot connect to server.");
                event.consume();
            }
        });

        dialog.showAndWait();
    }

    private static String validate(String currentPassword, String newPassword, String confirmPassword) {
        if (currentPassword == null || currentPassword.trim().isEmpty()) {
            return "Current password cannot be empty.";
        }

        String normalizedNewPassword = Validator.normalizePassword(newPassword);
        String normalizedConfirmPassword = Validator.normalizePassword(confirmPassword);

        if (!Validator.isValidPassword(normalizedNewPassword)) {
            return "New password must be at least 6 characters and include uppercase, lowercase, number, and special character.";
        }

        if (normalizedConfirmPassword == null || !normalizedConfirmPassword.equals(normalizedNewPassword)) {
            return "Confirm password does not match.";
        }

        if (Validator.normalizePassword(currentPassword).equals(normalizedNewPassword)) {
            return "New password must be different from the current password.";
        }

        return null;
    }

    private static void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
