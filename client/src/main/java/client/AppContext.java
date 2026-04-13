package client;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

import shared.models.Auction;
import shared.models.User;


public class AppContext {
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private static final AppContext instance = new AppContext();
    private User currentUser;
    private Auction selectedAuction;

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
        socket = new Socket("localhost", 54321);
        out = new PrintWriter(socket.getOutputStream(), true);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
    }
    public PrintWriter getOut() { return out; }
    public BufferedReader getIn() { return in; }
}