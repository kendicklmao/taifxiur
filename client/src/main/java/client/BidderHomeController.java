package client;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import shared.models.Auction;
import shared.network.Request;
import shared.network.Response;
import shared.utils.GsonUtils;
import shared.models.Item;
import shared.models.Electronic;
import shared.models.Vehicle;
import shared.models.Art;
import shared.models.Fashion;
import shared.models.Collectible;

import java.io.IOException;
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
import shared.enums.BankList;

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
    @FXML
    private VBox auctionDetailPane;

    private final AppContext ctx = AppContext.getInstance();
    private final Gson gson = GsonUtils.createGson();
    private final IAlertService alertService = new AlertServiceImpl();

    private Consumer<String> messageListener;
    private final ObservableList<Auction> allAuctions = FXCollections.observableArrayList();

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
        sortedData.addListener((ListChangeListener<Auction>) c -> updateAuctionGrid(sortedData));

        messageListener = line -> {
            try {
                Response res = gson.fromJson(line, Response.class);
                if ("UPDATE_PRICE".equals(res.getStatus()) || "AUCTION_UPDATED".equals(res.getStatus())
                        || "AUCTION_FINISHED".equals(res.getStatus()) || "AUCTION_CREATED".equals(res.getStatus())) {
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
        priceLabel.setText("Current Bid: " + auction.getCurrentPrice() + "$");
        statusLabel.setText(auction.getStatus().toString());
        startsAtLabel.setText("Starts: " + formatEndTime(auction.getStartTime()));
        endsInLabel.setText("Ends: " + formatEndTime(auction.getEndTime()));

        card.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                showAuctionDetails(auction);
            }
        });

        return card;
    }

    private void showAuctionDetails(Auction auction) {
        auctionDetailPane.getChildren().clear();
        auctionDetailPane.setVisible(true);
        auctionDetailPane.setManaged(true);

        // Title and close button
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

        // Image
        ImageView imageView = new ImageView();
        if (auction.getItem().getImageUrl() != null && !auction.getItem().getImageUrl().isEmpty()) {
            imageView.setImage(new Image(auction.getItem().getImageUrl(), 200, 200, true, true));
        }

        // Details grid
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
        detailsGrid.add(new Label(formatEndTime(auction.getStartTime())), 1, rowIndex++);
        detailsGrid.add(new Label("End Time:"), 0, rowIndex);
        detailsGrid.add(new Label(formatEndTime(auction.getEndTime())), 1, rowIndex++);
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

        // Action buttons
        HBox actionBox = new HBox(10);
        Button chartButton = new Button("View Chart");
        chartButton.getStyleClass().add("dashboard-btn-ghost");
        chartButton.setOnAction(e -> showPriceChart(auction));
        actionBox.getChildren().add(chartButton);

        if ("RUNNING".equals(auction.getStatus().toString())) {
            Button bidButton = new Button("Place Bid");
            bidButton.getStyleClass().add("dashboard-btn-primary");
            bidButton.setOnAction(e -> showBidAmountDialog(auction, false));

            Button autoBidButton = new Button("Auto Bid");
            autoBidButton.getStyleClass().add("dashboard-btn-ghost");
            autoBidButton.setOnAction(e -> showBidAmountDialog(auction, true));
            actionBox.getChildren().addAll(bidButton, autoBidButton);
        }

        auctionDetailPane.getChildren().addAll(titleBox, imageView, detailsGrid, actionBox);
    }

    private void refreshAuctions() {
        try {
            Response response = ctx.sendRequestAndWait(new Request("GET_AUCTIONS", new HashMap<>()), 15);
            if ("SUCCESS".equals(response.getStatus())) {
                List<Auction> auctions = gson.fromJson(response.getMessage(), new TypeToken<List<Auction>>() {
                }.getType());
                List<Auction> activeAuctions = auctions.stream()
                        .filter(a -> a.getStatus() != shared.enums.AuctionStatus.CANCELED)
                        .collect(Collectors.toList());
                Platform.runLater(() -> {
                    allAuctions.setAll(activeAuctions);
                });
            }
        } catch (Exception e) {
            alertService.showAlert("Error", "Failed to refresh auctions: " + e.getMessage(), welcomeLabel);
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

        // --- Custom Layout for the Dialog ---
        TextField amountField = new TextField();
        amountField.setPromptText("Enter amount");
        amountField.getStyleClass().add("dashboard-input");

        ComboBox<BankList> bankNameComboBox = new ComboBox<>();
        bankNameComboBox.setPromptText("Select bank name");
        bankNameComboBox.setEditable(true);
        bankNameComboBox.getStyleClass().add("dashboard-choicebox");
        ObservableList<BankList> bankOptions = FXCollections.observableArrayList(BankList.values());
        bankNameComboBox.setItems(bankOptions);

        // Autocomplete filter for the ComboBox
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
        content.getChildren().addAll(
                new Label("Amount"), amountField,
                new Label("Bank Name"), bankNameComboBox,
                new Label("Account Number"), accountNumberField
        );
        content.setStyle("-fx-padding: 20px;");

        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        // Apply custom dialog styling
        dialog.getDialogPane().getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());
        dialog.getDialogPane().getStyleClass().add("my-dialog");


        // Convert the result to a string array
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
            if (result == null || result.isEmpty()) return;

            String[] parts = result.split(",", 3);
            if (parts.length < 3 || parts[0].isEmpty() || parts[1] == null || parts[1].trim().isEmpty() || parts[2].isEmpty()) {
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

        // Apply custom dialog styling
        dialog.getDialogPane().getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());
        dialog.getDialogPane().getStyleClass().add("my-dialog");

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
        Navigator.switchSceneStatic("login.fxml");
    }


    private void showBidOptionsDialog(Auction auction) {
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Auction Options");
        dialog.setHeaderText("Choose an action for: " + auction.getItem().getName());

        // Apply custom dialog styling
        dialog.getDialogPane().getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());
        dialog.getDialogPane().getStyleClass().add("my-dialog");

        // Create buttons
        ButtonType placeBidType = new ButtonType("Place Bid", ButtonBar.ButtonData.YES);
        ButtonType autoBidType = new ButtonType("Auto Bid", ButtonBar.ButtonData.NO);
        ButtonType viewChartType = new ButtonType("View Chart", ButtonBar.ButtonData.HELP_2);
        ButtonType cancelType = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);

        // Add buttons based on auction status
        dialog.getDialogPane().getButtonTypes().add(viewChartType);
        if ("RUNNING".equals(auction.getStatus().toString())) {
            dialog.getDialogPane().getButtonTypes().addAll(placeBidType, autoBidType);
        }
        dialog.getDialogPane().getButtonTypes().add(cancelType);


        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == placeBidType) return "PLACE_BID";
            if (dialogButton == autoBidType) return "AUTO_BID";
            if (dialogButton == viewChartType) return "VIEW_CHART";
            return null;
        });

        dialog.showAndWait().ifPresent(result -> {
            if (result == null) return;
            switch (result) {
                case "PLACE_BID":
                    showBidAmountDialog(auction, false);
                    break;
                case "AUTO_BID":
                    showBidAmountDialog(auction, true);
                    break;
                case "VIEW_CHART":
                    showPriceChart(auction);
                    break;
            }
        });
    }

    private void showPriceChart(Auction auction) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/auction_chart.fxml"));
            Parent root = loader.load();

            AuctionChartController controller = loader.getController();
            controller.populateChart(auction);

            Stage stage = new Stage();
            stage.setTitle("Price Chart");
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setScene(new Scene(root));
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

        // Apply custom dialog styling
        dialog.getDialogPane().getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());
        dialog.getDialogPane().getStyleClass().add("my-dialog");

        TextField amountField = new TextField();
        amountField.setPromptText("Enter amount");
        amountField.getStyleClass().add("dashboard-input");

        BigDecimal minBid = auction.getCurrentPrice().add(auction.getItem().getMinIncrement());

        VBox content = new VBox(10);
        content.setStyle("-fx-padding: 20px;");
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

    private void showAlert(String title, String message) {
        alertService.showAlert(title, message, welcomeLabel.getScene().getWindow());
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
