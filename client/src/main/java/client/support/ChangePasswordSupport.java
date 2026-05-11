package client.support;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.layout.VBox;
import javafx.stage.StageStyle;
import javafx.stage.Window;
import shared.network.Request;
import shared.network.Response;
import shared.utils.Validator;

import java.util.HashMap;
import java.util.Map;

import client.AppContext;

public final class ChangePasswordSupport {

    private ChangePasswordSupport() {
    }

    public static void showDialog(AppContext ctx, Node ownerNode) {
        Dialog<Void> dialog = new Dialog<>();

        if (ownerNode != null && ownerNode.getScene() != null) {
            dialog.initOwner(ownerNode.getScene().getWindow());
        }

        dialog.initStyle(StageStyle.TRANSPARENT);

        dialog.setTitle("Change Password");
        dialog.setHeaderText("Update your account password");

        shared.utils.DialogHelper.applyCustomStyle(dialog);

        PasswordField currentPasswordField = new PasswordField();
        currentPasswordField.setPromptText("Current password");
        currentPasswordField.getStyleClass().add("dashboard-input");

        PasswordField newPasswordField = new PasswordField();
        newPasswordField.setPromptText("New password");
        newPasswordField.getStyleClass().add("dashboard-input");

        PasswordField confirmPasswordField = new PasswordField();
        confirmPasswordField.setPromptText("Confirm new password");
        confirmPasswordField.getStyleClass().add("dashboard-input");

        Label currentPasswordLabel = new Label("CURRENT PASSWORD");
        currentPasswordLabel.getStyleClass().add("form-label-register");

        Label newPasswordLabel = new Label("NEW PASSWORD");
        newPasswordLabel.getStyleClass().add("form-label-register");

        Label confirmPasswordLabel = new Label("CONFIRM NEW PASSWORD");
        confirmPasswordLabel.getStyleClass().add("form-label-register");

        Label errorLabel = new Label();
        errorLabel.getStyleClass().add("error-label");
        errorLabel.setWrapText(true);
        errorLabel.setManaged(false);

        VBox content = new VBox(10);
        content.setStyle("-fx-padding: 24;");
        content.getChildren().addAll(currentPasswordLabel, currentPasswordField, newPasswordLabel, newPasswordField,
                                    confirmPasswordLabel, confirmPasswordField, errorLabel);

        Button changeButton = new Button("CHANGE PASSWORD");
        changeButton.getStyleClass().add("login-btn-primary");
        changeButton.setMaxWidth(Double.MAX_VALUE);

        Button cancelButton = new Button("CANCEL");
        cancelButton.getStyleClass().add("login-btn-secondary");
        cancelButton.setMaxWidth(Double.MAX_VALUE);

        VBox buttonContainer = new VBox(10, changeButton, cancelButton);
        buttonContainer.setStyle("-fx-padding: 16 0 0 0;");
        content.getChildren().add(buttonContainer);

        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        Node closeButton = dialog.getDialogPane().lookupButton(ButtonType.CLOSE);
        closeButton.setVisible(false);
        closeButton.setManaged(false);

        dialog.getDialogPane().setContent(content);

        cancelButton.setOnAction(e -> dialog.close());

        changeButton.setOnAction(event -> {
            String validationError = validate(currentPasswordField.getText(), newPasswordField.getText(),
                                            confirmPasswordField.getText());

            if (validationError != null) {
                errorLabel.setText(validationError);
                errorLabel.setManaged(true);
                return;
            }

            errorLabel.setManaged(false);
            content.setDisable(true);

            Task<Response> task = new Task<>() {
                @Override
                protected Response call() throws Exception {
                    if (!ctx.isConnected()) {
                        ctx.connect();
                    }

                    Map<String, String> data = new HashMap<>();
                    data.put("username", ctx.getCurrentUser().getUsername());
                    data.put("oldPassword", currentPasswordField.getText());
                    data.put("newPassword", newPasswordField.getText());
                    return ctx.sendRequestAndWait(new Request("CHANGE_PASSWORD", data), 10);
                }
            };

            task.setOnSucceeded(e -> {
                Response response = task.getValue();

                if ("SUCCESS".equals(response.getStatus())) {
                    dialog.close();
                    Platform.runLater(() -> showAlert("Success", response.getMessage(), ownerNode));
                } else {
                    errorLabel.setText(response.getMessage());
                    errorLabel.setManaged(true);
                    content.setDisable(false);
                }
            });

            task.setOnFailed(e -> {
                errorLabel.setText("Cannot connect to server.");
                errorLabel.setManaged(true);
                content.setDisable(false);
            });

            new Thread(task).start();
        });

        dialog.setOnShown(e -> {
            Window owner = dialog.getOwner();
            if (owner != null) {
                dialog.setX(owner.getX() + (owner.getWidth() - dialog.getWidth()) / 2);
                dialog.setY(owner.getY() + (owner.getHeight() - dialog.getHeight()) / 2);
            }
        });

        dialog.showAndWait();
    }

    private static String validate(String currentPassword, String newPassword, String confirmPassword) {
        if (currentPassword == null || currentPassword.trim().isEmpty()) {
            return "Current password cannot be empty.";
        }

        String newPass = Validator.normalize(newPassword);
        String confirmPass = Validator.normalize(confirmPassword);

        if (!Validator.isValidPassword(newPass)) {
            return "New password must be at least 6 characters and include uppercase, lowercase, number, and special character.";
        }

        if (confirmPass == null || !confirmPass.equals(newPass)) {
            return "Confirm password does not match.";
        }

        if (Validator.normalize(currentPassword).equals(newPass)) {
            return "New password must be different from the current password.";
        }

        return null;
    }

    private static void showAlert(String title, String message, Node ownerNode) {
        Dialog<Void> dialog = new Dialog<>();

        if (ownerNode != null && ownerNode.getScene() != null) {
            dialog.initOwner(ownerNode.getScene().getWindow());
        }

        dialog.initStyle(StageStyle.TRANSPARENT);

        dialog.setTitle(title);
        dialog.setHeaderText(title.toUpperCase());

        shared.utils.DialogHelper.applyCustomStyle(dialog);

        Label messageLabel = new Label(message);
        messageLabel.setWrapText(true);
        messageLabel.getStyleClass().add("login-subtitle-main");

        Button okButton = new Button("OK");
        okButton.getStyleClass().add("login-btn-primary");
        okButton.setMaxWidth(Double.MAX_VALUE);
        okButton.setOnAction(e -> dialog.close());

        VBox box = new VBox(20, messageLabel, okButton);
        box.setAlignment(Pos.CENTER);
        box.setStyle("-fx-padding: 24;");

        dialog.getDialogPane().setContent(box);

        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        Node closeButton = dialog.getDialogPane().lookupButton(ButtonType.CLOSE);
        closeButton.setVisible(false);
        closeButton.setManaged(false);

        dialog.setOnShown(e -> {
            Window owner = dialog.getOwner();
            if (owner != null) {
                dialog.setX(owner.getX() + (owner.getWidth() - dialog.getWidth()) / 2);
                dialog.setY(owner.getY() + (owner.getHeight() - dialog.getHeight()) / 2);
            }
        });

        dialog.showAndWait();
    }
}