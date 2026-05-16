package client;

import client.service.IAlertService;
import javafx.scene.Node;
import javafx.stage.Window;

public class FakeAlertService implements IAlertService {
    private String lastTitle;
    private String lastMessage;
    private int callCount = 0;

    @Override
    public void showAlert(String title, String message, Node ownerNode) {
        this.lastTitle = title;
        this.lastMessage = message;
        this.callCount++;
    }

    @Override
    public void showAlert(String title, String message, Window ownerWindow) {
        this.lastTitle = title;
        this.lastMessage = message;
        this.callCount++;
    }

    @Override
    public boolean showConfirmation(String title, String message, Window ownerWindow) {
        this.lastTitle = title;
        this.lastMessage = message;
        this.callCount++;
        return true; // Mặc định giả lập người dùng nhấn OK
    }

    public String getLastTitle() {
        return lastTitle;
    }

    public String getLastMessage() {
        return lastMessage;
    }

    public int getCallCount() {
        return callCount;
    }
}