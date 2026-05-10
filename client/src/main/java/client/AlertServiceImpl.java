package client;

import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.StageStyle;
import javafx.stage.Window;

public class AlertServiceImpl implements IAlertService {
    @Override
    public void showAlert(String title, String message, Node ownerNode) {
        Window ownerWindow = ownerNode != null ? ownerNode.getScene().getWindow() : null;
        showAlert(title, message, ownerWindow);
    }

    @Override
    public void showAlert(String title, String message, Window ownerWindow) {
        Dialog<Void> dialog = new Dialog<>();

        if (ownerWindow != null) {
            dialog.initOwner(ownerWindow);
        }

        dialog.setOnShown(ev -> {
            Platform.runLater(() -> {
                double dw = dialog.getWidth();
                double dh = dialog.getHeight();
                
                if (dw <= 0 || Double.isNaN(dw)) {
                    dw = dialog.getDialogPane().getWidth();
                    if (dw <= 0) {
                        dw = dialog.getDialogPane().prefWidth(-1);
                    }
                }
                if (dh <= 0 || Double.isNaN(dh)) {
                    dh = dialog.getDialogPane().getHeight();
                    if (dh <= 0) {
                        dh = dialog.getDialogPane().prefHeight(-1);
                    }
                }
                
                javafx.stage.Screen screen = javafx.stage.Screen.getPrimary();
                if (ownerWindow != null && ownerWindow.getWidth() > 0 && ownerWindow.getHeight() > 0) {
                    java.util.List<javafx.stage.Screen> screens = javafx.stage.Screen.getScreensForRectangle(
                        ownerWindow.getX(), ownerWindow.getY(), ownerWindow.getWidth(), ownerWindow.getHeight()
                    );
                    if (!screens.isEmpty()) {
                        screen = screens.get(0);
                    }
                }
                
                if (dw > 0 && dh > 0) {
                    javafx.geometry.Rectangle2D bounds = screen.getVisualBounds();
                    double x = bounds.getMinX() + (bounds.getWidth() - dw) / 2;
                    double y = bounds.getMinY() + (bounds.getHeight() - dh) / 2;
                    dialog.setX(x);
                    dialog.setY(y);
                }
            });
        });

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

        VBox alertContent = new VBox(20, messageLabel, okButton);
        alertContent.setAlignment(Pos.CENTER);
        alertContent.setStyle("-fx-padding: 24;");
        dialog.getDialogPane().setContent(alertContent);

        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        Node closeButton = dialog.getDialogPane().lookupButton(ButtonType.CLOSE);
        closeButton.setVisible(false);
        closeButton.setManaged(false);

        dialog.show();
    }
}