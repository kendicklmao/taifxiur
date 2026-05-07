package client;

import shared.network.Request;
import shared.network.Response;

public class FakeAppContext extends AppContext {
    private Response responseToReturn;
    private Exception exceptionToThrow;
    private boolean connectCalled = false;

    public void setResponseToReturn(Response response) {
        this.responseToReturn = response;
        this.exceptionToThrow = null;
    }

    public void setExceptionToThrow(Exception exception) {
        this.exceptionToThrow = exception;
        this.responseToReturn = null;
    }

    @Override
    public void connect() throws Exception {
        connectCalled = true;
        if (exceptionToThrow != null && exceptionToThrow.getMessage().contains("Connection")) {
            throw exceptionToThrow;
        }
    }

    @Override
    public Response sendRequestAndWait(Request req, long timeoutSeconds) throws Exception {
        if (exceptionToThrow != null) {
            throw exceptionToThrow;
        }
        return responseToReturn;
    }

    public boolean isConnectCalled() {
        return connectCalled;
    }
}