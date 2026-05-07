package client;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.concurrent.Task;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import shared.models.AdminActionLog;
import shared.models.Auction;
import shared.models.User;
import shared.network.Request;
import shared.network.Response;
import shared.utils.GsonUtils;
import shared.models.Item;
import shared.models.Electronic;
import shared.models.Vehicle;
import shared.models.Art;
import shared.models.Fashion;
import shared.models.Collectible;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class AdminHomeController {
    @FXML private ListView<Auction> allAuctionsList;
    @FXML private VBox auctionDetailPane;
    @FXML private ListView<User> allUsersList;
    @FXML private ListView<AdminActionLog> adminActionLogsList;
    @FXML private ListView<Map<String, String>> depositRequestsList;
    @FXML private ListView<Map<String, String>> withdrawRequestsList;
    @FXML private ComboBox<String> usernameField;
    @FXML private TextArea userStatusArea;
    @FXML private Label welcomeLabel;

    private final AppContext ctx = AppContext.getInstance();
    private final Gson gson = GsonUtils.createGson();
    private final IAlertService alertService = new AlertServiceImpl();
    private List<String> allUsernames = new ArrayList<>();
    private Consumer<String> messageListener;

    @FXML
    public void initialize() {
        welcomeLabel.setText("Welcome " + ctx.getCurrentUser().getUsername());
        
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
                        .filter(s -> s.toLowerCase().contains(newText.toLowerCase()))
                        .collect(Collectors.toList());
                usernameField.setItems(javafx.collections.FXCollections.observableArrayList(filteredList));
                usernameField.show();
            }
        });

        // Đăng ký lắng nghe sự kiện từ Server
        messageListener = line -> {
            try {
                Response res = gson.fromJson(line, Response.class);
                if ("AUCTION_CREATED".equals(res.getStatus()) || "AUCTION_UPDATED".equals(res.getStatus()) || "UPDATE_PRICE".equals(res.getStatus())) {
                    System.out.println("[ADMIN] Received auction update from server, refreshing auction list...");
                    this.refreshAuctions();
                }
            } catch (Exception e) {
                // Bỏ qua các tin nhắn không hợp lệ
            }
        };
        ctx.addMessageListener(messageListener);
    }

    private void showAuctionDetails(Auction auction) {
        auctionDetailPane.getChildren().clear();
        auctionDetailPane.setVisible(true);
        auctionDetailPane.setManaged(true);

        // Tiêu đề và nút đóng
        Label titleLabel = new Label("Auction Details");
        titleLabel.getStyleClass().add("dashboard-section-title");
        Button closeButton = new Button("X");
        closeButton.getStyleClass().add("dashboard-btn-ghost");
        closeButton.setOnAction(e -> {
            auctionDetailPane.setVisible(false);
            auctionDetailPane.setManaged(false);
        });
        HBox titleBox = new HBox(10, titleLabel, closeButton);
        HBox.setHgrow(titleLabel, Priority.ALWAYS);

        // Ảnh
        ImageView imageView = new ImageView();
        if (auction.getItem().getImageUrl() != null && !auction.getItem().getImageUrl().isEmpty()) {
            imageView.setImage(new Image(auction.getItem().getImageUrl(), 200, 200, true, true));
        }

        // Thông tin chi tiết
        GridPane detailsGrid = new GridPane();
        detailsGrid.setHgap(10);
        detailsGrid.setVgap(8);
        detailsGrid.getStyleClass().add("details-grid");

        int rowIndex = 0;
        detailsGrid.add(new Label("Name:"), 0, rowIndex);
        detailsGrid.add(new Label(auction.getItem().getName()), 1, rowIndex++);
        detailsGrid.add(new Label("Description:"), 0, rowIndex);
        Label descLabel = new Label(auction.getItem().getDescription());
        descLabel.setWrapText(true);
        detailsGrid.add(descLabel, 1, rowIndex++);
        detailsGrid.add(new Label("Base Price:"), 0, rowIndex);
        detailsGrid.add(new Label("$" + auction.getItem().getStartingPrice()), 1, rowIndex++);
        detailsGrid.add(new Label("Current Price:"), 0, rowIndex);
        detailsGrid.add(new Label("$" + auction.getCurrentPrice()), 1, rowIndex++);
        detailsGrid.add(new Label("Start Time:"), 0, rowIndex);
        detailsGrid.add(new Label(formatTime(auction.getStartTime())), 1, rowIndex++);
        detailsGrid.add(new Label("End Time:"), 0, rowIndex);
        detailsGrid.add(new Label(formatTime(auction.getEndTime())), 1, rowIndex++);
        detailsGrid.add(new Label("Status:"), 0, rowIndex);
        detailsGrid.add(new Label(auction.getStatus().toString()), 1, rowIndex++);
        detailsGrid.add(new Label("Seller:"), 0, rowIndex);
        detailsGrid.add(new Label(auction.getSeller().getUsername()), 1, rowIndex++);

        Item item = auction.getItem();
        if (item instanceof Electronic) {
            Electronic electronic = (Electronic) item;
            detailsGrid.add(new Label("Brand:"), 0, rowIndex);
            detailsGrid.add(new Label(electronic.getBrand()), 1, rowIndex++);
            detailsGrid.add(new Label("Item Status:"), 0, rowIndex);
            detailsGrid.add(new Label(electronic.getStatus().toString()), 1, rowIndex++);
        } else if (item instanceof Vehicle) {
            Vehicle vehicle = (Vehicle) item;
            detailsGrid.add(new Label("Brand:"), 0, rowIndex);
            detailsGrid.add(new Label(vehicle.getBrand()), 1, rowIndex++);
            detailsGrid.add(new Label("Model Year:"), 0, rowIndex);
            detailsGrid.add(new Label(String.valueOf(vehicle.getModel())), 1, rowIndex++);
            detailsGrid.add(new Label("KM Traveled:"), 0, rowIndex);
            detailsGrid.add(new Label(String.valueOf(vehicle.getKMTravel())), 1, rowIndex++);
        } else if (item instanceof Art) {
            Art art = (Art) item;
            detailsGrid.add(new Label("Artist:"), 0, rowIndex);
            detailsGrid.add(new Label(art.getArtist()), 1, rowIndex++);
            detailsGrid.add(new Label("Year Created:"), 0, rowIndex);
            detailsGrid.add(new Label(String.valueOf(art.getYearCreated())), 1, rowIndex++);
            detailsGrid.add(new Label("Original:"), 0, rowIndex);
            detailsGrid.add(new Label(art.getIsOriginal() ? "Yes" : "No"), 1, rowIndex++);
        } else if (item instanceof Fashion) {
            Fashion fashion = (Fashion) item;
            detailsGrid.add(new Label("Brand:"), 0, rowIndex);
            detailsGrid.add(new Label(fashion.getBrand()), 1, rowIndex++);
            detailsGrid.add(new Label("Item Status:"), 0, rowIndex);
            detailsGrid.add(new Label(fashion.getStatus().toString()), 1, rowIndex++);
        } else if (item instanceof Collectible) {
            Collectible collectible = (Collectible) item;
            detailsGrid.add(new Label("Year Created:"), 0, rowIndex);
            detailsGrid.add(new Label(String.valueOf(collectible.getYearCreated())), 1, rowIndex++);
        }

        Button terminateButton = new Button("Terminate Auction");
        terminateButton.getStyleClass().add("dashboard-btn-logout");
        terminateButton.setOnAction(e -> handleTerminateAuction(auction));

        auctionDetailPane.getChildren().addAll(titleBox, imageView, detailsGrid, terminateButton);
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
            Response response = ctx.sendRequestAndWait(req, 15);

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
                    setText(
                            item.getItem().getName() +
                            " | ID: " + item.getId() +
                            " | Seller: " + item.getSeller().getUsername() +
                            " | Price: $" + item.getCurrentPrice() +
                            " | Status: " + item.getStatus() +
                            " | Start: " + formatTime(item.getStartTime()) +
                            " | End: " + formatTime(item.getEndTime())
                    );
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
    }

    private void setupDepositRequestListCell() {
        depositRequestsList.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(Map<String, String> item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText("Deposit | " + item.get("username") + " | $" + item.get("amount") + " | Bank: " + item.get("bankName") + " | Acc: " + item.get("accountNumber") + " | " + item.get("createdAt"));
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
                    setText("Withdraw | " + item.get("username") + " | $" + item.get("amount") + " | Bank: " + item.get("bankName") + " | Acc: " + item.get("accountNumber") + " | " + item.get("createdAt"));
                }
            }
        });
    }

    @FXML
    public void refreshAdminActionLogs() {
        try {
            Request req = new Request("GET_ADMIN_ACTION_LOGS", new HashMap<>());
            Response response = ctx.sendRequestAndWait(req, 15);
            if ("SUCCESS".equals(response.getStatus())) {
                AdminActionLog[] logs = gson.fromJson(response.getMessage(), AdminActionLog[].class);
                Platform.runLater(() -> adminActionLogsList.getItems().setAll(logs));
            }
        } catch (Exception e) {
            alertService.showAlert("Error", "Failed to load admin action logs: " + e.getMessage(), welcomeLabel);
        }
    }

    @FXML
    public void refreshUsers() {

        Task<User[]> task = new Task<>() {

            @Override
            protected User[] call() throws Exception {

                Request req =
                        new Request(
                                "GET_ALL_USERS",
                                new HashMap<>()
                        );

                Response response =
                        ctx.sendRequestAndWait(req, 15);

                if (!"SUCCESS".equals(response.getStatus())) {
                    return new User[0];
                }

                return gson.fromJson(
                        response.getMessage(),
                        User[].class
                );
            }
        };

        task.setOnSucceeded(e -> {

            User[] users = task.getValue();

            allUsersList.getItems().setAll(users);

            List<String> usernames =
                    java.util.Arrays.stream(users)
                            .map(User::getUsername)
                            .collect(Collectors.toList());

            allUsernames.clear();

            allUsernames.addAll(usernames);

            usernameField.setItems(
                    javafx.collections.FXCollections
                            .observableArrayList(allUsernames)
            );

            allUsersList.refresh();
        });

        task.setOnFailed(e -> {

            alertService.showAlert(
                    "Error",
                    "Failed to load users",
                    welcomeLabel
            );
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

                Request req =
                        new Request(
                                "GET_AUCTIONS",
                                new HashMap<>()
                        );

                Response response =
                        ctx.sendRequestAndWait(req, 15);

                if (!"SUCCESS".equals(response.getStatus())) {
                    return List.of();
                }

                Auction[] auctions =
                        gson.fromJson(
                                response.getMessage(),
                                Auction[].class
                        );

                return java.util.Arrays.stream(auctions)
                        .filter(a ->
                                a.getStatus()
                                        != shared.enums.AuctionStatus.CANCELED
                        )
                        .collect(Collectors.toList());
            }
        };

        task.setOnSucceeded(e -> {

            allAuctionsList.getItems()
                    .setAll(task.getValue());
        });

        task.setOnFailed(e -> {

            alertService.showAlert(
                    "Error",
                    "Failed to load auctions",
                    welcomeLabel
            );
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
        try {
            Response response = ctx.sendRequestAndWait(new Request("GET_PENDING_DEPOSIT_REQUESTS", new HashMap<>()), 15);
            if ("SUCCESS".equals(response.getStatus())) {
                Type listType = new TypeToken<List<Map<String, String>>>() {}.getType();
                List<Map<String, String>> requests = gson.fromJson(response.getMessage(), listType);
                Platform.runLater(() -> depositRequestsList.getItems().setAll(requests));
            }
        } catch (Exception e) {
            alertService.showAlert("Error", "Failed to load deposit requests: " + e.getMessage(), welcomeLabel);
        }
    }

    @FXML
    public void refreshWithdrawRequests() {
        try {
            Response response = ctx.sendRequestAndWait(new Request("GET_PENDING_WITHDRAW_REQUESTS", new HashMap<>()), 15);
            if ("SUCCESS".equals(response.getStatus())) {
                Type listType = new TypeToken<List<Map<String, String>>>() {}.getType();
                List<Map<String, String>> requests = gson.fromJson(response.getMessage(), listType);
                Platform.runLater(() -> withdrawRequestsList.getItems().setAll(requests));
            }
        } catch (Exception e) {
            alertService.showAlert("Error", "Failed to load withdraw requests: " + e.getMessage(), welcomeLabel);
        }
    }

    @FXML
    public void handleBanUser() {
        String username = usernameField.getEditor().getText() != null ? usernameField.getEditor().getText().trim() : "";
        if (username.isEmpty()) {
            alertService.showAlert("Error", "Please enter a username", welcomeLabel);
            return;
        }

        try {
            Map<String, String> data = new HashMap<>();
            data.put("username", username);
            Request req = new Request("BAN_USER", data);
            Response response = ctx.sendRequestAndWait(req, 15);

            if ("SUCCESS".equals(response.getStatus())) {
                alertService.showAlert("Success", "User " + username + " has been banned!", welcomeLabel);
                userStatusArea.appendText("\nBanned user: " + username);
                usernameField.setValue(null);
                usernameField.getEditor().clear();
                refreshUsers();
            } else {
                alertService.showAlert("Error", response.getMessage(), welcomeLabel);
            }
        } catch (Exception e) {
            alertService.showAlert("Error", "Failed to ban user: " + e.getMessage(), welcomeLabel);
        }
    }

    @FXML
    public void handleUnbanUser() {
        String username = usernameField.getEditor().getText() != null ? usernameField.getEditor().getText().trim() : "";
        if (username.isEmpty()) {
            alertService.showAlert("Error", "Please enter a username", welcomeLabel);
            return;
        }

        try {
            Map<String, String> data = new HashMap<>();
            data.put("username", username);
            Request req = new Request("UNBAN_USER", data);
            Response response = ctx.sendRequestAndWait(req, 15);

            if ("SUCCESS".equals(response.getStatus())) {
                alertService.showAlert("Success", "User " + username + " has been unbanned!", welcomeLabel);
                userStatusArea.appendText("\nUnbanned user: " + username);
                usernameField.setValue(null);
                usernameField.getEditor().clear();
                refreshUsers();
            } else {
                alertService.showAlert("Error", response.getMessage(), welcomeLabel);
            }
        } catch (Exception e) {
            alertService.showAlert("Error", "Failed to unban user: " + e.getMessage(), welcomeLabel);
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
        ChangePasswordSupport.showDialog(ctx, welcomeLabel);
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
        Navigator.switchSceneStatic("login.fxml");
    }

    private void processWalletRequest(Map<String, String> requestItem, String action) {
        if (requestItem == null) {
            alertService.showAlert("Error", "Please select a request first", welcomeLabel);
            return;
        }

        try {
            Map<String, String> data = new HashMap<>();
            data.put("requestId", requestItem.get("id"));
            Response response = ctx.sendRequestAndWait(new Request(action, data), 15);

            if ("SUCCESS".equals(response.getStatus())) {
                alertService.showAlert("Success", response.getMessage(), welcomeLabel);
                refreshWalletRequests();
            } else {
                alertService.showAlert("Error", response.getMessage(), welcomeLabel);
            }
        } catch (Exception e) {
            alertService.showAlert("Error", "Failed to process request: " + e.getMessage(), welcomeLabel);
        }
    }

    private String formatTime(java.time.Instant instant) {
        return shared.utils.FormatUtils.formatTime(instant);
    }
}
