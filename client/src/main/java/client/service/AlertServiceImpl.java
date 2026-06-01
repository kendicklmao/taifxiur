package client.service;

import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.stage.Window;

public class AlertServiceImpl implements IAlertService {
    @Override
    public void showAlert(String title, String message, Node ownerNode) {
        Window ownerWindow = ownerNode != null ? ownerNode.getScene().getWindow() : null;
        showAlert(title, message, ownerWindow);
    }

    @Override
    public void showAlert(String title, String message, Window ownerWindow) {
        Platform.runLater(() -> {
            Alert.AlertType type = title.equalsIgnoreCase("Error") ? Alert.AlertType.ERROR: Alert.AlertType.INFORMATION;
            Alert alert = createSplashAlert(type, title, message, ownerWindow);

            alert.setOnHidden(ev -> SoundManager.stopPopup());

            SoundManager.playPopup();
            alert.show();
        });
    }

    @Override
    public boolean showConfirmation(String title, String message, Window ownerWindow) {
        // Confirmation cần chạy trên FX Thread và đợi kết quả
        if (!Platform.isFxApplicationThread()) {
            final java.util.concurrent.atomic.AtomicBoolean result = new java.util.concurrent.atomic.AtomicBoolean(
                    false);
            java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(1);
            Platform.runLater(() -> {
                result.set(internalShowConfirmation(title, message, ownerWindow));
                latch.countDown();
            });
            try {
                latch.await();
            } catch (InterruptedException e) {
            }
            return result.get();
        } else {
            return internalShowConfirmation(title, message, ownerWindow);
        }
    }

    private boolean internalShowConfirmation(String title, String message, Window ownerWindow) {
        Alert alert = createSplashAlert(Alert.AlertType.CONFIRMATION, title, message, ownerWindow);

        javafx.scene.Node cancelBtn = alert.getDialogPane().lookupButton(javafx.scene.control.ButtonType.CANCEL);
        if (cancelBtn != null) {
            cancelBtn.getStyleClass().add("btn-ghost-white");
        }

        SoundManager.playPopup();
        java.util.Optional<javafx.scene.control.ButtonType> result = alert.showAndWait();
        SoundManager.stopPopup();
        return result.isPresent() && result.get() == javafx.scene.control.ButtonType.OK;
    }

    private Alert createSplashAlert(Alert.AlertType type, String title, String message, Window ownerWindow) {
        Alert alert = new Alert(type);
        if (ownerWindow != null) {
            alert.initOwner(ownerWindow);
        }
        alert.initStyle(javafx.stage.StageStyle.UTILITY);

        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setGraphic(null);
        alert.setContentText(null);

        javafx.scene.layout.VBox mainBox = new javafx.scene.layout.VBox(15);
        mainBox.setAlignment(javafx.geometry.Pos.CENTER);
        mainBox.setStyle("-fx-padding: 30 24 30 24;");

        String iconPath = "/pics/success.png";
        if (type == Alert.AlertType.ERROR) {
            iconPath = "/pics/error.png";
        } else if (type == Alert.AlertType.CONFIRMATION) {
            iconPath = "/pics/question.png";
        }

        try {
            javafx.scene.image.ImageView statusIcon = new javafx.scene.image.ImageView(
                    new javafx.scene.image.Image(getClass().getResourceAsStream(iconPath)));
            statusIcon.setFitWidth(256); // Điều chỉnh kích thước vừa phải
            statusIcon.setFitHeight(256);
            mainBox.getChildren().add(statusIcon);
        } catch (Exception e) {
        }

        javafx.scene.control.Label titleLabel = new javafx.scene.control.Label(title.toUpperCase());
        titleLabel.getStyleClass().add("login-title-main");
        titleLabel.setAlignment(javafx.geometry.Pos.CENTER);
        mainBox.getChildren().add(titleLabel);

        javafx.scene.control.Label messageLabel = new javafx.scene.control.Label(message);
        messageLabel.setWrapText(true);
        messageLabel.getStyleClass().add("login-subtitle-main");
        messageLabel.setAlignment(javafx.geometry.Pos.CENTER);
        messageLabel.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
        mainBox.getChildren().add(messageLabel);

        alert.getDialogPane().setContent(mainBox);

        javafx.scene.Node graphicContainer = alert.getDialogPane().lookup(".graphic-container");
        if (graphicContainer != null) {
            graphicContainer.setStyle("-fx-min-width: 0; -fx-pref-width: 0; -fx-max-width: 0; -fx-padding: 0;");
            graphicContainer.setVisible(false);
            graphicContainer.setManaged(false);
        }
        mainBox.setMaxWidth(Double.MAX_VALUE);

        shared.utils.DialogHelper.applyCustomStyle(alert);
        return alert;
    }
}
