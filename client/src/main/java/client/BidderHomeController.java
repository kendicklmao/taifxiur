package client;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import shared.models.Auction;
import shared.network.Request;
import shared.network.Response;
import shared.utils.GsonUtils;

import java.io.PrintWriter;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.function.Consumer;

public class BidderHomeController {
    @FXML private ListView<Auction> auctionList;
    @FXML private Label welcomeLabel;
    @FXML private Label walletBalanceLabel;
    private final AppContext ctx = AppContext.getInstance();
    private final Gson gson = GsonUtils.createGson();
    private Consumer<String> messageListener;

    @FXML
    public void initialize() {
        welcomeLabel.setText("Welcome " + ctx.getCurrentUser().getUsername());
        auctionList.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(Auction item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.getItem().getName() + " - Current: " + item.getCurrentPrice() + " - " + item.getStatus());
                }
            }
        });

        // Add double-click listener
        auctionList.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                Auction selected = auctionList.getSelectionModel().getSelectedItem();
                if (selected != null) {
                    showBidOptionsDialog(selected);
                }
            }
        });

        messageListener = line -> {
            try {
                Response res = gson.fromJson(line, Response.class);
                if ("SUCCESS".equals(res.getStatus())) {
                    // Could be the result of GET_AUCTIONS or PLACE_BID
                    // But we don't know which one.
                    // Let's check if it looks like a list of auctions
                    try {
                        List<Auction> list = gson.fromJson(res.getMessage(), new TypeToken<List<Auction>>(){}.getType());
                        if (list != null) {
                            Platform.runLater(() -> {
                                auctionList.getItems().setAll(list);
                            });
                        }
                    } catch (Exception e) {
                        // Not a list, maybe a simple success message
                    }
                } else if ("UPDATE_PRICE".equals(res.getStatus())) {
                    Platform.runLater(this::refreshAuctions);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        };
        ctx.addMessageListener(messageListener);

        refreshWalletBalance();
        refreshAuctions();

        // Schedule periodic refresh
        Timer timer = new Timer(true);
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                Platform.runLater(() -> {
                    refreshWalletBalance();
                    refreshAuctions();
                });
            }
        }, 0, 5000); // Refresh every 5 seconds
    }

    private void refreshWalletBalance() {
        try {
            Map<String, String> data = new HashMap<>();
            data.put("username", ctx.getCurrentUser().getUsername());
            Response response = ctx.sendRequestAndWait(new Request("GET_WALLET_BALANCE", data), 5);
            if ("SUCCESS".equals(response.getStatus())) {
                Platform.runLater(() -> walletBalanceLabel.setText("Balance: $" + response.getMessage()));
            }
        } catch (Exception e) {
            Platform.runLater(() -> walletBalanceLabel.setText("Balance: unavailable"));
        }
    }

    private void refreshAuctions() {
        try {
            Request req = new Request("GET_AUCTIONS", new HashMap<>());
            Response response = ctx.sendRequestAndWait(req, 5);
            if ("SUCCESS".equals(response.getStatus())) {
                Auction[] auctions = gson.fromJson(response.getMessage(), Auction[].class);
                Platform.runLater(() -> auctionList.getItems().setAll(auctions));
            }
        } catch (Exception e) {
            System.out.println("Failed to refresh auctions: " + e.getMessage());
        }
    }

    public void handleBid() {
        // This method is no longer used - double-click on auction instead
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

        VBox content = new VBox(10);
        content.getChildren().addAll(new Label("Amount"), amountField);
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.setResultConverter(buttonType -> buttonType == ButtonType.OK ? amountField.getText() : null);
        dialog.showAndWait().ifPresent(amountText -> {
            try {
                BigDecimal amount = new BigDecimal(amountText);
                if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                    showAlert("Error", "Amount must be greater than 0");
                    return;
                }

                Map<String, String> data = new HashMap<>();
                data.put("username", ctx.getCurrentUser().getUsername());
                data.put("amount", amount.toPlainString());
                Response response = ctx.sendRequestAndWait(new Request("CREATE_DEPOSIT_REQUEST", data), 5);
                if ("SUCCESS".equals(response.getStatus())) {
                    showAlert("Success", response.getMessage());
                } else {
                    showAlert("Error", response.getMessage());
                }
            } catch (Exception e) {
                showAlert("Error", "Please enter a valid amount");
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

    private void showBidOptionsDialog(Auction auction) {
        // Check if auction is running
        if (!"RUNNING".equals(auction.getStatus().toString())) {
            showAlert("Error", "This auction is not currently running. Status: " + auction.getStatus());
            return;
        }

        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Bid Options");
        dialog.setHeaderText("Choose bidding method for: " + auction.getItem().getName());

        // Create buttons
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
        content.getChildren().add(new Label("Minimum bid: " + minBid + " (current + " + auction.getItem().getMinIncrement() + ")"));
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
                        showAlert("Error", "Bid amount must be at least " + minBid + " (current price + minimum increment)");
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
            Response response = ctx.sendRequestAndWait(req, 5); // Wait for server response

            if ("SUCCESS".equals(response.getStatus())) {
                // Immediately refresh auctions to show the updated price
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
            Response response = ctx.sendRequestAndWait(req, 5); // Wait for server response

            if ("SUCCESS".equals(response.getStatus())) {
                showAlert("Success", "Auto-bid registered successfully!");
            } else {
                showAlert("Error", response.getMessage());
            }
        } catch (Exception e) {
            showAlert("Error", "Failed to register auto-bid: " + e.getMessage());
        }
    }
}
