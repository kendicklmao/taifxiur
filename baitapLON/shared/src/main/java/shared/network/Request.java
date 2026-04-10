package shared.network;

import java.util.Map;

public class Request {
    private String action;
    private Map<String, String> data;

    public Request(String action, Map<String, String> data) {
        this.action = action;
        this.data = data;
    }

    public String getAction() {
        return action;
    }

    public Map<String, String> getData() {
        return data;
    }
}