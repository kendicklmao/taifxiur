package shared.network;

import java.util.Map;

public class Request {
    private String action;
    private Map<String, String> data;
    private String requestId;

    public Request(String action, Map<String, String> data) {
        this.action = action;
        this.data = data;
    }

    public Request(String action, Map<String, String> data, String requestId) {
        this.action = action;
        this.data = data;
        this.requestId = requestId;
    }

    public String getAction() {
        return action;
    }

    public Map<String, String> getData() {
        return data;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }
}