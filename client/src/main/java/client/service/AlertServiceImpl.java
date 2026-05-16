package client.service;

import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
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
            Alert.AlertType type = title.equalsIgnoreCase("Error") ? Alert.AlertType.ERROR : Alert.AlertType.INFORMATION;
            Alert alert = new Alert(type);
            
            if (ownerWindow != null) {
                alert.initOwner(ownerWindow);
            }
            alert.initStyle(javafx.stage.StageStyle.UTILITY);

            alert.setTitle(title);
            alert.setHeaderText(title.toUpperCase());
            alert.setContentText(message);

            // Áp dụng style tùy chỉnh để có màu tối và bo góc
            shared.utils.DialogHelper.applyCustomStyle(alert);

            // Tinh chỉnh label nội dung để bọc text nếu quá dài
            Node content = alert.getDialogPane().lookup(".content.label");
            if (content instanceof Label) {
                ((Label) content).setWrapText(true);
                ((Label) content).getStyleClass().add("login-subtitle-main");
            }

            alert.setOnHidden(ev -> SoundManager.stopPopup());

            SoundManager.playPopup();
            alert.show();
        });
    }
}