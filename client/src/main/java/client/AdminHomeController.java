package client;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import shared.models.Auction;
import shared.models.User;
import shared.models.AdminActionLog;
import shared.network.Request;
import shared.network.Response;
import shared.utils.GsonUtils;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AdminHomeController {
    @FXML private ListView<Auction> allAuctionsList;
    @FXML private ListView<User> allUsersList;
    @FXML private ListView<AdminActionLog> adminActionLogsList;
    @FXML private ListView<Map<String, String>> depositRequestsList;
    @FXML private ListView<Map<String, String>> withdrawRequestsList;
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
        setupDepositRequestListCell();
        setupWithdrawRequestListCell();
        refreshAuctions();
        refreshUsers();
        refreshAdminActionLogs();
        refreshWalletRequests();
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

    private void setupDepositRequestListCell() {
        depositRequestsList.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(Map<String, String> item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText("Deposit | " + item.get("username") + " | $" + item.get("amount") + " | " + item.get("createdAt"));
                }
            }
        });
    }

    private void setupWithdrawRequestListCell() {
        withdrawRequestsList.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(Map<String, String> item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText("Withdraw | " + item.get("username") + " | $" + item.get("amount") + " | " + item.get("bankAccount"));
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
    public void refreshWalletRequests() {
        refreshDepositRequests();
        refreshWithdrawRequests();
    }

    @FXML
    public void refreshDepositRequests() {
        try {
            Response response = ctx.sendRequestAndWait(new Request("GET_PENDING_DEPOSIT_REQUESTS", new HashMap<>()), 5);
            if ("SUCCESS".equals(response.getStatus())) {
                Type listType = new TypeToken<List<Map<String, String>>>() {}.getType();
                List<Map<String, String>> requests = gson.fromJson(response.getMessage(), listType);
                Platform.runLater(() -> depositRequestsList.getItems().setAll(requests));
            }
        } catch (Exception e) {
            showAlert("Error", "Failed to load deposit requests: " + e.getMessage());
        }
    }

    @FXML
    public void refreshWithdrawRequests() {
        try {
            Response response = ctx.sendRequestAndWait(new Request("GET_PENDING_WITHDRAW_REQUESTS", new HashMap<>()), 5);
            if ("SUCCESS".equals(response.getStatus())) {
                Type listType = new TypeToken<List<Map<String, String>>>() {}.getType();
                List<Map<String, String>> requests = gson.fromJson(response.getMessage(), listType);
                Platform.runLater(() -> withdrawRequestsList.getItems().setAll(requests));
            }
        } catch (Exception e) {
            showAlert("Error", "Failed to load withdraw requests: " + e.getMessage());
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
    public void handleApproveDepositRequest() {
        processWalletRequest(depositRequestsList.getSelectionModel().getSelectedItem(), "APPROVE_DEPOSIT_REQUEST");
    }

    @FXML
    public void handleRejectDepositRequest() {
        processWalletRequest(depositRequestsList.getSelectionModel().getSelectedItem(), "REJECT_DEPOSIT_REQUEST");
    }

    @FXML
    public void handleApproveWithdrawRequest() {
        processWalletRequest(withdrawRequestsList.getSelectionModel().getSelectedItem(), "APPROVE_WITHDRAW_REQUEST");
    }

    @FXML
    public void handleRejectWithdrawRequest() {
        processWalletRequest(withdrawRequestsList.getSelectionModel().getSelectedItem(), "REJECT_WITHDRAW_REQUEST");
    }

    @FXML
    public void handleChangePassword() {
        ChangePasswordSupport.showDialog(ctx);
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

    private void processWalletRequest(Map<String, String> requestItem, String action) {
        if (requestItem == null) {
            showAlert("Error", "Please select a request first");
            return;
        }

        try {
            Map<String, String> data = new HashMap<>();
            data.put("requestId", requestItem.get("id"));
            Response response = ctx.sendRequestAndWait(new Request(action, data), 5);

            if ("SUCCESS".equals(response.getStatus())) {
                showAlert("Success", response.getMessage());
                refreshWalletRequests();
            } else {
                showAlert("Error", response.getMessage());
            }
        } catch (Exception e) {
            showAlert("Error", "Failed to process request: " + e.getMessage());
        }
    }

    private void showAlert(String title, String msg) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setContentText(msg);
        alert.showAndWait();
    }
}

