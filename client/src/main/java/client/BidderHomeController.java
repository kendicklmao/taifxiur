package client;

import javafx.scene.control.Alert;

public class BidderHomeController {

    public void handleBid() {
        System.out.println("Bid!");
    }

    public void handleLogout() {
        Navigator.switchScene("login.fxml");
    }
    private void showAlert(String title, String msg) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setContentText(msg);
        alert.showAndWait();
    }
}