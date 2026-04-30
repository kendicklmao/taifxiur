package client;

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
        Dialog<Void> dialog = new Dialog<>();

        // Set the owner of the dialog so it appears on top
        if (ownerNode != null) {
            Window ownerWindow = ownerNode.getScene().getWindow();
            dialog.initOwner(ownerWindow);
        }

        // --- THIS IS THE FIX ---
        // Initialize the dialog with a transparent style.
        // The faulty line that caused the crash has been REMOVED.
        dialog.initStyle(StageStyle.TRANSPARENT);

        dialog.setTitle(title);
        dialog.setHeaderText(title.toUpperCase());

        // Apply the dark theme stylesheet
        dialog.getDialogPane().getStylesheets().add(
                getClass().getResource("/styles.css").toExternalForm()
        );
        dialog.getDialogPane().getStyleClass().add("my-dialog");

        // --- Custom Layout for the Alert ---
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

        // Remove the default button bar
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        Node closeButton = dialog.getDialogPane().lookupButton(ButtonType.CLOSE);
        closeButton.setVisible(false);
        closeButton.setManaged(false);

        // Manually center the dialog on its owner
        dialog.setOnShown(e -> {
            Window owner = dialog.getOwner();
            if (owner != null) {
                double ownerX = owner.getX();
                double ownerY = owner.getY();
                double ownerWidth = owner.getWidth();
                double ownerHeight = owner.getHeight();

                double dialogWidth = dialog.getWidth();
                double dialogHeight = dialog.getHeight();

                dialog.setX(ownerX + (ownerWidth - dialogWidth) / 2);
                dialog.setY(ownerY + (ownerHeight - dialogHeight) / 2);
            }
        });

        dialog.showAndWait();
    }
}