package client;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

import shared.models.Auction;
import shared.models.User;


import java.util.function.Consumer;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.List;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import shared.network.Request;
import shared.network.Response;
import shared.utils.GsonUtils;
import com.google.gson.Gson;

public class AppContext {
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private static final AppContext instance = new AppContext();
    private User currentUser;
    private Auction selectedAuction;
    private final List<Consumer<String>> messageListeners = new CopyOnWriteArrayList<>();
    private final ConcurrentHashMap<String, CompletableFuture<Response>> pendingRequests = new ConcurrentHashMap<>();
    private final Gson gson = GsonUtils.createGson();
    private Thread listenerThread;

    public static AppContext getInstance() {
        return instance;
    }

    public User getCurrentUser() {
        return currentUser;
    }
    public void setCurrentUser(User u) {
        currentUser = u;
    }
    public Auction getSelectedAuction() {
        return selectedAuction;
    }
    public void setSelectedAuction(Auction a) {
        selectedAuction = a;
    }
    public void connect() throws Exception {
        // Always disconnect first to ensure clean state
        disconnect();

        socket = new Socket("localhost", 54321);
        out = new PrintWriter(socket.getOutputStream(), true);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        
        startListenerThread();
    }
    
    private void startListenerThread() {
        if (listenerThread != null && listenerThread.isAlive()) return;
        
        listenerThread = new Thread(() -> {
            try {
                String line;
                while ((line = in.readLine()) != null) {
                    final String message = line;
                    try {
                        Response res = gson.fromJson(message, Response.class);
                        if (res.getRequestId() != null && pendingRequests.containsKey(res.getRequestId())) {
                            pendingRequests.get(res.getRequestId()).complete(res);
                            pendingRequests.remove(res.getRequestId());
                        }
                    } catch (Exception e) {
                        // Not a Response or no requestId, normal broadcast
                    }
                    
                    for (Consumer<String> listener : messageListeners) {
                        listener.accept(message);
                    }
                }
            } catch (Exception e) {
                System.out.println("Connection lost: " + e.getMessage());
                // Reset connection state on error
                socket = null;
                out = null;
                in = null;
                listenerThread = null;
            }
        });
        listenerThread.setDaemon(true);
        listenerThread.start();
    }
    
    public Response sendRequestAndWait(Request req, long timeoutSeconds) throws Exception {
        String requestId = UUID.randomUUID().toString();
        req.setRequestId(requestId);
        CompletableFuture<Response> future = new CompletableFuture<>();
        pendingRequests.put(requestId, future);
        
        out.println(gson.toJson(req));
        
        try {
            return future.get(timeoutSeconds, TimeUnit.SECONDS);
        } catch (Exception e) {
            pendingRequests.remove(requestId);
            throw e;
        }
    }

    public void addMessageListener(Consumer<String> listener) {
        messageListeners.add(listener);
    }
    
    public void removeMessageListener(Consumer<String> listener) {
        messageListeners.remove(listener);
    }

    public PrintWriter getOut() { return out; }
    public BufferedReader getIn() { return in; }

    public boolean isConnected() {
        return socket != null && !socket.isClosed();
    }

    public void disconnect() {
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
            if (listenerThread != null && listenerThread.isAlive()) {
                listenerThread.interrupt();
            }
            socket = null;
            out = null;
            in = null;
            listenerThread = null;
            messageListeners.clear();
            pendingRequests.clear();
        } catch (Exception e) {
            System.out.println("Error disconnecting: " + e.getMessage());
        }
    }
}