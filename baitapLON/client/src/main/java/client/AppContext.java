package client;

import shared.models.Auction;
import shared.models.User;

public class AppContext {
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
}