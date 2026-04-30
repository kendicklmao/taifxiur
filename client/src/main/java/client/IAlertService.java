package client;

import javafx.scene.Node;

public interface IAlertService {
    void showAlert(String title, String message, Node ownerNode);
}
