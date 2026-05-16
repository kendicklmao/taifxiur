package server.controller;

import com.google.gson.Gson;

import shared.utils.GsonUtils;
import server.service.UserService;
import shared.network.Request;
import shared.network.Response;

import java.io.BufferedReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

public class ClientHandler implements Runnable {
    private static final List<ClientHandler> activeClients = new CopyOnWriteArrayList<>();

    private final Socket socket;
    private final Gson gson = GsonUtils.createGson();
    private PrintWriter out;
    private String loggedInUsername;

    private final UserService userService;
    private final Map<String, RequestHandler> handlers;

    public ClientHandler(Socket socket, UserService userService, Map<String, RequestHandler> handlers) {
        this.socket = socket;
        this.userService = userService;
        this.handlers = handlers;
    }

    @Override
    public void run() {
        try {
            BufferedReader in = new BufferedReader(new java.io.InputStreamReader(socket.getInputStream(), java.nio.charset.StandardCharsets.UTF_8));
            out = new PrintWriter(new java.io.OutputStreamWriter(socket.getOutputStream(), java.nio.charset.StandardCharsets.UTF_8), true);

            activeClients.add(this);

            String clientMessage;
            while ((clientMessage = in.readLine()) != null) {
                Request request = gson.fromJson(clientMessage, Request.class);
                Response response = handleRequest(request);
                if (response != null) {
                    if (request.getRequestId() != null) {
                        response.setRequestId(request.getRequestId());
                    }
                    String responseJson = gson.toJson(response);
                    sendMessage(responseJson);
                }
            }
        } catch (Throwable e) {
            System.err.println("ERROR in ClientHandler (" + socket.getInetAddress() + "): " + e.getMessage());
            e.printStackTrace();
        } finally {
            activeClients.remove(this);
            if (loggedInUsername != null) {
                userService.logout(loggedInUsername);
            }
        }
    }

    public void sendMessage(String jsonMessage) {
        if (out != null) {
            out.println(jsonMessage);
        }
    }

    public static void broadcast(String message) {
        for (ClientHandler client: activeClients) {
            client.sendMessage(message);
        }
    }

    public String getLoggedInUsername() {
        return loggedInUsername;
    }

    public void setLoggedInUsername(String username) {
        this.loggedInUsername = username;
    }

    private Response handleRequest(Request request) {
        String action = request.getAction();
        if (action == null) {
            return new Response("FAIL", "Invalid action");
        }

        RequestHandler handler = handlers.get(action);
        if (handler != null) {
            return handler.handle(request, this);
        }

        return new Response("FAIL", "Unsupported function");
    }
}