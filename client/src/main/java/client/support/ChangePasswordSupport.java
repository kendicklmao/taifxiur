package client.support;

import client.AppContext;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.StageStyle;
import shared.network.Request;
import shared.network.Response;
import shared.utils.Validator;

import java.util.HashMap;
import java.util.Map;

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
        titleLabel.setAlignment(Pos.CENTER);
        titleLabel.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);

        Label currentPasswordLabel = new Label("CURRENT PASSWORD");
        currentPasswordLabel.getStyleClass().add("form-label-login");

        Label newPasswordLabel = new Label("NEW PASSWORD");
        newPasswordLabel.getStyleClass().add("form-label-login");

        Label confirmPasswordLabel = new Label("CONFIRM NEW PASSWORD");
        confirmPasswordLabel.getStyleClass().add("form-label-login");

        PasswordField currentPasswordField = new PasswordField();
        PasswordField newPasswordField = new PasswordField();
        PasswordField confirmPasswordField = new PasswordField();

        StackPane currentPasswordPane =
                createPasswordField(currentPasswordField, "Current password");

        StackPane newPasswordPane =
                createPasswordField(newPasswordField, "New password");

        StackPane confirmPasswordPane =
                createPasswordField(confirmPasswordField, "Confirm new password");

        Label errorLabel = new Label();
        errorLabel.getStyleClass().add("error-label");
        errorLabel.setWrapText(true);
        errorLabel.setManaged(false);
        errorLabel.setPrefWidth(400);
        errorLabel.setMinHeight(Region.USE_PREF_SIZE);

        VBox content = new VBox(10);
        content.setStyle("-fx-padding: 24;");

        content.getChildren().addAll(
                titleLabel,
                new Region(),

                currentPasswordLabel,
                currentPasswordPane,

                newPasswordLabel,
                newPasswordPane,

                confirmPasswordLabel,
                confirmPasswordPane,

                errorLabel
        );

        Button changeButton = new Button("CHANGE PASSWORD");
        changeButton.getStyleClass().add("login-btn-primary");
        changeButton.setMaxWidth(Double.MAX_VALUE);
        changeButton.setPrefHeight(45);

        Button cancelButton = new Button("CANCEL");
        cancelButton.getStyleClass().add("btn-ghost-white");
        cancelButton.setMaxWidth(Double.MAX_VALUE);
        cancelButton.setPrefHeight(45);

        VBox buttonContainer = new VBox(10, changeButton, cancelButton);
        buttonContainer.setAlignment(Pos.CENTER);
        buttonContainer.setFillWidth(true);
        buttonContainer.setStyle("-fx-padding: 16 0 0 0;");

        content.getChildren().add(buttonContainer);

        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);

        Node internalCloseButton =
                dialog.getDialogPane().lookupButton(ButtonType.CLOSE);

        if (internalCloseButton != null) {
            internalCloseButton.setVisible(false);
            internalCloseButton.setManaged(false);
        }

        shared.utils.DialogHelper.applyCustomStyle(dialog);

        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().setPrefWidth(450);

        cancelButton.setOnAction(e -> dialog.close());

        changeButton.setOnAction(event -> {

            String validationError = validate(
                    currentPasswordField.getText(),
                    newPasswordField.getText(),
                    confirmPasswordField.getText()
            );

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
                    data.put("username",
                            ctx.getCurrentUser().getUsername());
                    data.put("oldPassword",
                            currentPasswordField.getText());
                    data.put("newPassword",
                            newPasswordField.getText());

                    return ctx.sendRequestAndWait(
                            new Request("CHANGE_PASSWORD", data),
                            10
                    );
                }
            };

            task.setOnSucceeded(e -> {

                Response response = task.getValue();

                if ("SUCCESS".equals(response.getStatus())) {

                    dialog.close();

                    Platform.runLater(() ->
                            ctx.getAlertService().showAlert(
                                    "Success",
                                    response.getMessage(),
                                    ownerNode
                            ));

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

    private static String validate(
            String currentPassword,
            String newPassword,
            String confirmPassword) {

        if (currentPassword == null ||
                currentPassword.trim().isEmpty()) {
            return "Current password cannot be empty.";
        }

        String newPass = Validator.normalize(newPassword);
        String confirmPass = Validator.normalize(confirmPassword);

        if (!Validator.isValidPassword(newPass)) {
            return "New password must be at least 6 characters and include uppercase, lowercase, number, and special character.";
        }

        if (confirmPass == null ||
                !confirmPass.equals(newPass)) {
            return "Confirm password does not match.";
        }

        if (Validator.normalize(currentPassword)
                .equals(newPass)) {
            return "New password must be different from the current password.";
        }

        return null;
    }

    private static StackPane createPasswordField(
            PasswordField passwordField,
            String promptText) {

        passwordField.setPromptText(promptText);
        passwordField.getStyleClass()
                .addAll("login-input", "password-input-with-eye");

        TextField visibleField = new TextField();
        visibleField.setPromptText(promptText);
        visibleField.getStyleClass()
                .addAll("login-input", "password-input-with-eye");

        visibleField.setVisible(false);
        visibleField.setManaged(false);

        visibleField.textProperty()
                .bindBidirectional(passwordField.textProperty());

        Button eyeBtn = new Button("👁");
        eyeBtn.getStyleClass().add("password-eye-btn");
        eyeBtn.setFocusTraversable(false);

        eyeBtn.setOnAction(e -> {
            boolean showing = visibleField.isVisible();

            visibleField.setVisible(!showing);
            visibleField.setManaged(!showing);

            passwordField.setVisible(showing);
            passwordField.setManaged(showing);
        });

        StackPane pane = new StackPane(
                passwordField,
                visibleField,
                eyeBtn
        );

        StackPane.setAlignment(eyeBtn, Pos.CENTER_RIGHT);

        return pane;
    }
}