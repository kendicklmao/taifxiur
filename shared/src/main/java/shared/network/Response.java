package shared.network;

public class Response {
    private String status;
    private String message;
    private String requestId;

    public Response(String status, String message) {
        this.status = status;
        this.message = message;
    }

    public Response(String status, String message, String requestId) {
        this.status = status;
        this.message = message;
        this.requestId = requestId;
    }

    public String getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }
}