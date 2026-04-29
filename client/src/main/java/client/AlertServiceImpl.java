package client;

import javafx.scene.control.Alert;

public class AlertServiceImpl implements IAlertService {
    @Override
    public void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setContentText(message);
        alert.showAndWait();
    }
}

