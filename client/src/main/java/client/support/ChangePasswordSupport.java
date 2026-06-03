package client.support;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.layout.VBox;
import javafx.stage.StageStyle;
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

        dialog.initStyle(StageStyle.UTILITY);

        dialog.setTitle("Change Password");
        Label titleLabel = new Label("CHANGE PASSWORD");
        titleLabel.getStyleClass().add("login-title-main");
        titleLabel.setMaxWidth(Double.MAX_VALUE);
        titleLabel.setAlignment(javafx.geometry.Pos.CENTER);
        titleLabel.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);

        Label currentPasswordLabel = new Label("CURRENT PASSWORD");
        currentPasswordLabel.getStyleClass().add("form-label-login");

        PasswordField currentPasswordField = new PasswordField();
        currentPasswordField.setPromptText("Current password");
        currentPasswordField.getStyleClass().add("login-input");

        Label newPasswordLabel = new Label("NEW PASSWORD");
        newPasswordLabel.getStyleClass().add("form-label-login");

        PasswordField newPasswordField = new PasswordField();
        newPasswordField.setPromptText("New password");
        newPasswordField.getStyleClass().add("login-input");

        Label confirmPasswordLabel = new Label("CONFIRM NEW PASSWORD");
        confirmPasswordLabel.getStyleClass().add("form-label-login");

        PasswordField confirmPasswordField = new PasswordField();
        confirmPasswordField.setPromptText("Confirm new password");
        confirmPasswordField.getStyleClass().add("login-input");

        Label errorLabel = new Label();
        errorLabel.getStyleClass().add("error-label");
        errorLabel.setWrapText(true);
        errorLabel.setManaged(false);
        errorLabel.setPrefWidth(400);
        errorLabel.setMinHeight(javafx.scene.layout.Region.USE_PREF_SIZE);

        VBox content = new VBox(10);
        content.setStyle("-fx-padding: 24;");
        content.getChildren().addAll(titleLabel, new javafx.scene.layout.Region(),
                                    currentPasswordLabel, currentPasswordField, newPasswordLabel, newPasswordField,
                                    confirmPasswordLabel, confirmPasswordField, errorLabel);

        Button changeButton = new Button("CHANGE PASSWORD");
        changeButton.getStyleClass().add("login-btn-primary");
        changeButton.setMaxWidth(Double.MAX_VALUE);
        changeButton.setPrefHeight(45);

        Button cancelButton = new Button("CANCEL");
        cancelButton.getStyleClass().add("btn-ghost-white");
        cancelButton.setMaxWidth(Double.MAX_VALUE);
        cancelButton.setPrefHeight(45);

        changeButton.setStyle("-fx-padding: 12 0 12 0;");
        cancelButton.setStyle("-fx-padding: 12 0 12 0;");

        VBox buttonContainer = new VBox(10, changeButton, cancelButton);
        buttonContainer.setStyle("-fx-padding: 16 0 0 0;");
        buttonContainer.setAlignment(javafx.geometry.Pos.CENTER);
        buttonContainer.setFillWidth(true);
        content.getChildren().add(buttonContainer);

        dialog.getDialogPane().getButtonTypes().add(javafx.scene.control.ButtonType.CLOSE);
        Node internalCloseButton = dialog.getDialogPane().lookupButton(javafx.scene.control.ButtonType.CLOSE);
        if (internalCloseButton != null) {
            internalCloseButton.setVisible(false);
            internalCloseButton.setManaged(false);
        }

        shared.utils.DialogHelper.applyCustomStyle(dialog);
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().setPrefWidth(450);

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
                    Platform.runLater(() -> ctx.getAlertService().showAlert("Success", response.getMessage(), ownerNode));
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

}