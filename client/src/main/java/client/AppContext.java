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

    protected AppContext() {
    }

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
        System.out.println("DEBUG CLIENT: Connecting to server...");
        // Luôn disconnect trước khi connect để đảm bảo state sạch
        disconnect();

        socket = new Socket("localhost", 54321);
        socket.setTcpNoDelay(true); // Tắt thuật toán Nagle's algorithm để tăng tốc độ gói nhỏ
        out = new PrintWriter(socket.getOutputStream(), true);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        
        System.out.println("DEBUG CLIENT: Connected. Starting listener thread...");
        startListenerThread();
    }
    
    private void startListenerThread() {
        if (listenerThread != null && listenerThread.isAlive()) {
            System.out.println("DEBUG CLIENT: Listener thread already running.");
            return;
        }
        
        final BufferedReader currentIn = this.in;
        listenerThread = new Thread(() -> {
            System.out.println("DEBUG CLIENT: Listener thread started.");
            try {
                String line;
                while (currentIn != null && (line = currentIn.readLine()) != null) {
                    final String message = line;
                    System.out.println("DEBUG CLIENT: Received from server: " + message);
                    try {
                        Response res = gson.fromJson(message, Response.class);
                        if (res.getRequestId() != null) {
                            CompletableFuture<Response> future = pendingRequests.get(res.getRequestId());
                            if (future != null) {
                                System.out.println("DEBUG CLIENT: Completing future for request " + res.getRequestId());
                                future.complete(res);
                                pendingRequests.remove(res.getRequestId());
                            } else {
                                System.out.println("DEBUG CLIENT: No pending request found for ID " + res.getRequestId());
                            }
                        }
                    } catch (Exception e) {
                        System.out.println("DEBUG CLIENT: Message is not a standard Response or has no ID: " + e.getMessage());
                    }
                    
                    for (Consumer<String> listener : messageListeners) {
                        listener.accept(message);
                    }
                }
                System.out.println("DEBUG CLIENT: Listener thread reached end of stream.");
            } catch (Throwable e) {
                System.err.println("DEBUG CLIENT: Connection lost or error in listener: " + e.getMessage());
                e.printStackTrace();
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

    public PrintWriter getOut() {
        return out;
    }
    
    public BufferedReader getIn() {
        return in;
    }

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
