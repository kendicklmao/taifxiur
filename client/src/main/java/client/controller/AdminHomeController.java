package client.controller;

import com.google.gson.reflect.TypeToken;

import client.support.AuctionDetailViewBuilder;
import client.support.ChangePasswordSupport;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.concurrent.Task;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

import javafx.util.Duration;
import shared.models.AdminActionLog;
import shared.models.Auction;
import shared.models.User;
import shared.network.Request;
import shared.network.Response;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class AdminHomeController extends BaseHomeController {
    @FXML
    private ListView<Auction> allAuctionsList;
    @FXML
    private VBox auctionDetailPane;
    @FXML
    private ListView<User> allUsersList;
    @FXML
    private ListView<AdminActionLog> adminActionLogsList;
    @FXML
    private ListView<Map<String, String>> depositRequestsList;
    @FXML
    private ListView<Map<String, String>> withdrawRequestsList;
    @FXML
    private ComboBox<String> usernameField;
    @FXML
    private TextArea userStatusArea;
    private List<String> allUsernames = new ArrayList<>();

    @FXML
    public void initialize() {
        setupHome();

        setupAuctionListCell();
        setupUserListCell();
        setupDepositRequestListCell();
        setupWithdrawRequestListCell();

        Platform.runLater(() -> {
            refreshAuctions();
            refreshUsers();
            refreshAdminActionLogs();
            refreshWalletRequests();
        });

        // Tìm kiếm gợi ý (Autocomplete)
        usernameField.getEditor().textProperty().addListener((obs, oldText, newText) -> {
            if (newText == null || newText.isEmpty()) {
                usernameField.setItems(javafx.collections.FXCollections.observableArrayList(allUsernames));
            } else {
                List<String> filteredList = allUsernames.stream()
                        .filter(s -> s.toLowerCase().contains(newText.toLowerCase())).collect(Collectors.toList());
                usernameField.setItems(javafx.collections.FXCollections.observableArrayList(filteredList));
                usernameField.show();
            }
        });
    }

    @Override
    protected void onSocketMessage(Response res) {
        if ("AUCTION_CREATED".equals(res.getStatus()) ||
                "AUCTION_UPDATED".equals(res.getStatus()) ||
                "UPDATE_PRICE".equals(res.getStatus())) {
            System.out.println("[ADMIN] Received auction update from server, refreshing auction list...");
            this.refreshAuctions();
        }
    }

    private void showAuctionDetails(Auction auction) {
        AuctionDetailViewBuilder.populateBasicDetails(auctionDetailPane, auction, () -> {
        });

        Button terminateButton = new Button("Terminate Auction");
        terminateButton.getStyleClass().add("dashboard-btn-logout");
        terminateButton.setOnAction(e -> handleTerminateAuction(auction));

        auctionDetailPane.getChildren().add(terminateButton);
    }

    private void handleTerminateAuction(Auction auction) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirm Termination");
        alert.setHeaderText("Are you sure you want to terminate this auction?");
        alert.setContentText("This action cannot be undone.");

        shared.utils.DialogHelper.applyCustomStyle(alert);

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            terminateAuctionOnServer(auction);
        }
    }

    private void terminateAuctionOnServer(Auction auction) {
        try {
            Map<String, String> data = new HashMap<>();
            data.put("auctionId", auction.getId());
            data.put("username", ctx.getCurrentUser().getUsername());
            Request req = new Request("TERMINATE_AUCTION", data);
            Response response = ctx.sendRequestAndWait(req, 20);

            if ("SUCCESS".equals(response.getStatus())) {
                alertService.showAlert("Success", "Auction terminated successfully.", welcomeLabel);
                refreshAuctions();
                auctionDetailPane.setVisible(false);
                auctionDetailPane.setManaged(false);
            } else {
                alertService.showAlert("Error", "Failed to terminate auction: " + response.getMessage(), welcomeLabel);
            }

        } catch (Exception e) {
            alertService.showAlert("Error", "An error occurred while terminating the auction.", welcomeLabel);
        }
    }

    private void setupAuctionListCell() {
        allAuctionsList.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(Auction item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.getItem().getName() + " | ID: " + item.getId() +
                            " | Seller: " + item.getSeller().getUsername() + " | Price: $" + item.getCurrentPrice() +
                            " | Status: " + item.getStatus() + " | Start: " + formatTime(item.getStartTime()) +
                            " | End: " + formatTime(item.getEndTime()));
                }
            }
        });

        allAuctionsList.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                showAuctionDetails(newSelection);
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

        allUsersList.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                usernameField.setValue(newSelection.getUsername());
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
                    setText("Deposit | " + item.get("username") + " | $" + item.get("amount") + " | Bank: "
                            + item.get("bankName") + " | Acc: " + item.get("accountNumber") + " | "
                            + item.get("createdAt"));
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
                    setText("Withdraw | " + item.get("username") + " | $" + item.get("amount") + " | Bank: "
                            + item.get("bankName") + " | Acc: " + item.get("accountNumber") + " | "
                            + item.get("createdAt"));
                }
            }
        });
    }

    @FXML
    public void refreshAdminActionLogs() {
        if (adminActionLogsList == null) {
            return;
        }
        Task<AdminActionLog[]> task = new Task<>() {
            @Override
            protected AdminActionLog[] call() throws Exception {
                Request req = new Request("GET_ADMIN_ACTION_LOGS", new HashMap<>());
                Response response = ctx.sendRequestAndWait(req, 20);
                if ("SUCCESS".equals(response.getStatus())) {
                    return gson.fromJson(response.getMessage(), AdminActionLog[].class);
                }
                return new AdminActionLog[0];
            }
        };

        task.setOnSucceeded(e -> {
            adminActionLogsList.getItems().setAll(task.getValue());
        });

        task.setOnFailed(e -> {
            System.err.println("Failed to load admin action logs: " + task.getException().getMessage());
        });

        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }

    @FXML
    public void refreshUsers() {
        System.out.println("Refreshing users...");
        Task<User[]> task = new Task<>() {
            @Override
            protected User[] call() throws Exception {
                Request req = new Request("GET_ALL_USERS", new HashMap<>());
                Response response = ctx.sendRequestAndWait(req, 20);
                if (!"SUCCESS".equals(response.getStatus())) {
                    return new User[0];
                }
                return gson.fromJson(response.getMessage(), User[].class);
            }
        };

        task.setOnSucceeded(e -> {
            User[] users = task.getValue();
            allUsersList.getItems().setAll(users);
            List<String> usernames = java.util.Arrays.stream(users).map(User::getUsername).collect(Collectors.toList());
            allUsernames.clear();
            allUsernames.addAll(usernames);
            usernameField.setItems(javafx.collections.FXCollections.observableArrayList(allUsernames));
            allUsersList.refresh();
        });

        task.setOnFailed(e -> {
            alertService.showAlert("Error", "Failed to load users", welcomeLabel);
        });

        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }

    @FXML
    public void refreshAuctions() {

        Task<List<Auction>> task = new Task<>() {
            @Override
            protected List<Auction> call() throws Exception {
                Request req = new Request("GET_AUCTIONS", new HashMap<>());
                Response response = ctx.sendRequestAndWait(req, 20);
                if (!"SUCCESS".equals(response.getStatus())) {
                    return List.of();
                }
                Auction[] auctions = gson.fromJson(response.getMessage(), Auction[].class);
                return java.util.Arrays.stream(auctions).collect(Collectors.toList());
            }

        };

        task.setOnSucceeded(e -> {
            allAuctionsList.getItems().setAll(task.getValue());
        });

        task.setOnFailed(e -> {
            alertService.showAlert("Error", "Failed to load auctions", welcomeLabel);
        });

        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }

    @FXML
    public void refreshWalletRequests() {
        refreshDepositRequests();
        refreshWithdrawRequests();
    }

    @FXML
    public void refreshDepositRequests() {
        Task<List<Map<String, String>>> task = new Task<>() {
            @Override
            protected List<Map<String, String>> call() throws Exception {
                Response response = ctx.sendRequestAndWait(new Request("GET_PENDING_DEPOSIT_REQUESTS", new HashMap<>()),
                        15);
                if ("SUCCESS".equals(response.getStatus())) {
                    Type listType = new TypeToken<List<Map<String, String>>>() {
                    }.getType();
                    return gson.fromJson(response.getMessage(), listType);
                }
                return List.of();
            }
        };

        task.setOnSucceeded(e -> {
            depositRequestsList.getItems().setAll(task.getValue());
        });

        task.setOnFailed(e -> {
            System.err.println("Failed to load deposit requests: " + task.getException().getMessage());
        });

        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }

    @FXML
    public void refreshWithdrawRequests() {
        Task<List<Map<String, String>>> task = new Task<>() {
            @Override
            protected List<Map<String, String>> call() throws Exception {
                Response response = ctx
                        .sendRequestAndWait(new Request("GET_PENDING_WITHDRAW_REQUESTS", new HashMap<>()), 15);
                if ("SUCCESS".equals(response.getStatus())) {
                    Type listType = new TypeToken<List<Map<String, String>>>() {
                    }.getType();
                    return gson.fromJson(response.getMessage(), listType);
                }
                return List.of();
            }
        };

        task.setOnSucceeded(e -> {
            withdrawRequestsList.getItems().setAll(task.getValue());
        });

        task.setOnFailed(e -> {
            System.err.println("Failed to load withdraw requests: " + task.getException().getMessage());
        });

        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }

    @FXML
    public void handleBanUser() {
        String username = usernameField.getEditor().getText() != null ? usernameField.getEditor().getText().trim() : "";
        if (username.isEmpty()) {
            alertService.showAlert("Error", "Please enter a username", welcomeLabel);
            return;
        }

        javafx.concurrent.Task<Response> task = new javafx.concurrent.Task<>() {
            @Override
            protected Response call() throws Exception {
                Map<String, String> data = new HashMap<>();
                data.put("username", username);
                Request req = new Request("BAN_USER", data);
                return ctx.sendRequestAndWait(req, 20);
            }
        };

        task.setOnSucceeded(e -> {
            Response response = task.getValue();
            if (response != null && "SUCCESS".equals(response.getStatus())) {
                alertService.showAlert("Success", "User " + username + " has been banned!", welcomeLabel);
                userStatusArea.appendText("\nBanned user: " + username);
                usernameField.setValue(null);
                usernameField.getEditor().clear();
                refreshUsers();
            } else {
                String errorMsg = response != null ? response.getMessage() : "Unknown error";
                alertService.showAlert("Error", errorMsg, welcomeLabel);
            }
        });

        task.setOnFailed(e -> {
            alertService.showAlert("Error", "Failed to ban user: " + task.getException().getMessage(), welcomeLabel);
        });

        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }

    @FXML
    public void handleUnbanUser() {
        String username = usernameField.getEditor().getText() != null ? usernameField.getEditor().getText().trim() : "";
        if (username.isEmpty()) {
            alertService.showAlert("Error", "Please enter a username", welcomeLabel);
            return;
        }

        javafx.concurrent.Task<Response> task = new javafx.concurrent.Task<>() {
            @Override
            protected Response call() throws Exception {
                Map<String, String> data = new HashMap<>();
                data.put("username", username);
                Request req = new Request("UNBAN_USER", data);
                return ctx.sendRequestAndWait(req, 20);
            }
        };

        task.setOnSucceeded(e -> {
            Response response = task.getValue();
            if (response != null && "SUCCESS".equals(response.getStatus())) {
                alertService.showAlert("Success", "User " + username + " has been unbanned!", welcomeLabel);
                userStatusArea.appendText("\nUnbanned user: " + username);
                usernameField.setValue(null);
                usernameField.getEditor().clear();
                refreshUsers();
            } else {
                String errorMsg = response != null ? response.getMessage() : "Unknown error";
                alertService.showAlert("Error", errorMsg, welcomeLabel);
            }
        });

        task.setOnFailed(e -> {
            alertService.showAlert("Error", "Failed to unban user: " + task.getException().getMessage(), welcomeLabel);
        });

        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
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
        ChangePasswordSupport.showDialog(ctx, welcomeLabel);
    }

    private void processWalletRequest(Map<String, String> requestItem, String action) {
        if (requestItem == null) {
            alertService.showAlert("Error", "Please select a request first", welcomeLabel);
            return;
        }

        javafx.concurrent.Task<Response> task = new javafx.concurrent.Task<>() {
            @Override
            protected Response call() throws Exception {
                Map<String, String> data = new HashMap<>();
                data.put("requestId", requestItem.get("id"));
                return ctx.sendRequestAndWait(new Request(action, data), 15);
            }
        };

        task.setOnSucceeded(e -> {
            Response response = task.getValue();
            if (response != null && "SUCCESS".equals(response.getStatus())) {
                alertService.showAlert("Success", response.getMessage(), welcomeLabel);
                refreshWalletRequests();
            } else {
                String errorMsg = response != null ? response.getMessage() : "Unknown error";
                alertService.showAlert("Error", errorMsg, welcomeLabel);
            }
        });

        task.setOnFailed(e -> {
            alertService.showAlert("Error", "Failed to process request: " + task.getException().getMessage(), welcomeLabel);
        });

        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }
}