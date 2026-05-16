package client.controller;

import com.google.gson.reflect.TypeToken;

import client.support.AuctionDetailViewBuilder;
import client.support.ChangePasswordSupport;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import java.util.Comparator;
import javafx.concurrent.Task;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import java.lang.reflect.Type;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import shared.models.Auction;
import shared.network.Request;
import shared.network.Response;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import shared.enums.BankList;

public class BidderHomeController extends BaseHomeController {

    @Override
    protected void refreshData() {
        loadAuction();
    }

    @FXML
    private TilePane auctionGrid;
    @FXML
    private Label walletBalanceLabel;
    @FXML
    private TextField searchField;
    @FXML
    private ComboBox<String> sortComboBox;

    private final ObservableList<Auction> allAuctions = FXCollections.observableArrayList();
    private AuctionChartController activeChartController;
    private Auction activeChartAuction;

    @FXML
    public void initialize() {

        setupHome();

        // Setup filter
        sortComboBox.getItems().addAll("Name (A-Z)", "Price (Low to High)");
        FilteredList<Auction> filteredData = new FilteredList<>(allAuctions, p -> true);
        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            filteredData.setPredicate(auction -> {
                if (newValue == null || newValue.isEmpty()) {
                    return true;
                }
                return auction.getItem().getName().toLowerCase().contains(newValue.toLowerCase());
            });
        });

        SortedList<Auction> sortedData = new SortedList<>(filteredData);

        sortComboBox.valueProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue == null) {
                sortedData.setComparator(null);
                return;
            }
            switch (newValue) {
                case "Name (A-Z)":
                    sortedData.setComparator(Comparator.comparing(a -> a.getItem().getName()));
                    break;
                case "Price (Low to High)":
                    sortedData.setComparator(Comparator.comparing(Auction::getCurrentPrice));
                    break;
            }
        });

        sortedData.addListener((ListChangeListener<Auction>) c -> updateAuctionGrid(sortedData));

        Platform.runLater(() -> {
            loadWallet();
            loadAuction();
        });

        // Tự động làm mới giao diện và ví mỗi 2 giây
        javafx.animation.Timeline autoRefresh = new javafx.animation.Timeline(
            new javafx.animation.KeyFrame(javafx.util.Duration.seconds(2), e -> {
                handleRefresh();
            })
        );
        autoRefresh.setCycleCount(javafx.animation.Timeline.INDEFINITE);
        autoRefresh.play();
    }

    private void loadAuction() {
        Task<List<Auction>> task = new Task<>() {
            @Override
            protected List<Auction> call() throws Exception {
                Map<String, String> data = new HashMap<>();
                Response response = ctx.sendRequestAndWait(new Request("GET_AUCTIONS", data), 20);
                Type type = new TypeToken<List<Auction>>() {
                }.getType();
                return gson.fromJson(response.getMessage(), type);
            }

        };

        task.setOnSucceeded(e -> {
            List<Auction> list = task.getValue();
            if (list != null) {
                allAuctions.setAll(list);
                if (selectedAuction != null) {
                    Auction updated = list.stream().filter(a -> a.getId().equals(selectedAuction.getId())).findFirst().orElse(null);
                    if (updated != null) {
                        showAuctionDetails(updated);
                    }
                }
                if (activeChartController != null && activeChartAuction != null) {
                    Auction updatedChartAuction = list.stream()
                            .filter(a -> a.getId().equals(activeChartAuction.getId())).findFirst().orElse(null);
                    if (updatedChartAuction != null) {
                        activeChartAuction = updatedChartAuction;
                        activeChartController.populateChart(updatedChartAuction);
                    }
                }
            }
        });

        task.setOnFailed(e -> {
            task.getException().printStackTrace();
            alertService.showAlert("Error", "Cannot load auctions", auctionGrid);
        });

        new Thread(task).start();
    }

    private void loadWallet() {
        Task<String> task = new Task<>() {
            @Override
            protected String call() throws Exception {
                Map<String, String> data = new HashMap<>();
                data.put("username", ctx.getCurrentUser().getUsername());
                Response response = ctx.sendRequestAndWait(new Request("GET_WALLET_BALANCE", data), 20);
                if ("SUCCESS".equals(response.getStatus())) {
                    return response.getMessage();
                }
                return null;
            }
        };

        task.setOnSucceeded(e -> {
            String balance = task.getValue();
            if (balance != null) {
                walletBalanceLabel.setText("Balance: $" + balance);
            } else {
                walletBalanceLabel.setText("Balance unavailable");
            }
        });

        task.setOnFailed(e -> {
            walletBalanceLabel.setText("Balance unavailable");
        });

        new Thread(task).start();
    }

    @FXML
    public void handleRefresh() {
        loadAuction();
        loadWallet();
    }

    @Override
    protected TilePane getAuctionGrid() {
        return auctionGrid;
    }

    @Override
    protected void onAuctionCardDoubleClicked(Auction auction) {
        // Lấy dữ liệu mới nhất từ danh sách hiện tại
        Auction latestAuction = allAuctions.stream()
                .filter(a -> a.getId().equals(auction.getId()))
                .findFirst()
                .orElse(auction);
        showAuctionDetails(latestAuction);
    }

    private void showAuctionDetails(Auction auction) {
        this.selectedAuction = auction;

        Button chartButton = new Button("View Chart");
        chartButton.getStyleClass().add("dashboard-btn-ghost");
        chartButton.setOnAction(e -> showPriceChart(auction));

        if ("RUNNING".equals(auction.getStatus().toString())) {
            Button bidButton = new Button("Place Bid");
            bidButton.getStyleClass().add("dashboard-btn-primary");
            bidButton.setOnAction(e -> showBidAmountDialog(auction, false));

            Button autoBidButton = new Button("Auto Bid");
            autoBidButton.getStyleClass().add("dashboard-btn-ghost");
            autoBidButton.setOnAction(e -> showBidAmountDialog(auction, true));

            AuctionDetailViewBuilder.populateFullDetails(auctionDetailPane, auction, () -> {
                this.selectedAuction = null;
            }, chartButton, bidButton, autoBidButton);
        } else {
            AuctionDetailViewBuilder.populateFullDetails(auctionDetailPane, auction, () -> {
                this.selectedAuction = null;
            }, chartButton);
        }
    }

    @FXML
    public void handleChangePassword() {
        ChangePasswordSupport.showDialog(ctx, welcomeLabel);
    }

    @FXML
    public void handleDepositRequest() {
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Deposit Request");
        dialog.setHeaderText("Send a deposit request to admin");

        TextField amountField = new TextField();
        amountField.setPromptText("Enter amount");
        amountField.getStyleClass().add("dashboard-input");

        ComboBox<BankList> bankNameComboBox = new ComboBox<>();
        bankNameComboBox.setPromptText("Select bank name");
        bankNameComboBox.setEditable(true);
        bankNameComboBox.getStyleClass().add("dashboard-choicebox");
        ObservableList<BankList> bankOptions = FXCollections.observableArrayList(BankList.values());
        bankNameComboBox.setItems(bankOptions);

        // Autocomplete filter cho ComboBox
        bankNameComboBox.getEditor().textProperty().addListener((obs, oldText, newText) -> {
            if (newText == null || newText.isEmpty()) {
                bankNameComboBox.setItems(bankOptions);
            } else {
                List<BankList> filteredList = Arrays.stream(BankList.values())
                        .filter(s -> s.name().toLowerCase().contains(newText.toLowerCase()))
                        .collect(Collectors.toList());
                bankNameComboBox.setItems(FXCollections.observableArrayList(filteredList));
                bankNameComboBox.show();
            }

        });

        TextField accountNumberField = new TextField();
        accountNumberField.setPromptText("Enter account number");
        accountNumberField.getStyleClass().add("dashboard-input");

        VBox content = new VBox(15);
        content.getChildren().addAll(new Label("Amount"), amountField, new Label("Bank Name"), bankNameComboBox,
                new Label("Account Number"), accountNumberField);
        content.setStyle("-fx-padding: 20px;");

        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        shared.utils.DialogHelper.applyCustomStyle(dialog);

        // Convert kết quả thành chuỗi
        dialog.setResultConverter(buttonType -> {
            if (buttonType == ButtonType.OK) {
                Object value = bankNameComboBox.getValue();
                String selectedBank = "";
                if (value instanceof BankList) {
                    selectedBank = ((BankList) value).name();
                } else if (value != null) {
                    selectedBank = value.toString();
                }
                return amountField.getText() + "," + selectedBank + "," + accountNumberField.getText();
            }
            return null;
        });

        dialog.showAndWait().ifPresent(result -> {
            if (result == null || result.isEmpty())
                return;

            String[] parts = result.split(",", 3);
            if (parts.length < 3 || parts[0].isEmpty() || parts[1] == null || parts[1].trim().isEmpty()
                    || parts[2].isEmpty()) {
                alertService.showAlert("Error", "Please fill in all fields.", welcomeLabel);
                return;
            }

            try {
                BigDecimal amount = new BigDecimal(parts[0]);
                String bankName = parts[1].trim();
                String accountNumber = parts[2].trim();

                if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                    alertService.showAlert("Error", "Amount must be greater than 0.", welcomeLabel);
                    return;
                }

                Map<String, String> data = new HashMap<>();
                data.put("username", ctx.getCurrentUser().getUsername());
                data.put("amount", amount.toPlainString());
                data.put("bankName", bankName);
                data.put("accountNumber", accountNumber);

                Response response = ctx.sendRequestAndWait(new Request("CREATE_DEPOSIT_REQUEST", data), 15);

                if ("SUCCESS".equals(response.getStatus())) {
                    alertService.showAlert("Success", response.getMessage(), welcomeLabel);
                } else {
                    alertService.showAlert("Error", response.getMessage(), welcomeLabel);
                }

            } catch (NumberFormatException e) {
                alertService.showAlert("Error", "Please enter a valid amount.", welcomeLabel);
            } catch (Exception e) {
                alertService.showAlert("Error", "An unexpected error occurred: " + e.getMessage(), welcomeLabel);
                e.printStackTrace();
            }
        });
    }

    @FXML
    public void handleWithdrawRequest() {
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Withdraw Request");
        dialog.setHeaderText("Send a withdraw request to admin");

        TextField amountField = new TextField();
        amountField.setPromptText("Enter amount");
        amountField.getStyleClass().add("dashboard-input");

        ComboBox<BankList> bankNameComboBox = new ComboBox<>();
        bankNameComboBox.setPromptText("Select bank name");
        bankNameComboBox.setEditable(true);
        bankNameComboBox.getStyleClass().add("dashboard-choicebox");
        ObservableList<BankList> bankOptions = FXCollections.observableArrayList(BankList.values());
        bankNameComboBox.setItems(bankOptions);

        bankNameComboBox.getEditor().textProperty().addListener((obs, oldText, newText) -> {
            if (newText == null || newText.isEmpty()) {
                bankNameComboBox.setItems(bankOptions);
            } else {
                List<BankList> filteredList = Arrays.stream(BankList.values())
                        .filter(s -> s.name().toLowerCase().contains(newText.toLowerCase()))
                        .collect(Collectors.toList());
                bankNameComboBox.setItems(FXCollections.observableArrayList(filteredList));
            }
        });

        TextField accountNumberField = new TextField();
        accountNumberField.setPromptText("Enter account number");
        accountNumberField.getStyleClass().add("dashboard-input");

        VBox content = new VBox(15);
        content.getChildren().addAll(new Label("Amount"), amountField, new Label("Bank Name"), bankNameComboBox,
                new Label("Account Number"), accountNumberField);
        content.setStyle("-fx-padding: 20px;");

        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        shared.utils.DialogHelper.applyCustomStyle(dialog);

        dialog.setResultConverter(buttonType -> {
            if (buttonType == ButtonType.OK) {
                Object value = bankNameComboBox.getValue();
                String selectedBank = "";
                if (value instanceof BankList) {
                    selectedBank = ((BankList) value).name();
                } else if (value != null) {
                    selectedBank = value.toString();
                }
                return amountField.getText() + "," + selectedBank + "," + accountNumberField.getText();
            }
            return null;
        });

        dialog.showAndWait().ifPresent(result -> {
            String[] parts = result.split(",");
            if (parts.length == 3) {
                try {
                    BigDecimal amount = new BigDecimal(parts[0]);
                    String bankName = parts[1];
                    String accountNumber = parts[2];
                    if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                        showAlert("Error", "Amount must be greater than 0");
                        return;
                    }

                    if (bankName.trim().isEmpty() || accountNumber.trim().isEmpty()) {
                        showAlert("Error", "Bank name and account number cannot be empty");
                        return;
                    }

                    Map<String, String> data = new HashMap<>();
                    data.put("username", ctx.getCurrentUser().getUsername());
                    data.put("amount", amount.toPlainString());
                    data.put("bankName", bankName.trim());
                    data.put("accountNumber", accountNumber.trim());
                    Response response = ctx.sendRequestAndWait(new Request("CREATE_WITHDRAW_REQUEST", data), 15);
                    if ("SUCCESS".equals(response.getStatus())) {
                        showAlert("Success", response.getMessage());
                    } else {
                        showAlert("Error", response.getMessage());
                    }

                } catch (Exception e) {
                    showAlert("Error", "Please enter valid data");
                }
            }
        });
    }

    private void showPriceChart(Auction auction) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/auction_chart.fxml"));
            Parent root = loader.load();

            AuctionChartController controller = loader.getController();
            controller.populateChart(auction);

            this.activeChartController = controller;
            this.activeChartAuction = auction;

            Stage stage = new Stage();
            stage.setTitle("Price Chart");
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setScene(new Scene(root));
            stage.setOnHidden(e -> {
                this.activeChartController = null;
                this.activeChartAuction = null;
            });
            stage.showAndWait();

        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Error", "Could not load the price chart view.");
        }
    }

    private void showBidAmountDialog(Auction auction, boolean isAutoBid) {
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle(isAutoBid ? "Auto Bid" : "Place Bid");
        dialog.setHeaderText("Enter bid amount for: " + auction.getItem().getName());

        shared.utils.DialogHelper.applyCustomStyle(dialog);

        TextField amountField = new TextField();
        amountField.setPromptText("Enter amount");
        amountField.getStyleClass().add("dashboard-input");

        BigDecimal minBid = (auction.getHighestBidder() == null) 
                            ? auction.getStartPrice() 
                            : auction.getCurrentPrice().add(auction.getItem().getMinIncrement());

        VBox content = new VBox(10);
        content.setStyle("-fx-padding: 20px;");
        content.getChildren().add(new Label("Current price: $" + auction.getCurrentPrice()));
        
        if (auction.getHighestBidder() == null) {
            content.getChildren().add(new Label("Minimum bid: $" + minBid + " (Starting Price)"));
        } else {
            content.getChildren().add(new Label("Minimum bid: $" + minBid + " (Current + $" + auction.getItem().getMinIncrement() + ")"));
        }
        
        content.getChildren().add(new Label("Enter amount:"));
        content.getChildren().add(amountField);

        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == ButtonType.OK) {
                return amountField.getText();
            }
            return null;
        });

        dialog.showAndWait().ifPresent(amount -> {
            if (amount != null && !amount.isEmpty()) {
                try {
                    BigDecimal bidAmount = new BigDecimal(amount);
                    if (bidAmount.compareTo(minBid) < 0) {
                        showAlert("Error", "Bid amount must be at least $" + minBid);
                        return;
                    }

                    if (isAutoBid) {
                        registerAutoBid(auction, amount);
                    } else {
                        placeBid(auction, amount);
                    }

                } catch (NumberFormatException e) {
                    showAlert("Error", "Please enter a valid number!");
                }
            } else {
                showAlert("Error", "Please enter a valid amount!");
            }
        });
    }

    private void placeBid(Auction auction, String amount) {
        try {
            Map<String, String> data = new HashMap<>();
            data.put("auctionId", auction.getId());
            data.put("amount", amount);
            data.put("username", ctx.getCurrentUser().getUsername());

            Request req = new Request("PLACE_BID", data);
            Response response = ctx.sendRequestAndWait(req, 15);

            if ("SUCCESS".equals(response.getStatus())) {
                loadAuction();
                showAlert("Success", "Bid placed successfully!");
            } else {
                showAlert("Error", response.getMessage());
            }
        } catch (Exception e) {
            showAlert("Error", "Failed to place bid: " + e.getMessage());
        }
    }

    private void registerAutoBid(Auction auction, String maxAmount) {

        try {
            Map<String, String> data = new HashMap<>();
            data.put("auctionId", auction.getId());
            data.put("maxBid", maxAmount);
            data.put("username", ctx.getCurrentUser().getUsername());

            Request req = new Request("REGISTER_AUTOBID", data);
            Response response = ctx.sendRequestAndWait(req, 15);

            if ("SUCCESS".equals(response.getStatus())) {
                loadAuction();
                showAlert("Success", "Auto-bid registered successfully!");
            } else {
                showAlert("Error", response.getMessage());
            }
        } catch (Exception e) {
            showAlert("Error", "Failed to register auto-bid: " + e.getMessage());
        }
    }

    private void showAlert(String title, String message) {
        alertService.showAlert(title, message, welcomeLabel.getScene().getWindow());
    }

    @Override
    protected void onSocketMessage(Response res) {
        try {
            if ("UPDATE_PRICE".equals(res.getStatus())) {
                java.lang.reflect.Type type = new com.google.gson.reflect.TypeToken<Map<String, String>>() {
                }.getType();
                Map<String, String> payload = gson.fromJson(res.getMessage(), type);
                String auctionId = payload.get("auctionId");
                String newPrice = payload.get("newPrice");
                Platform.runLater(() -> {
                    VBox card = auctionCardMap.get(auctionId);
                    if (card != null) {
                        Label priceLabel = (Label) card.getProperties().get("priceLabel");
                        if (priceLabel != null) {
                            priceLabel.setText("Current Bid: $" + newPrice);
                        }
                    }
                    if (selectedAuction != null && selectedAuction.getId().equals(auctionId)) {
                        selectedAuction.setCurrentPriceForDBRestore(new java.math.BigDecimal(newPrice));
                        showAuctionDetails(selectedAuction);
                    }
                });
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}