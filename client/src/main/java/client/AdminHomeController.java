package client;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import com.google.gson.Gson;
import shared.models.Auction;
import shared.models.User;
import shared.models.AdminActionLog;
import shared.network.Request;
import shared.network.Response;
import shared.utils.GsonUtils;

import java.util.HashMap;
import java.util.Map;

public class AdminHomeController {
    @FXML private ListView<Auction> allAuctionsList;
    @FXML private ListView<User> allUsersList;
    @FXML private ListView<AdminActionLog> adminActionLogsList;
    @FXML private TextField usernameField;
    @FXML private TextArea userStatusArea;
    @FXML private Label welcomeLabel;

    private final AppContext ctx = AppContext.getInstance();
    private final Gson gson = GsonUtils.createGson();

    @FXML
    public void initialize() {
        welcomeLabel.setText("Welcome " + ctx.getCurrentUser().getUsername());
        setupAuctionListCell();
        setupUserListCell();
        refreshAuctions();
        refreshUsers();
        refreshAdminActionLogs();
    }

    private void setupAuctionListCell() {
        allAuctionsList.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(Auction item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(
                        "📦 " + item.getItem().getName() +
                        " | Seller: " + item.getSeller().getUsername() +
                        " | Price: " + item.getCurrentPrice() +
                        " | Status: " + item.getStatus()
                    );
                }
            }
        });
    }

    private void setupUserListCell() {
        allUsersList.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(User item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    String status = item.isBanned() ? "BANNED" : "ACTIVE";
                    setText(item.getUsername() + " (" + item.getRole() + ") - " + status);
                }
            }
        });
    }

    @FXML
    public void refreshAdminActionLogs() {
        try {
            Request req = new Request("GET_ADMIN_ACTION_LOGS", new HashMap<>());
            Response response = ctx.sendRequestAndWait(req, 5);
            if ("SUCCESS".equals(response.getStatus())) {
                AdminActionLog[] logs = gson.fromJson(response.getMessage(), AdminActionLog[].class);
                Platform.runLater(() -> adminActionLogsList.getItems().setAll(logs));
            }
        } catch (Exception e) {
            showAlert("Error", "Failed to load admin action logs: " + e.getMessage());
        }
    }

    @FXML
    public void refreshAuctions() {
        try {
            Request req = new Request("GET_AUCTIONS", new HashMap<>());
            Response response = ctx.sendRequestAndWait(req, 5);
            if ("SUCCESS".equals(response.getStatus())) {
                Auction[] auctions = gson.fromJson(response.getMessage(), Auction[].class);
                Platform.runLater(() -> allAuctionsList.getItems().setAll(auctions));
            }
        } catch (Exception e) {
            showAlert("Error", "Failed to load auctions: " + e.getMessage());
        }
    }

    @FXML
    public void refreshUsers() {
        try {
            Request req = new Request("GET_ALL_USERS", new HashMap<>());
            Response response = ctx.sendRequestAndWait(req, 5);
            if ("SUCCESS".equals(response.getStatus())) {
                User[] users = gson.fromJson(response.getMessage(), User[].class);
                Platform.runLater(() -> allUsersList.getItems().setAll(users));
            }
        } catch (Exception e) {
            showAlert("Error", "Failed to load users: " + e.getMessage());
        }
    }

    @FXML
    public void handleBanUser() {
        String username = usernameField.getText().trim();
        if (username.isEmpty()) {
            showAlert("Error", "Please enter a username");
            return;
        }

        try {
            Map<String, String> data = new HashMap<>();
            data.put("username", username);
            Request req = new Request("BAN_USER", data);
            Response response = ctx.sendRequestAndWait(req, 5);

            if ("SUCCESS".equals(response.getStatus())) {
                showAlert("Success", "User " + username + " has been banned!");
                userStatusArea.appendText("\n✓ Banned user: " + username);
                refreshUsers();
                usernameField.clear();
            } else {
                showAlert("Error", response.getMessage());
            }
        } catch (Exception e) {
            showAlert("Error", "Failed to ban user: " + e.getMessage());
        }
    }

    @FXML
    public void handleUnbanUser() {
        String username = usernameField.getText().trim();
        if (username.isEmpty()) {
            showAlert("Error", "Please enter a username");
            return;
        }

        try {
            Map<String, String> data = new HashMap<>();
            data.put("username", username);
            Request req = new Request("UNBAN_USER", data);
            Response response = ctx.sendRequestAndWait(req, 5);

            if ("SUCCESS".equals(response.getStatus())) {
                showAlert("Success", "User " + username + " has been unbanned!");
                userStatusArea.appendText("\n✓ Unbanned user: " + username);
                refreshUsers();
                usernameField.clear();
            } else {
                showAlert("Error", response.getMessage());
            }
        } catch (Exception e) {
            showAlert("Error", "Failed to unban user: " + e.getMessage());
        }
    }

    @FXML
    public void handleLogout() {
        try {
            Map<String, String> data = new HashMap<>();
            data.put("username", ctx.getCurrentUser().getUsername());
            Request req = new Request("LOGOUT", data);
            ctx.sendRequestAndWait(req, 5);
        } catch (Exception e) {
            // Ignore, proceed with logout
        }
        ctx.setCurrentUser(null);
        Navigator.switchScene("login.fxml");
    }

    private void showAlert(String title, String msg) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setContentText(msg);
        alert.showAndWait();
    }
}

