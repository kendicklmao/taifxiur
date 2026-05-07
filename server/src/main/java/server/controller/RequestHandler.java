package server.controller;

import shared.network.Request;
import shared.network.Response;

public interface RequestHandler {
    Response handle(Request request, ClientHandler clientHandler);
}
