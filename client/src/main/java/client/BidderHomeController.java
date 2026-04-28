package client;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;
import shared.models.Auction;
import shared.network.Request;
import shared.network.Response;
import shared.utils.GsonUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class BidderHomeController {
    @FXML
    private TilePane auctionGrid;
    @FXML
    private Label welcomeLabel;
    @FXML
    private Label walletBalanceLabel;
    @FXML
    private TextField searchField;
    @FXML
    private ComboBox<String> sortComboBox;

    private final AppContext ctx = AppContext.getInstance();
    private final Gson gson = GsonUtils.createGson();
    private Consumer<String> messageListener;
    private final ObservableList<Auction> allAuctions = FXCollections.observableArrayList();

    private static final List<String> BANK_NAMES = Arrays.asList(
            "Vietcombank", "Techcombank", "BIDV", "Agribank", "VPBank",
            "MBBank", "ACB", "Sacombank", "Eximbank", "HDBank",
            "TPBank", "VIB", "SeABank", "SHB", "OCB",
            "MSB", "LienVietPostBank", "BacABank", "VietBank", "PVcomBank");

    @FXML
    public void initialize() {
        welcomeLabel.setText("Welcome " + ctx.getCurrentUser().getUsername());

        // Setup sorting options
        sortComboBox.getItems().addAll("Name (A-Z)", "Price (Low to High)");

        // 1. Wrap the ObservableList in a FilteredList (initially display all data).
        FilteredList<Auction> filteredData = new FilteredList<>(allAuctions, p -> true);

        // 2. Set the filter Predicate whenever the filter changes.
        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            filteredData.setPredicate(auction -> {
                if (newValue == null || newValue.isEmpty()) {
                    return true; // Display all auctions if search field is empty.
                }
                String lowerCaseFilter = newValue.toLowerCase();
                // Filter matches item name.
                return auction.getItem().getName().toLowerCase().contains(lowerCaseFilter);
            });
        });

        // 3. Wrap the FilteredList in a SortedList.
        SortedList<Auction> sortedData = new SortedList<>(filteredData);

        // 4. Bind the SortedList comparator to the ComboBox selection.
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
                default:
                    sortedData.setComparator(null);
                    break;
            }
        });

        // 5. Add a listener to the SortedList to update the UI whenever it changes.
        sortedData.addListener((javafx.collections.ListChangeListener<Auction>) c -> updateAuctionGrid(sortedData));

        messageListener = line -> {
            try {
                Response res = gson.fromJson(line, Response.class);
                if ("UPDATE_PRICE".equals(res.getStatus()) || "AUCTION_UPDATED".equals(res.getStatus())
                        || "AUCTION_FINISHED".equals(res.getStatus())) {
                    Platform.runLater(this::refreshAuctions);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        };
        ctx.addMessageListener(messageListener);

        refreshWalletBalance();
        refreshAuctions();
    }

    @FXML
    public void handleRefresh() {
        refreshWalletBalance();
        refreshAuctions();
    }

    private void refreshWalletBalance() {
        try {
            Map<String, String> data = new HashMap<>();
            data.put("username", ctx.getCurrentUser().getUsername());
            Response response = ctx.sendRequestAndWait(new Request("GET_WALLET_BALANCE", data), 15);
            if ("SUCCESS".equals(response.getStatus())) {
                Platform.runLater(() -> walletBalanceLabel.setText("Balance: $" + response.getMessage()));
            } else {
                Platform.runLater(() -> walletBalanceLabel.setText("Balance: unavailable"));
            }
        } catch (Exception e) {
            Platform.runLater(() -> walletBalanceLabel.setText("Balance: unavailable"));
        }
    }

    private void updateAuctionGrid(List<Auction> auctions) {
        auctionGrid.getChildren().clear();
        for (Auction auction : auctions) {
            VBox card = createAuctionCard(auction);
            auctionGrid.getChildren().add(card);
        }
    }

    private VBox createAuctionCard(Auction auction) {
        ImageView imageView = new ImageView();
        Label nameLabel = new Label();
        Label priceLabel = new Label();
        Label statusLabel = new Label();
        Label startsAtLabel = new Label();
        Label endsInLabel = new Label();
        VBox card = new VBox(10);

        imageView.setFitHeight(150);
        imageView.setFitWidth(150);
        card.getStyleClass().add("auction-card");
        nameLabel.getStyleClass().add("item-name");
        priceLabel.getStyleClass().add("item-price");
        statusLabel.getStyleClass().add("item-status");
        startsAtLabel.getStyleClass().add("item-ends-in");
        endsInLabel.getStyleClass().add("item-ends-in");
        VBox itemDetails = new VBox(5, nameLabel, priceLabel, statusLabel, startsAtLabel, endsInLabel);
        card.getChildren().addAll(imageView, itemDetails);

        if (auction.getItem().getImageUrl() != null && !auction.getItem().getImageUrl().isEmpty()) {
            imageView.setImage(new Image(auction.getItem().getImageUrl(), 150, 150, true, true));
        }
        nameLabel.setText(auction.getItem().getName());
        priceLabel.setText("Current Bid: " + auction.getCurrentPrice() + " VND");
        statusLabel.setText(auction.getStatus().toString());
        startsAtLabel.setText("Starts: " + formatEndTime(auction.getStartTime()));
        endsInLabel.setText("Ends: " + formatEndTime(auction.getEndTime()));

        card.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                showBidOptionsDialog(auction);
            }
        });

        return card;
    }

    private void refreshAuctions() {
        try {
            Request req = new Request("GET_AUCTIONS", new HashMap<>());
            Response response = ctx.sendRequestAndWait(req, 15);
            if ("SUCCESS".equals(response.getStatus())) {
                List<Auction> auctions = gson.fromJson(response.getMessage(), new TypeToken<List<Auction>>() {
                }.getType());
                Platform.runLater(() -> {
                    allAuctions.setAll(auctions);
                });
            }
        } catch (Exception e) {
            System.out.println("Failed to refresh auctions: " + e.getMessage());
        }
    }

    @FXML
    public void handleChangePassword() {
        ChangePasswordSupport.showDialog(ctx);
    }

    @FXML
    public void handleDepositRequest() {
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Deposit Request");
        dialog.setHeaderText("Send a deposit request to admin");

        TextField amountField = new TextField();
        amountField.setPromptText("Enter amount");

        ComboBox<String> bankNameComboBox = new ComboBox<>();
        bankNameComboBox.setPromptText("Select bank name");
        bankNameComboBox.setEditable(true);
        ObservableList<String> bankOptions = FXCollections.observableArrayList(BANK_NAMES);
        bankNameComboBox.setItems(bankOptions);

        bankNameComboBox.getEditor().textProperty().addListener((obs, oldText, newText) -> {
            if (newText == null || newText.isEmpty()) {
                bankNameComboBox.setItems(bankOptions);
            } else {
                List<String> filteredList = BANK_NAMES.stream()
                        .filter(s -> s.toLowerCase().contains(newText.toLowerCase()))
                        .collect(Collectors.toList());
                bankNameComboBox.setItems(FXCollections.observableArrayList(filteredList));
            }
        });

        TextField accountNumberField = new TextField();
        accountNumberField.setPromptText("Enter account number");

        VBox content = new VBox(10);
        content.getChildren().addAll(new Label("Amount"), amountField, new Label("Bank Name"), bankNameComboBox,
                new Label("Account Number"), accountNumberField);
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.setResultConverter(buttonType -> {
            if (buttonType == ButtonType.OK) {
                String selectedBank = bankNameComboBox.getSelectionModel().getSelectedItem();
                if (selectedBank == null && !bankNameComboBox.getEditor().getText().isEmpty()) {
                    selectedBank = bankNameComboBox.getEditor().getText();
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
                    Response response = ctx.sendRequestAndWait(new Request("CREATE_DEPOSIT_REQUEST", data), 15);
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

    @FXML
    public void handleWithdrawRequest() {
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Withdraw Request");
        dialog.setHeaderText("Send a withdraw request to admin");

        TextField amountField = new TextField();
        amountField.setPromptText("Enter amount");

        ComboBox<String> bankNameComboBox = new ComboBox<>();
        bankNameComboBox.setPromptText("Select bank name");
        bankNameComboBox.setEditable(true);
        ObservableList<String> bankOptions = FXCollections.observableArrayList(BANK_NAMES);
        bankNameComboBox.setItems(bankOptions);

        bankNameComboBox.getEditor().textProperty().addListener((obs, oldText, newText) -> {
            if (newText == null || newText.isEmpty()) {
                bankNameComboBox.setItems(bankOptions);
            } else {
                List<String> filteredList = BANK_NAMES.stream()
                        .filter(s -> s.toLowerCase().contains(newText.toLowerCase()))
                        .collect(Collectors.toList());
                bankNameComboBox.setItems(FXCollections.observableArrayList(filteredList));
            }
        });

        TextField accountNumberField = new TextField();
        accountNumberField.setPromptText("Enter account number");

        VBox content = new VBox(10);
        content.getChildren().addAll(new Label("Amount"), amountField, new Label("Bank Name"), bankNameComboBox,
                new Label("Account Number"), accountNumberField);
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.setResultConverter(buttonType -> {
            if (buttonType == ButtonType.OK) {
                String selectedBank = bankNameComboBox.getSelectionModel().getSelectedItem();
                if (selectedBank == null && !bankNameComboBox.getEditor().getText().isEmpty()) {
                    selectedBank = bankNameComboBox.getEditor().getText();
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

    @FXML
    public void handleLogout() {
        ctx.removeMessageListener(messageListener);
        try {
            Map<String, String> data = new HashMap<>();
            data.put("username", ctx.getCurrentUser().getUsername());
            Request req = new Request("LOGOUT", data);
            ctx.sendRequestAndWait(req, 15);
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

    private void showBidOptionsDialog(Auction auction) {
        String status = auction.getStatus().toString();
        if (!"RUNNING".equals(status)) {
            String msg = "OPEN".equals(status) ? "This auction has not started yet." : "This auction has already finished.";
            showAlert("Error", msg + " Status: " + status);
            return;
        }

        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Bid Options");
        dialog.setHeaderText("Choose bidding method for: " + auction.getItem().getName());

        ButtonType placeBidType = new ButtonType("Place Bid", ButtonBar.ButtonData.YES);
        ButtonType autoBidType = new ButtonType("Auto Bid", ButtonBar.ButtonData.NO);
        ButtonType cancelType = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);

        dialog.getDialogPane().getButtonTypes().addAll(placeBidType, autoBidType, cancelType);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == placeBidType) {
                return "PLACE_BID";
            } else if (dialogButton == autoBidType) {
                return "AUTO_BID";
            }
            return null;
        });

        dialog.showAndWait().ifPresent(result -> {
            if ("PLACE_BID".equals(result)) {
                showBidAmountDialog(auction, false);
            } else if ("AUTO_BID".equals(result)) {
                showBidAmountDialog(auction, true);
            }
        });
    }

    private void showBidAmountDialog(Auction auction, boolean isAutoBid) {
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle(isAutoBid ? "Auto Bid" : "Place Bid");
        dialog.setHeaderText("Enter bid amount for: " + auction.getItem().getName());

        TextField amountField = new TextField();
        amountField.setPromptText("Enter amount");

        BigDecimal minBid = auction.getCurrentPrice().add(auction.getItem().getMinIncrement());

        VBox content = new VBox(10);
        content.getChildren().add(new Label("Current price: " + auction.getCurrentPrice()));
        content.getChildren()
                .add(new Label("Minimum bid: " + minBid + " (current + " + auction.getItem().getMinIncrement() + ")"));
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
                        showAlert("Error",
                                "Bid amount must be at least " + minBid + " (current price + minimum increment)");
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
                refreshAuctions();
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
                refreshAuctions();
                showAlert("Success", "Auto-bid registered successfully!");
            } else {
                showAlert("Error", response.getMessage());
            }
        } catch (Exception e) {
            showAlert("Error", "Failed to register auto-bid: " + e.getMessage());
        }
    }

    private String formatEndTime(Instant endTime) {
        if (endTime == null) {
            return "Unknown";
        }

        LocalDateTime dateTime = LocalDateTime.ofInstant(endTime, ZoneId.systemDefault());
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        return dateTime.format(formatter);
    }
}