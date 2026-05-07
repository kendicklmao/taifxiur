package client;

import javafx.scene.Node;
import javafx.stage.Window;

public interface IAlertService {
    void showAlert(String title, String message, Node ownerNode);
    void showAlert(String title, String message, Window ownerWindow);
}