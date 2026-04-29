package client;

public class FakeAlertService implements IAlertService {
    private String lastTitle;
    private String lastMessage;
    private int callCount = 0;

    @Override
    public void showAlert(String title, String message) {
        this.lastTitle = title;
        this.lastMessage = message;
        this.callCount++;
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

